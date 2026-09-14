;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; Runtime setup for Glojure binaries.

(ns yamlscript.glojure-runtime
  (:require
   [ys.v0.imports :as imports]
   [clojure.string :as str]
   [clojure.math]
   [clojure.set]
   [clojure.tools.cli]
   [yamlscript.compiler :as compiler]
   [yamlscript.module.csv]
   [yamlscript.module.fs]
   [yamlscript.module.http]
   [yamlscript.module.io]
   [yamlscript.module.pprint]
   [yamlscript.module.pods]
   [yamlscript.module.taptest]
   [yamlscript.process]
   [yamlscript.regex :as regex]
   [ys.v0]
   [ys.v0.common :as common]
   [ys.v0.global :as global]
   [ys.v0.ipc]
   [ys.v0.pprint]
   [ys.v0.re :as re]
   [ys.v0.ys :as ys])
  (:refer-clojure :exclude [load require use]))

(def builtin-modules
  '{ys.std     ys.v0.std
    ys.clj     ys.v0.clj
    ys.ys      ys.v0.ys
    ys.cli     clojure.tools.cli
    ys.csv     yamlscript.module.csv
    ys.ext     ys.v0.ext
    ys.fs      yamlscript.module.fs
    ys.http    yamlscript.module.http
    ys.io      yamlscript.module.io
    ys.ipc     ys.v0.ipc
    ys.json    ys.v0.json
    ys.math    clojure.math
    ys.pprint  yamlscript.module.pprint
    ys.pods    yamlscript.module.pods
    ys.set     clojure.set
    ys.str     clojure.string
    ys.walk    clojure.walk
    ys.yaml    ys.v0.yaml
    ys.taptest yamlscript.module.taptest})

(def hidden-core
  '[compile file-seq flush line-seq load-file load-reader newline
    pr prn printf println read-line slurp spit])

(def legacy-std
  '[bash bash-out curl exec process sh sh-out shell
    fs-d fs-e fs-f fs-l fs-r fs-s fs-w fs-x fs-z
    fs-abs fs-abs? fs-dirname fs-filename fs-basename
    fs-glob fs-ls fs-mtime fs-rel fs-rel? fs-which])

