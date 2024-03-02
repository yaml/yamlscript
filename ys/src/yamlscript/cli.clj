;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.cli library compiles into the `ys` command line binary for
;; YAMLScript.

(ns yamlscript.cli
  (:gen-class)
  (:require
   ;; This goes first for pprint/graalvm patch (prevents binary bloating)
   [yamlscript.compiler :as compiler]
   [yamlscript.runtime :as runtime]
   ;; For www debugging
   [yamlscript.debug :refer [www]]
   [babashka.process :refer [exec]]
   [clj-yaml.core :as yaml]
   [clojure.data.json :as json]
   [clojure.pprint :as pp]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.tools.cli :as cli]))

(def yamlscript-version "0.1.38")

(def testing (atom false))

;; ----------------------------------------------------------------------------
(defn in-repl []
  (some #(and
           (= "clojure.main$repl" (.getClassName ^StackTraceElement %1))
           (= "doInvoke" (.getMethodName ^StackTraceElement %1)))
    (.getStackTrace (Thread/currentThread))))

(defn exit [n]
  (if (or (in-repl) @testing)
    (str "*** exit " n " ***")
    (System/exit n)))

(defn die [& xs]
  (binding [*out* *err*]
    (println (apply str "Error: " xs))
    (exit 1)))

(defn todo [s & _]
  (die "--" s " not implemented yet."))

(defn daemon-thread [^Runnable f]
  (.start (doto (new Thread f)
            (.setDaemon true))))

(defn get-stdin []
  (let [stdin (promise)]
    (daemon-thread #(do (Thread/sleep 500)
                        (deliver stdin nil)))
    (daemon-thread #(do (deliver stdin (slurp *in*))))
    @stdin))

;; ----------------------------------------------------------------------------
(def to-fmts #{"json" "yaml" "edn"})

(def stages
  {"parse" true
   "compose" true
   "resolve" true
   "build" true
   "transform" true
   "construct" true
   "print" true})

;; See https://clojure.github.io/tools.cli/#clojure.tools.cli/parse-opts
(def cli-options
  [;
   [nil "--run"
    "Run a YAMLScript program file (default)"]
   ["-l" "--load"
    "Output (compact) JSON of YAMLScript evaluation"]
   ["-e" "--eval YSEXPR"
    "Evaluate a YAMLScript expression
                           multiple -e values joined by newline"
    :default []
    :update-fn conj
    :multi true]

   ["-c" "--compile"
    "Compile YAMLScript to Clojure"]
   ["-b" "--binary"
    "Compile to a native binary executable"]

   ["-p" "--print"
    "Print the result of --run in code mode"]
   ["-o" "--output FILE"
    "Output file for --load, --compile or --binary"]

   ["-T" "--to FORMAT"
    "Output format for --load:
                             json, yaml, edn"
    :validate
    [#(contains? to-fmts %1)
     (str "must be one of: " (str/join ", " to-fmts))]]
   ["-J" "--json"
    "Output (pretty) JSON for --load"]
   ["-Y" "--yaml"
    "Output YAML for --load"]
   ["-E" "--edn"
    "Output EDN for --load"]

   ["-m" "--mode MODE"
    "Add a mode tag: code, data, or bare (for -e)"
    :validate
    [#(some #{%1} ["c" "code", "d" "data", "b" "bare"])
     (str "must be one of: c, code, d, data, b or bare")]]
   ["-C" "--clj"
    "Treat input as Clojure code"]

   #_["-R" "--repl"
      "Start an interactive YAMLScript REPL"]
   #_["-N" "--nrepl"
      "Start a new nREPL server"]
   #_["-K" "--kill"
      "Stop the nREPL server"]

   ["-d" nil
    "Debug all compilation stages"
    :id :debug-stage
    :update-fn (fn [_] stages)]
   ["-D" "--debug-stage STAGE"
    "Debug a specific compilation stage:
                             parse, compose, resolve, build,
                             transform, construct, print
                           can be used multiple times"
    :default {}
    :update-fn #(if (= "all" %2) stages (assoc %1 %2 true))
    :multi true
    :validate
    [#(or (contains? stages %1) (= %1 "all"))
     (str "must be one of: "
       (str/join ", " (keys stages))
       " or all")]]
   ["-S" "--stack-trace"
    "Print full stack trace for errors"]

   [nil "--install"
    "Install the libyamlscript shared library"]
   [nil "--upgrade"
    "Upgrade both ys and libyamlscript"]

   [nil "--version"
    "Print version and exit"]
   ["-h" "--help"
    "Print this help and exit"]])

(defn do-error [errs help]
  (let [errs (if (> (count errs) 1)
               (apply list "Error(s):" errs)
               errs)]
    (println (str/join "\n* " errs))
    (println (str "\n" help)))
  (exit 1))

