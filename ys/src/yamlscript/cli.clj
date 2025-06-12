;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.cli library compiles into the `ys` command line binary for
;; YS.

(ns yamlscript.cli
  (:gen-class)
  (:require
   [babashka.fs :as fs]
   [babashka.process :refer [check exec process]]
   [clj-yaml.core :as yaml]
   [clojure.data.csv :as csv]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.stacktrace]
   [clojure.tools.cli :as cli]
   [yamlscript.common]
   [yamlscript.compiler :as compiler]
   [yamlscript.global :as global :refer [env]]
   [yamlscript.runtime :as runtime])
  (:refer-clojure))

(def yamlscript-version "0.1.97")

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

(defn err [e]
  (let [prefix @global/error-msg-prefix
        msg (if (instance? Throwable e)
              (:cause (Throwable->map e))
              e)]
    (global/reset-error-msg-prefix!)
    (binding [*out* *err*]
      (print prefix)
      (if (and (:stack-trace @global/opts) (instance? Throwable e))
        (do
          (clojure.stacktrace/print-stack-trace e)
          (flush))
        (println (str/replace msg "java.lang.Exception: " "")))))
  (exit 1))

(defn todo [s & _]
  (err (str "--" s " not implemented yet.")))

;; ----------------------------------------------------------------------------
(def to-fmts #{"json" "yaml" "csv" "tsv" "edn"})

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
   ["-e" "--eval YSEXPR"
    "Evaluate a YS expression
                             multiple -e values are joined by newline"
    :default []
    :update-fn conj
    :multi true]
   ["-l" "--load"
    "Output the (compact) JSON of YS evaluation"]
   ["-f" "--file FILE"
    "Explicitly indicate input file"]

   ["-c" "--compile"
    "Compile YS to Clojure"]
   ["-b" "--binary"
    "Compile to a native binary executable"]

   ["-p" "--print"
    "Print the final evaluation result value"]
   ["-o" "--output FILE"
    "Output file for --load, --compile or --binary"]
   ["-s" "--stream"
    "Output all results from a multi-document stream"]

   ["-T" "--to FORMAT"
    "Output format for --load:
                             json, yaml, csv, tsv, edn"
    :validate
    [#(contains? to-fmts %1)
     (str "must be one of: json, yaml, csv, tsv, edn")]]
   ["-J" "--json"
    "Output (pretty) JSON for --load"]
   ["-Y" "--yaml"
    "Output YAML for --load"]
   ["-U" "--unordered"
    "Mappings don't preserve key order (faster)"]

   ["-m" "--mode MODE"
    "Add a mode tag: code, data, or bare (for -e)"
    :validate
    [#(some #{%1} ["c" "code", "d" "data", "b" "bare"])
     (str "must be one of: c, code, d, data, b or bare")]]
   ["-C" "--clojure"
    "Treat input as Clojure code"]

   #_["-R" "--repl"
      "Start an interactive YS REPL"]
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
   ["-x" "--xtrace"
    "Print each expression before evaluation"]

   [nil "--install"
    "Install the libyamlscript shared library"]
   [nil "--upgrade"
    "Upgrade both ys and libyamlscript"]

   [nil "--version"
    "Print version and exit"]
   ["-h" "--help"
    "Print this help and exit"]])

(defn do-error [errs]
  (let [errs (if (> (count errs) 1)
               (apply list "Error(s):" errs)
               errs)]
    (println (str (str/join "\n* " errs) "\n")))
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
      (err "No input file specified"))
    (let [in-file (if code "NO-NAME.ys" in-file)]
      (or (re-find #"\.ys$" in-file)
        (err "Input file must end in .ys"))
      [in-file code])))