(def selection-options
  #{:as :get :all :none :not})

(def source-options
  #{:path :file :url :from})

(def legacy-use ys/+use)
(def legacy-get+ ys.v0.std/get+)

(declare set-root!)

(def enabled-modules (atom #{}))

(def wasi-restricted-modules
  '#{ys.fs ys.http ys.ipc ys.pods})

(defn- configured-modules []
  (when-let [value (System/getenv "YS_MODULES")]
    (into #{}
      (map #(if (str/starts-with? %1 "ys.")
              (symbol %1)
              (symbol (str "ys." %1))))
      (remove str/blank? (str/split value #"[,\s]+")))))

(defn- check-module-access! [module]
  (when (and (= runtime.GOOS "wasip1")
          (wasi-restricted-modules module))
    (throw
      (ex-info
        (str module " is not available in the WASI build") {})))
  (when-let [allowed (configured-modules)]
    (when-not (allowed module)
      (throw
        (ex-info
          (str module " is disabled by YS_MODULES") {})))))

(defn- short-module? [module]
  (and (symbol? module)
    (nil? (namespace module))
    (not (str/includes? (str module) "."))))

(defn- normalize-form [form]
  (let [[module & args] form
        [module args] (if (short-module? module)
                        [(symbol (str "ys." module))
                         (if (seq args) args (list :as module))]
                        [module args])
        builtin? (and (builtin-modules module)
                   (not (some source-options args)))
        public-module module
        module (if builtin? (builtin-modules module) module)
        args (if (some selection-options args)
               args
               (concat args [:none]))]
    (concat [module (if builtin? :builtin :external) public-module] args)))

(defn normalize-use-forms [forms]
  (let [forms (if (every? symbol? forms)
                (map list forms)
                (if (symbol? (first forms)) (list forms) forms))]
    (map normalize-form forms)))

(defn- option-map [args]
  (loop [args args options {}]
    (if-not (seq args)
      options
      (let [option (first args)]
        (cond
          (= :as option)
          (let [value (second args)]
            (when-not (and (symbol? value) (nil? (namespace value)))
              (throw
                (ex-info
                  "Invalid 'use' option ':as': expected one symbol" {})))
            (recur (nnext args) (assoc options option value)))

          (source-options option)
          (let [value (second args)]
            (when-not (string? value)
              (throw
                (ex-info
                  (str "Invalid 'use' option '" option
                    "': expected one string") {})))
            (when (:source options)
              (throw
                (ex-info
                  (str "Invalid 'use' option '" option
                    "': another source option is already set") {})))
            (recur (nnext args) (assoc options :source [option value])))

          (#{:get :not} option)
          (let [[symbols more]
                (split-with (complement keyword?) (rest args))]
            (recur more (assoc options option (vec symbols))))

          (#{:all :none} option)
          (recur (next args) (assoc options option true))

          :else
          (throw
            (ex-info (str "Invalid 'use' option '" option "'") {})))))))

(defn- select-vars [module options]
  (when-let [alias (:as options)]
    (clojure.core/alias alias module))
  (when-let [symbols (:get options)]
    (let [only (mapv #(if (namespace %1)
                        (symbol (namespace %1))
                        %1)
                 symbols)
          rename (into {}
                   (keep #(when-let [old (namespace %1)]
                            [(symbol old) (symbol (name %1))]))
                   symbols)]
      (doseq [sym (vals (merge (zipmap only only) rename))]
        (ns-unmap *ns* sym))
      (refer module :only only :rename rename)))
  (when (or (:all options) (:not options))
    (let [excluded (set (:not options))]
      (doseq [sym (remove excluded (keys (ns-publics module)))]
        (ns-unmap *ns* sym))
      (refer module :exclude (vec (:not options))))))

(declare load-external-module!)

(defn apply-use [target forms]
  (doseq [form (imports/normalize-use-forms forms)]
    (if (= 'ys.v0 (first form))
      (apply-use target
        (imports/v0-imports
          (into #{} (remove #(and (= runtime.GOOS "wasip1")
                              (wasi-restricted-modules %))
                      (keys builtin-modules)))
          (ns-aliases target) (configured-modules)))
      (let [[module kind public-module & args] (normalize-form form)]
    (check-module-access! public-module)
    (swap! enabled-modules conj public-module)
    (let [options (option-map args)
          module (if (= kind :builtin)
                   module
                   (load-external-module! target module options))]
      (binding [*ns* target]
        (when (and (= kind :builtin)
                (not (and (:umbrella (meta form))
                       (contains? (ns-aliases target) public-module))))
          (clojure.core/alias public-module module))
        (select-vars module options))))))
  nil)

(defn- check-form-access! [form]
  (when-not (and (seq? form) (= 'use (first form)))
    (doseq [value (tree-seq coll? seq form)]
      (when (symbol? value)
        (when-let [namespace (namespace value)]
          (let [module (symbol namespace)]
            (when (and (builtin-modules module)
                    (not (@enabled-modules module)))
              (throw
                (ex-info
                  (str "Could not resolve symbol: " value) {})))))))))

(declare install! set-root!)

(defn- definition-symbols [form]
  (when (seq? form)
    (let [head (first form)]
      (cond
        (= 'declare head) (rest form)
        (contains? '#{def defmacro defn defn- defonce} head)
        [(second form)]))))

(defn- unmap-referred-definitions! [form]
  (let [refers (ns-refers *ns*)]
    (doseq [sym (definition-symbols form)
            :when (and (symbol? sym) (contains? refers sym))]
      (ns-unmap *ns* sym))))

(def ^:dynamic *auto-use-v0* false)

(defn- prepare-expression! [form]
  (when (and *auto-use-v0* (imports/expression-imports? form))
    (apply-use *ns* '(v0))))

(defn- eval-forms [forms]
  (loop [forms forms result nil]
    (if-let [form (first forms)]
      (let [namespace-form? (and (seq? form) (= 'ns (first form)))
            _ (prepare-expression! form)
            _ (check-form-access! form)
            _ (unmap-referred-definitions! form)
            result (eval form)]
        (when namespace-form?
          (install! *ns*))
        (recur (next forms) result))
      result)))

(defn- eval-document [form]
  (if (and (seq? form) (= '+++ (first form)))
    ((var-get (resolve 'ys.v0.std/+++*))
     (eval-forms (rest form)))
    (eval form)))

(defn- portable-form [form]
  (cond
    (= form 'ys.v0.std/stream)
    'yamlscript.glojure-runtime/std-stream

    (list? form)
    (apply list (map portable-form form))

    (vector? form)
    (mapv portable-form form)

    (map? form)
    (into {} (map (fn [[key value]]
                    [(portable-form key) (portable-form value)])) form)

    (set? form)
    (set (map portable-form form))

    :else form))

(defn eval-code
  ([code] (eval-code code false))
  ([code auto-use-v0]
  (binding [*auto-use-v0* auto-use-v0]
  (loop [forms (map portable-form
                 (seq (read-string (str "[" code "\n]"))))
         result nil]
    (if-let [form (first forms)]
      (let [namespace-form? (and (seq? form) (= 'ns (first form)))
            _ (prepare-expression! form)
            _ (check-form-access! form)
            _ (unmap-referred-definitions! form)
            result (eval-document form)]
        (when namespace-form?
          (install! *ns*))
        (recur (next forms) result))
      result)))))

(declare eval-yamlscript normalize-error-message)

(defn eval-yamlscript
  ([ys-code] (eval-yamlscript ys-code "EVAL" false))
  ([ys-code _file stream-mode]
   (let [saved @global/stream-values
         saved-ns *ns*]
     (reset! global/stream-values [])
     (try
       (let [value (eval-code (compiler/compile ys-code))]
         (if stream-mode @global/stream-values value))
       (catch go/any error
         (let [message (or (ex-message error) (str error))
               message (first (str/split message #"\n\nGLJ Stack:" 2))
               message (normalize-error-message message)]
           (throw (ex-info message {}))))
       (finally
         (in-ns (ns-name saved-ns))
         (reset! global/stream-values saved))))))

(defn- absolute-path [path]
  (let [path (str path)
        path (if (path:filepath.IsAbs path)
               path
               (path:filepath.Join global/DIR path))
        [path error] (path:filepath.Abs path)]
    (when error (throw error))
    path))

(defn- get-yspath [base]
  (let [yspath (or
                 (get (System/getenv) "YSPATH")
                 (when (re-matches #".*[\\/]NO-NAME$" base)
                   (System/getProperty "user.dir"))
                 (path:filepath.Dir (absolute-path base)))
        separator (if (= runtime.GOOS "windows") #";" #":")]
    (str/split yspath separator)))

(defn- regular-file? [path]
  (let [[info error] (os.Stat path)]
    (and (nil? error) (not (.IsDir info)))))

(defn- module-candidates [root module]
  (let [path (str/replace (str module) "." "/")
        clj-path (str/replace path "-" "_")]
    [(path:filepath.Join root (str clj-path ".clj"))
     (path:filepath.Join root (str clj-path ".cljc"))
     (path:filepath.Join root (str path ".ys"))]))

(defn- find-module-file [roots module]
  (some #(when (regular-file? %1) %1)
    (mapcat #(module-candidates (absolute-path %1) module) roots)))

(defn- source-text [path]
  (yamlscript.module.fs/read path))

(defn- eval-module-source! [source file yamlscript?]
  (let [saved-file global/FILE
        saved-dir global/DIR
        saved-inc global/INC
        saved-ns *ns*]
    (try
      (set-root! #'global/FILE file)
      (when-not (str/starts-with? file "http")
        (set-root! #'global/DIR (path:filepath.Dir file))
        (set-root! #'global/INC (get-yspath file)))
      (if yamlscript?
        (eval-yamlscript source file false)
        (binding [*file* file]
          (eval-code source)))
      (finally
        (in-ns (ns-name saved-ns))
        (set-root! #'global/FILE saved-file)
        (set-root! #'global/DIR saved-dir)
        (set-root! #'global/INC saved-inc)))))

(defn- load-module-file! [file]
  (eval-module-source! (source-text file) file
    (str/ends-with? file ".ys")))

(defn- load-from-coordinate! [coordinate]
  (try
    (clojure.core/require 'clojurestar.deps)
    (let [require-deps (resolve 'clojurestar.deps/require-deps*)]
      (when-not require-deps
        (throw (ex-info "clojurestar.deps is unavailable" {})))
      (require-deps
        (cond-> {}
          (get global/ENV "YS_MAVEN_REPOSITORY")
          (assoc :mvn/local-repo
            (get global/ENV "YS_MAVEN_REPOSITORY"))
          (get global/ENV "YS_GITLIBS_DIR")
          (assoc :gitlibs/dir (get global/ENV "YS_GITLIBS_DIR")))
        [coordinate]))
    (catch go/any error
      (throw
        (ex-info
          (str "Unable to load dependency '" coordinate "': "
            (or (ex-message error) (str error))) {})))))

(defn load-external-module! [_target module options]
  (let [[kind spec] (or (:source options) [:yspath global/INC])]
    (case kind
      :yspath
      (if-let [file (find-module-file spec module)]
        (load-module-file! file)
        (throw (ex-info (str "Module not found: " module) {})))

      :path
      (if-let [file (find-module-file [spec] module)]
        (load-module-file! file)
        (throw
          (ex-info (str "Module not found in ':path': " module) {})))

      :file
      (let [file (absolute-path spec)]
        (when-not (regular-file? file)
          (throw (ex-info (str "File not found for ':file': " file) {})))
        (when-not (some #(str/ends-with? file %1) [".clj" ".cljc" ".ys"])
          (throw
            (ex-info
              (str "Invalid 'use' option ':file': expected a .ys, .clj, "
                "or .cljc file")
              {})))
        (load-module-file! file))

      :url
      (do
        (when-not (re-find #"^https?://" spec)
          (throw
            (ex-info
              "Invalid 'use' option ':url': expected an HTTP(S) URL" {})))
        (let [response (yamlscript.module.http/get spec)
              source (:body response)]
          (when-not source (throw (ex-info (str response) {})))
          (eval-module-source! (str source) spec
            (not (re-find #"^\s*[;()]" (str source))))))

      :from (load-from-coordinate! spec))
    (when-not (find-ns module)
      (throw (ex-info (str "Namespace not found: " module) {})))
    module))

(defn load-yamlscript [ys-file]
  (check-module-access! 'ys.fs)
  (let [ys-file (if (path:filepath.IsAbs ys-file)
                  ys-file
                  (path:filepath.Join
                    (path:filepath.Dir global/FILE) ys-file))
        [path error] (path:filepath.Abs ys-file)]
    (when-not (nil? error) (throw error))
    (let [saved-file global/FILE
          saved-dir global/DIR
          saved-inc global/INC]
      (try
        (set-root! #'global/FILE path)
        (set-root! #'global/DIR (path:filepath.Dir path))
        (set-root! #'global/INC (get-yspath path))
        (eval-yamlscript (source-text path) path false)
        (finally
          (set-root! #'global/FILE saved-file)
          (set-root! #'global/DIR saved-dir)
          (set-root! #'global/INC saved-inc))))))

(defn load-url [url]
  (check-module-access! 'ys.http)
  (let [response (yamlscript.module.http/get url)]
    (if-let [body (:body response)]
      (eval-yamlscript (str body) url false)
      (throw (ex-info (str response) {})))))

(defn load-pod [args]
  (check-module-access! 'ys.pods)
  (when (= runtime.GOOS "wasip1")
    (throw (ex-info "ys.pods is not available in the WASI build" {})))
  (let [command (if (and (= 1 (count args))
                      (sequential? (first args)))
                  (first args)
                  args)
        [client error]
        (github.com:glojurelang:glojure:pkg:podclient.StartCommand command)]
    (when error (throw error))
    (swap! yamlscript.global/pods conj client)
    client))

(defn unload-pods []
  (doseq [client @yamlscript.global/pods]
    (.Close client))
  (reset! yamlscript.global/pods [])
  nil)

(defn install-hooks! []
  (reset! ys/hooks
    {:compile compiler/compile
     :eval eval-yamlscript
     :eval-stream #(eval-yamlscript %1 "EVAL" true)
     :load-file load-yamlscript
     :load-url load-url
     :load-pod load-pod
     :unload-pods unload-pods}))

(defn- run-test-command [opts command]
  (if (= runtime.GOOS "windows")
    (yamlscript.process/sh opts "bash" "-c" command)
    (yamlscript.process/sh opts command)))

(defn install-process! []
  (clojure.core/require 'babashka.process)
  (doseq [sym '[exec process sh shell]]
    (intern 'babashka.process sym
      (var-get (ns-resolve 'yamlscript.process sym)))))

(defmacro use [& forms]
  `(apply-use *ns* '~forms))

(defn require [& _]
  (throw
    (ex-info "The 'require' function is retired. Use 'use' instead.\n" {})))

(defn load [ys-file]
  (load-yamlscript ys-file))

(defn fs-read [path]
  (check-module-access! 'ys.fs)
  (let [[bytes error] (os.ReadFile path)]
    (when error (throw error))
    (fmt.Sprintf "%s" bytes)))

(defn fs-write [path content]
  (check-module-access! 'ys.fs)
  (let [error
        (github.com:yaml:yamlscript:internal:goyamlparser.WriteTextFile
          path (str content) 0644)]
    (when error (throw error))))

(defn- string-bytes [text]
  ((go/slice-of go/byte) (str text)))

(defn std-base64-points [text]
  (string-bytes
    (.EncodeToString encoding:base64.StdEncoding (string-bytes text))))

(defn std-base64-encode [text]
  (fmt.Sprintf "%s" (std-base64-points text)))

(defn std-base64-decode [text]
  (let [[bytes error] (.DecodeString encoding:base64.StdEncoding (str text))]
    (when error (throw error))
    (fmt.Sprintf "%s" bytes)))

(defn std-digest [algorithm text]
  (let [bytes (string-bytes text)
        digest (case algorithm
                 :md5 (crypto:md5.Sum bytes)
                 :sha1 (crypto:sha1.Sum bytes)
                 :sha256 (crypto:sha256.Sum256 bytes))]
    (fmt.Sprintf "%x" digest)))

(defn std-md5 [text] (std-digest :md5 text))
(defn std-sha1 [text] (std-digest :sha1 text))
(defn std-sha256 [text] (std-digest :sha256 text))

(defn std-document [value]
  (reset! global/doc-anchors_ {})
  (when ((some-fn map? seqable? number? string?) value)
    (set-root! #'global/_ value)
    (swap! global/stream-values conj value))
  value)

(defn std-stream
  ([] @global/stream-values)
  ([values]
   (reset! global/stream-values values)
   nil))

(defn std-get+ [collection key]
  (if (and (seqable? collection) (= '$ key))
    (last collection)
    (legacy-get+ collection key)))

(defn- normalize-error-message [message]
  (case message
    "divide by zero" "Divide by zero"
    message))

(defn glojure-error-map [error]
  {:cause (normalize-error-message
            (or (ex-message error) (str error)))})

(defn compatible-re-pattern [pattern]
  (regex/compile pattern))

(defn std-qr [pattern]
  (compatible-re-pattern pattern))

(defn std-split
  ([text]
   (let [text (str text)]
     (if (empty? text) [] (str/split text #""))))
  ([text pattern]
   (let [[text pattern] (if (common/regex? text)
                          [pattern text]
                          [text pattern])
         pattern (if (string? pattern)
                   (compatible-re-pattern pattern)
                   pattern)]
     (str/split (str text) pattern))))

(defmacro compatible-qw [& values]
  (let [values (map #(cond
                       (nil? %1) "nil"
                       (= "(%)" (str %1)) "{}"
                       (and (vector? %1) (empty? %1)) "[]"
                       :else (str %1))
                 values)]
    `[~@values]))

(defn- io-proxy [name args]
  (check-module-access! 'ys.io)
  (apply (var-get (ns-resolve 'yamlscript.module.io name)) args))

(defn std-err [& args] (io-proxy 'err args))
(defn std-out [& args] (io-proxy 'out args))
(defn std-pp [value]
  (check-module-access! 'ys.pprint)
  (yamlscript.module.pprint/pp value))
(defn std-print [& args] (io-proxy 'print args))
(defn std-say [& args] (io-proxy 'say args))
(defn std-warn [& args] (io-proxy 'warn args))

(defn std-readline
  ([] (io-proxy 'readline [*in*]))
  ([reader] (io-proxy 'readline [reader])))

(def standard-refers
  '[[ys.v0.std :all]
    [ys.v0.dwim :all]
    [ys.v0.util [condf]]
    [ys.v0.debug [DBG PPP TTT WWW XXX YYY ZZZ]]])

(def runtime-vars
  '[_ ARGS ARGV CWD DIR ENV FILE INC PUN RUN VERSION])

(defn install! [target]
  (do
    (intern 'clojure.core 're-pattern compatible-re-pattern)
    (let [var (intern 'ys.v0.std 'qw (var-get #'compatible-qw))]
      (alter-meta! var assoc :macro true))
    (intern 'ys.v0.std '+++* std-document)
    (intern 'ys.v0.std 'get+ std-get+)
    (intern 'ys.v0.std 'stream std-stream)
    (reset! yamlscript.module.taptest/error-map-hook glojure-error-map)
    (reset! yamlscript.module.taptest/command-hook run-test-command)
    (intern 'ys.v0.fs 'read fs-read)
    (intern 'ys.v0.fs 'write fs-write)
    (doseq [[sym value]
            {'read fs-read
             'write fs-write
             'base64 std-base64-encode
             'base64-decode std-base64-decode
             'base64-encode std-base64-encode
             'base64-points std-base64-points
             'md5 std-md5
             'qr std-qr
             'sha1 std-sha1
             'sha256 std-sha256
             'split std-split
             'err std-err
             'out std-out
             'pp std-pp
             'print std-print
             'readline std-readline
             'say std-say
             'warn std-warn}]
      (intern 'ys.v0.std sym value))
    (doseq [sym legacy-std]
      (ns-unmap 'ys.v0.std sym)
      (ns-unmap target sym))
    (doseq [[namespace symbols] standard-refers]
      (doseq [sym (if (= :all symbols)
                    (keys (ns-publics namespace))
                    symbols)]
        (ns-unmap target sym))
      (if (= :all symbols)
        (refer namespace)
        (refer namespace :only (vec symbols))))
    (refer 'ys.v0.global :only (vec runtime-vars))
    (doseq [sym hidden-core]
      (ns-unmap target sym))
    (doseq [sym '[load require use]]
      (ns-unmap target sym))
    (refer 'yamlscript.glojure-runtime :only '[load require use])
    nil))

(defn enter-main! []
  (let [target (or (find-ns 'main) (create-ns 'main))]
    (reset! enabled-modules #{})
    (alter-var-root #'*in* (constantly os.Stdin))
    (alter-var-root #'*out* (constantly os.Stdout))
    (alter-var-root #'*err* (constantly os.Stderr))
    (github.com:glojurelang:glojure:pkg:lang.PushThreadBindings
      {#'*ns* target})
    (refer 'clojure.core)
    (clojure.core/require 'ys.v0)
    (install-process!)
    (install! target)
    (install-hooks!)))

(defn- set-root! [var value]
  (alter-var-root var (constantly value)))

(defn- coerce-arg [arg]
  (cond
    (re-matches re/xnum arg)
    (read-string (str/replace arg #"^([-+]?)0o" (str "$1" "0")))
    (re-matches re/keyw arg) (keyword (subs arg 1))
    :else arg))

(defn- normalize-arch [arch]
  (case (str/lower-case (str arch))
    "amd64" "x86_64"
    "arm64" "aarch64"
    (str arch)))

(defn- glojure-version []
  (let [version *glojure-version*]
    (if (map? version)
      (str (:major version) "." (:minor version) "."
        (:incremental version)
        (when-let [qualifier (:qualifier version)]
          (str "-" qualifier)))
      (str version))))

(defn set-runtime! [file args original-args]
  (let [[file error] (path:filepath.Abs file)
        file (if (nil? error) file file)
        [executable _] (os.Executable)
        [hostname _] (os.Hostname)
        argv (vec args)]
    (set-root! #'global/ARGS (mapv coerce-arg argv))
    (set-root! #'global/ARGV argv)
    (set-root! #'global/ENV (into {} (System/getenv)))
    (set-root! #'global/DIR (path:filepath.Dir file))
    (set-root! #'global/FILE file)
    (set-root! #'global/INC
      (try (get-yspath file) (catch go/any _ [])))
    (set-root! #'global/VERSION ys.v0/VERSION)
    (set-root! #'global/RUN
      (merge global/RUN
        {:args (vec original-args)
         :arch (normalize-arch runtime.GOARCH)
         :bin (path:filepath.Dir executable)
         :hostname hostname
         :os runtime.GOOS
         :versions {:clojure (str "glojure " (glojure-version))
                    :yamlscript ys.v0/VERSION}
         :yspath executable}))
    (global/reset-env global/ENV)
    nil))