(declare add-ys-mode-tag)
(defn get-binary-info [opts args]
  (let [in-file (when (seq args) (first args))
        code (when (seq (:eval opts))
               (str
                 (->> opts
                   :eval
                   (str/join "\n"))
                 "\n"))]
    (or in-file code
      (die "No input file specified"))
    (let [in-file (if code "NO-NAME.ys" in-file)]
      (or (re-find #"\.ys$" in-file)
        (die "Input file must end in .ys"))
      [in-file code])))

(defn get-ys-sh-path []
  (let [path (-> (java.lang.ProcessHandle/current) .info .command .get)
        cmd (if (re-find #"-openjdk-" path)
              (str "ys-sh-" yamlscript-version)
              (str/replace path #"/[^/]*$"
                (str "/ys-sh-" yamlscript-version)))]
    [cmd path]))

(defn do-install [opts args]
  (let [[cmd] (get-ys-sh-path)]
    (exec cmd "--install")))

(defn do-upgrade [opts args]
  (let [[cmd] (get-ys-sh-path)]
    (exec cmd "--upgrade")))

(defn do-binary [opts args]
  (let [[cmd path] (get-ys-sh-path)
        [in-file code] (get-binary-info opts args)
        out-file (some identity
                   [(:output opts)
                    (and in-file (str/replace in-file #"\.ys$" ""))])
        path (if (re-find #"-openjdk-" path) "ys" path)]
    (flush)
    (exec {:extra-env {"YS_BIN" path
                       "YS_CODE" (or code "")}}
      cmd "--compile-to-binary"
      in-file out-file yamlscript-version)))

(defn do-version []
  (println (str "YAMLScript " yamlscript-version)))

(def help-heading "
ys - The YAMLScript (YS) Command Line Tool

Usage: ys [<option...>] [<file>]

Options:

")

(defn do-help [help]
  (let [help (str/replace help #"^" help-heading)
        help (str/replace help #"\[\]" "  ")
        help (str/replace help #"\{\}" "  ")
        ;; Insert blank lines in help text
        help (str/replace help #"\n  (-[cmpTd])" "\n\n  $1")
        help (str/replace help #"\n  (.*--version)" "\n\n  $1")
        help (str/replace help #"\n  (.*--install)" "\n\n  $1")
        help (str/replace help #"    ([A-Z])" #(second %1))]
    (println help)))

(defn add-ys-mode-tag [opts code]
  (let [mode (:mode opts)
        code (str/join ""
               [(cond
                  (or (str/starts-with? code "--- !yamlscript/v0")
                    (str/starts-with? code "!yamlscript/v0")
                    (:clj opts))
                  ""
                  (or (= "c" mode) (= "code" mode))
                  "--- !yamlscript/v0/code\n"
                  (or (= "d" mode) (= "data" mode))
                  "--- !yamlscript/v0/data\n"
                  (or (= "b" mode) (= "bare" mode))
                  "--- !yamlscript/v0/bare\n"
                  (:load opts)
                  ""
                  :else
                  "--- !yamlscript/v0\n")
                code])
        code (if (re-find #"^---(?:[ \n]|$)" code)
               code
               (str "---\n" code))]
    code))

(defn get-code [opts args]
  (let [[file & args] args
        f-code (if file
                 (str
                   (if (= "-" file)
                     (slurp *in*)
                     (slurp file))
                   "\n")
                 (get-stdin))
        e-code (when (seq (:eval opts))
                 (str
                   (->> opts
                     :eval
                     (str/join "\n")
                     (add-ys-mode-tag opts))
                   "\n"))
        opts (if (and e-code f-code)
               (assoc opts :load true)
               opts)
        _ (or e-code f-code
            (binding [*out* *err*]
              (println "Warning: No input found.")))
        code (str f-code e-code)
        file (or file "NO-NAME")]
    [code file args (:load opts)]))

(defn compile-code [code opts]
  (if (:clj opts)
    code
    (try
      (if (empty? (:debug-stage opts))
        (compiler/compile code)
        (do
          (reset! compiler/debug (:debug-stage opts))
          (compiler/compile-debug code)))
      (catch Exception e (die e opts)))))

(defn get-compiled-code [opts args]
  (let [[code file args load] (get-code opts args)
        code (if code (compile-code code opts) "")]
    [code file args load]))

(def json-options
  {:escape-unicode false
   :escape-js-separators false
   :escape-slash false})

(defn do-run [opts args]
  (try
    (let [[code file args load] (get-compiled-code opts args)
          result (runtime/eval-string code file args)]
      (if (:print opts)
        (pp/pprint result)
        (when (and load (not (= "" code)))
          (case (:to opts)
            "yaml" (println
                     (str/trim-newline
                       (yaml/generate-string
                         result
                         :dumper-options {:flow-style :block})))
            "json" (json/pprint result json-options)
            "edn"  (pp/pprint result)
            ,      (println (json/write-str result json-options))))))
    (catch Exception e
      (let [{:keys [cause data trace]} (Throwable->map e)
            {:keys [file line column]} data
            stack-trace (:stack-trace opts)
            msg (if stack-trace
                  (with-out-str
                    (pp/pprint
                        {:stack-trace true
                         :cause cause
                         :file file
                         :line line
                         :column column
                         :trace trace}))
                  (str cause "\n"
                    (if (seq file)
                      (str
                        "  in file '" file "'"
                        " line " line
                        " column " column "\n")
                      "")))]
        (die msg)))))

(defn do-compile [opts args]
  (let [[code _ _ #_file #_args] (get-compiled-code opts args)]
    (-> code
      compiler/pretty-format
      str/trim-newline
      println)))

(defn do-repl [opts]
  (todo "repl" opts))

(defn do-nrepl [opts args]
  (todo "nrepl" opts args))

(defn do-connect [opts args]
  (todo "connect" opts args))

(defn do-kill [opts args]
  (todo "kill" opts args))

(defn elide-empty [opt opts]
  (if (empty? (get opts opt))
    (dissoc opts opt)
    opts))

(defn do-default [opts args help]
  (if (or
        (seq (elide-empty
               :eval
               (elide-empty :debug-stage opts)))
        (seq args))
    (do-run opts args)
    (if (let [stdin (get-stdin)
              opts (assoc opts :stdin stdin)]
          (when stdin
            (do-run opts args))
          stdin)
      true
      (do-help help))))

(defn mutex [opts keys]
  (let [omap (reduce #(if (%2 opts)
                        (assoc %1 %2 true)
                        %1) {} keys)]
    (when (> (count omap) 1)
      (let [[first second] (seq omap)
            first (str "--" (name (key first)))
            second (str "--" (name (key second)))]
        (str "Options " first " and " second
          " are mutually exclusive.")))))

(defn mutex1 [opts opt keys]
  (let [omap (reduce #(if (%2 opts)
                        (assoc %1 %2 true)
                        %1) {} keys)]
    (when (and (opt opts) (> (count omap) 0))
      (let [[second] (seq omap)
            first (str "--" (name opt))
            second (str "--" (name (key second)))]
        (str "Options " first " and " second
          " are mutually exclusive.")))))

(defn k2o [k]
  (str "--" (name k)))

(defn needs [opts opt keys]
  (when (and (opt opts) (not (some opts keys)))
    (let [first (str "--" (name opt))]
      (str "Option " first " requires "
        (if (= 1 (count keys))
          (k2o (clojure.core/first keys))
          " one of ...")))))

(def all-opts
  #{:run :load :compile :eval
    :clj :mode :print :output
    :to :json :yaml :edn
    :repl :nrepl :kill
    :stack-trace :debug-stage
    :version :help})

(def action-opts
  #{:run :load :compile
    :repl :nrepl :kill
    :version :help})

(def eval-action-opts
  #{:run :load :compile})

(def format-opts
  #{:to :json :yaml :edn})

(def repl-opts
  #{:repl :nrepl :kill})

(def info-opts
  #{:version :help})

(defn validate-opts [opts]
  (let [opts (elide-empty :eval opts)
        opts (elide-empty :debug-stage opts)]
    (or
      (mutex opts action-opts)
      (mutex opts format-opts)
      (mutex1 opts :help (set/difference all-opts #{:help}))
      (mutex1 opts :version (set/difference all-opts #{:version}))
      (mutex1 opts :mode (set/union info-opts repl-opts))
      (mutex1 opts :eval (set/difference action-opts eval-action-opts))
      (needs opts :mode #{:eval})
      (mutex1 opts :print (set/difference action-opts #{:run}))
      (mutex1 opts :to (set/difference action-opts #{:load :compile})))))

(defn -main [& args]
  (let [options (cli/parse-opts args cli-options)
        {opts :options
         args :arguments
         help :summary
         errs :errors} options
        error (validate-opts opts)
        opts (if (:json opts) (assoc opts :to "json") opts)
        opts (if (:yaml opts) (assoc opts :to "yaml") opts)
        opts (if (:edn opts) (assoc opts :to "edn") opts)
        opts (if (:to opts) (assoc opts :load true) opts)
        opts (if (and (not (:mode opts)) (seq (:eval opts)))
               (assoc opts :mode "code") opts)]
    (cond
      error (do-error [(str "Error: " error)] help)
      (seq errs) (do-error errs help)
      (:help opts) (do-help help)
      (:version opts) (do-version)
      (:install opts) (do-install opts args)
      (:upgrade opts) (do-upgrade opts args)
      (:binary opts) (do-binary opts args)
      (:run opts) (do-run opts args)
      (:compile opts) (do-compile opts args)
      (:load opts) (do-run opts args)
      (:repl opts) (do-repl opts)
      (:connect opts) (do-connect opts args)
      (:kill opts) (do-kill opts args)
      (:nrepl opts) (do-nrepl opts args)
      :else (do-default opts args help))))

(comment
  www
  (-main)
  (-main
    "-e" "say: 123"
    ;"--run"
    ;"--load"
    ;"--to=json"
    ;"--compile"
    ; "--mode=code"
    ;"--help"
    ; "-e" "say: 123"
    #__)
  )