(defn get-ys-sh-path []
  (let [path (-> (java.lang.ProcessHandle/current) .info .command .get)
        cmd (if (re-find #"-openjdk-" path)
              (str "ys-sh-" yamlscript-version)
              (str/replace path #"/[^/]*$"
                (str "/ys-sh-" yamlscript-version)))]
    [cmd path]))

(defn do-install [_opts _args]
  (let [[cmd] (get-ys-sh-path)]
    (exec cmd "--install")))

(defn do-upgrade [_opts _args]
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
  (println (str "YS (YAMLScript) " yamlscript-version)))

(def help-heading (str "
ys - The YS Command Line Tool - v" yamlscript-version "

Usage: ys [<option...>] [<file>]

Options:

"))

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

(defn add-ecode-mode-tag
  "When we use 'ys -e' we don't want to require the leading !YS-v0 tag.
   We can assume code mode by default.
   We need to check if -m was used or if there is already a tag."
  [opts ecode]
  (let [mode (:mode opts)
        code (str/join ""
               [(cond
                  (or
                    (re-find #"(?m)^---\s" ecode)
                    (re-find #"(?m)^(?:---\s+)?!(?:yamlscript/v0|YS-v0)" ecode)
                    (:clojure opts))
                  ""
                  (or (= "c" mode) (= "code" mode))
                  "--- !YS-v0\n"
                  (or (= "d" mode) (= "data" mode))
                  "--- !YS-v0:\n"
                  (or (= "b" mode) (= "bare" mode))
                  "---\n"
                  (:load opts)
                  ""
                  :else
                  "--- !YS-v0\n")
                ecode])
        code (if (or (:clojure opts)
                   (re-find #"^---(?:[ \n]|$)" code))
               code
               (str "---\n" code))]
    code))

(defn get-code [opts]
  (let [file (get opts :file)
        f-code (when file
                 (str
                   (if (= "-" file)
                     (slurp *in*)
                     (slurp file))
                   "\n"))
        e-code (when (seq (:eval opts))
                 (str
                   (->> opts
                     :eval
                     (str/join "\n")
                     (#(if (not (re-find #"\n" %1))
                         (cond
                           (re-find #"^\.[\w\$]" %1)
                           (str (if (:stream opts)
                                  "stream()"
                                  "stream().last()") %1)
                           ,
                           (re-find #"^\:\w" %1)
                           , (str (if (:stream opts)
                                    "stream()"
                                    "stream().last()")
                               (str/replace %1 #"^:([-\w]+)" ".$1()"))
                           :else %1)
                         %1))
                     (add-ecode-mode-tag opts))
                   "\n"))
        opts (if (and e-code f-code)
               (assoc opts :load true)
               opts)
        _ (or e-code f-code
            (binding [*out* *err*]
              (println "Warning: No input found.")))
        code (str f-code e-code)
        file (or file "NO-NAME")]
    [code file (:load opts)]))

(defn compile-code [code opts]
  (if (:clojure opts)
    code
    (try
      (if (seq (:debug-stage opts))
        (compiler/compile-with-options code)
        (compiler/compile code))
      (catch Exception e
        (global/reset-error-msg-prefix! "Compile error: ")
        (err e)))))

(defn get-compiled-code [opts]
  (let [[code file load] (get-code opts)
        code (if code (compile-code code opts) "")]
    [code file load]))

(def json-options
  {:escape-unicode false
   :escape-js-separators false
   :escape-slash false
   :key-fn (fn [x]
             (let [t (type x)]
               (if (contains? #{clojure.lang.Symbol
                                Double
                                Float
                                Integer
                                Long
                                String} t)
                 (str x)
                 (if (= t clojure.lang.Keyword)
                   (subs (str x) 1)
                   (throw (Exception.
                            (str "Unsupported key type '" t "'\n"
                              "Key = '" x "'")))))))})

(defn clojure-format [clojure formatter]
  (let [out (try
              (:out
               (check
                 (process {:in clojure
                           :out :string
                           :err *err*}
                   formatter)))
              (catch Exception e
                (global/reset-error-msg-prefix!
                  (str "Compiler formatter error in '" formatter "':\n"))
                (err e)))]
    out))

(defn pretty-clojure [code]
  (str/trimr
    (let [code (compiler/pretty-format code)]
      (if (env "YS_FORMATTER")
        (let [formatter (env "YS_FORMATTER")]
          (if formatter
            (clojure-format code formatter)
            code))
        code))))

(defn do-compile [opts args]
  (let [[code _ _ #_file #_args] (get-compiled-code opts)
        clojure (pretty-clojure code)]
    (println clojure)
    (System/exit 0)))

(def line (str (str/join (repeat 80 "-")) "\n"))

(defn do-run [opts args]
  (try
    (let [[code file load] (get-compiled-code opts)
          _ (when (env "YS_SHOW_COMPILE")
              (eprint (str line (pretty-clojure code) "\n" line)))
          result (runtime/eval-string code file args)
          results (if (and (:stream opts) (or load
                                            (seq (:eval opts))))
                    @global/stream-values
                    [result])]
      (if (:print opts)
        (pp/pprint result)
        (when (and load (not (= "" code)))
          (doall
            (for [result (remove nil? results)]
              (case (:to opts)
                "yaml" (println
                         (str
                           (when (> (count results) 1) "---\n")
                           (str/trim-newline
                             (yaml/generate-string
                               result
                               :dumper-options {:flow-style :block}))))
                "json" (json/pprint result json-options)
                "csv"  (println
                         (with-open [s (java.io.StringWriter.)]
                           (csv/write-csv s result :separator \,)
                           (str s)))
                "tsv"  (println
                         (with-open [s (java.io.StringWriter.)]
                           (csv/write-csv s result :separator \tab)
                           (str s)))
                "edn"  (pp/pprint result)
                ,      (println (json/write-str result json-options))))))))
    (catch Exception e
      (global/reset-error-msg-prefix! "Error: ")
      (err e))))

(defn do-repl [opts]
  (todo "repl" opts))

(defn do-nrepl [opts args]
  (todo "nrepl" opts args))

(defn do-connect [opts args]
  (todo "connect" opts args))

(defn do-kill [opts args]
  (todo "kill" opts args))

(defn elide-empty [opts & keys]
  (reduce
    (fn [opts key]
      (if (empty? (key opts))
        (dissoc opts key)
        opts))
    opts keys))

(def env-opts
  #{:unordered :print :stack-trace :xtrace})

(defn do-default [opts args help]
  (if (or
        (:file opts)
        (seq (apply dissoc
               (elide-empty opts :eval :debug-stage)
               env-opts))
        (seq args))
    (do-run opts args)
    (do-help help)))

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
  #{:run :load :eval
    :compile :binary
    :print :output :stream
    :to :json :yaml :edn :unordered
    :mode :clojure
    ;:repl :nrepl :kill
    :debug-stage :stack-trace :xtrace
    :install :upgrade
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
  (let [opts (elide-empty opts :eval :debug-stage)]
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

(defn looks-like-expr [file]
  (when (and
          (string? file)
          (re-find #"(?:^(?:\ |(?:_?[.:]\w))|\ $|\(|:\ )" file))
    (if (fs/exists? file)
      (err (str "'" file "' looks like an expression,\n"
             "but is also the name of a file. Use --eval or -e."))
      true)))

(defn do-main [opts args help error errs]
  (if error
    (do-error [(str "Error: " error)])
    (if (seq errs)
      (do-error errs)
      (condp #(%1 %2) opts
        :help (do-help help)
        :version (do-version)
        :install (do-install opts args)
        :upgrade (do-upgrade opts args)
        :binary (do-binary opts args)
        :run (do-run opts args)
        :compile (do-compile opts args)
        :load (do-run opts args)
        :repl (do-repl opts)
        :connect (do-connect opts args)
        :kill (do-kill opts args)
        :nrepl (do-nrepl opts args)
        (do-run opts args)))))

(defn get-opts [argv]
  (let [orig argv
        file (first argv)
        [filed argv] (if (and file
                           (not (re-find #"^-." file))
                           (fs/exists? file)
                           (fs/regular-file? file))
                       [file (conj (rest argv) "--" file)]
                       [nil argv])
        [args argv] (split-with #(not= "--" %1) argv)
        [argv is--] (if (= "--" (first argv))
                      [(rest argv) true]
                      [argv false])
        options (cli/parse-opts args cli-options)
        {opts :options
         args :arguments
         help :summary
         errs :errors} options
        args (concat args argv)
        opts (if-not (seq orig) (assoc opts :help true) opts)
        error (validate-opts opts)
        opts (if (env "YS_FORMAT") (assoc opts :to (env "YS_FORMAT")) opts)
        opts (if (env "YS_LOAD") (assoc opts :load true) opts)
        opts (if (:json opts) (assoc opts :to "json") opts)
        opts (if (:yaml opts) (assoc opts :to "yaml") opts)
        opts (if (:edn opts) (assoc opts :to "edn") opts)
        opts (if (and (not (:mode opts)) (seq (:eval opts)))
               (assoc opts :mode "code") opts)

        opts (if (env "YS_OUTPUT")
               (assoc opts :output (env "YS_OUTPUT")) opts)
        out (:output opts)
        opts (if (env "YS_STREAM")
               (assoc opts :stream (env "YS_STREAM")) opts)
        opts (if (and
                   out
                   (re-find #"\.(?:yml|yaml|json|csv|tsv|edn)$" out)
                   (not (:to opts)))
               (let [to (str/replace out #".*\.(\w+)$" "$1")
                     to (if (= "yml" to) "yaml" to)]
                 (assoc opts :to to))
               opts)
        opts (if (:to opts) (assoc opts :load true) opts)
        opts (if (env "YS_PRINT") (assoc opts :print true) opts)
        opts (if (and (env "YS_PRINT_EVAL")
                   (seq (:eval opts))) (assoc opts :print true) opts)
        opts (if (env "YS_STACK_TRACE") (assoc opts :stack-trace true) opts)
        opts (if (env "YS_UNORDERED") (assoc opts :unordered true) opts)
        opts (if (env "YS_XTRACE") (assoc opts :xtrace true) opts)

        is-e (seq (:eval opts))
        [arg1 arg2] args
        [opts args] (if (and
                          (not (seq (:eval opts)))
                          (looks-like-expr arg1))
                      (let [opts (assoc opts
                                   :eval [arg1]
                                   :mode (or (:mode opts) "code")
                                   :load true
                                   :to (or (:to opts) "yaml"))
                            args (rest args)
                            [opts args] (if (and arg2
                                              (not is--)
                                              (not (re-find #"^-" arg2)))
                                          [(assoc opts :file arg2) (rest args)]
                                          [opts args])
                            file (or (:file opts)
                                   (when is-- "-"))
                            opts (if file
                                   (assoc opts :file file)
                                   opts)]
                        [opts args])
                      [opts args])
        file (:file opts)
        file (cond
               file file
               (and (seq (:eval opts)) (not is-e) (not is--)) "-"
               filed filed)
        [file args] (if file
                      [file args]
                      (if (and (not is--)
                            (seq args)
                            (not (re-find #"^-." (first args))))
                        [(first args) (rest args)]
                        [nil args]))
        file (or file
               (and (not (seq (:eval opts))) (:load opts) "-"))
        opts (if file (assoc opts :file file) opts)
        args (if filed (rest args) args)
        args (vec args)]
    (when (env "YS_SHOW_OPTS")
      (println (yaml/generate-string{:opts opts :args args})))
    [opts args error errs help]))

(comment
  (take 2 (get-opts [".foo" "bar" "baz"]))
  (take 2 (get-opts [".foo"]))
  #__)

(defn -main [& argv]
  (global/reset-env nil)
  (let [[opts args error errs help] (get-opts argv)
        out (:output opts)]
    (reset! global/opts opts)
    (if out
      (with-open [out (io/writer out)]
        (binding [*out* out]
          (do-main opts args help error errs)))
      (do-main opts args help error errs))))

(comment
  )
