;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.cli library compiles into the `ys` command line binary for
;; YS.

(ns yamlscript.cli
  (:gen-class)
  (:require
   [babashka.fs :as fs]
   [babashka.process :refer [check process]]
   [clj-yaml.core :as yaml]
   [clojure.data.csv :as csv]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.stacktrace]
   [clojure.tools.cli :as cli]
   [yamlscript.command :as command]
   [yamlscript.common]
   [yamlscript.compiler :as compiler]
   [yamlscript.global :as global :refer [env]]
   [yamlscript.runtime :as runtime]
   [yamlscript.util :as util])
  (:refer-clojure))

(def yamlscript-version "0.2.7")

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
      (if (and (:stack @global/opts) (instance? Throwable e))
        (do
          (clojure.stacktrace/print-stack-trace e)
          (flush))
        (println (str/replace msg "java.lang.Exception: " "")))))
  (exit 1))

(defn todo [s & _xs]
  (err (str "--" s " not implemented yet.")))

(comment (todo "repl"))

;; ----------------------------------------------------------------------------
(def to-fmts
  #{"yaml" "json" "xml" "csv" "tsv" "edn"
    "clj" "glj" "go" "graal" "wasm"})

(def build-fmts
  #{"glj" "go" "graal" "wasm"})

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
  [["-l" "--load"
    "Evaluate input & print the result value
    →  default output format is compact JSON"]
   ["-r" "--run"
    "Run the YS code (this is the default action)"]
   ["-c" "--compile"
    "Compile YS code to source code or binary
    →  default output format is Clojure code"]

   ["-Y" "--yaml"
    "Short for --to=yaml"]
   ["-J" "--json"
    "Short for --to=json"]
   ["-T" "--to FORMAT"
    "Output format for --load or --compile:
    →  load: yaml, json, xml, csv, tsv, edn
    →  compile: clj, glj, go, graal, wasm"
    :validate
    [#(contains? to-fmts %1)
     (str "must be one of: "
       "yaml, json, xml, csv, tsv, edn, "
       "clj, glj, go, graal or wasm")]]

   ["-e" "--eval YSEXPR"
    "Evaluate a YS expression
    →  enables --mode=code by default
    →  multiple -e values are joined by newline"
    :default []
    :update-fn conj
    :multi true]
   ["-p" "--print"
    "Print the final evaluation result value"]
   ["-m" "--mode MODE"
    "Set input mode: code, data, or bare (for -e)"
    :validate
    [#(some #{%1} ["c" "code", "d" "data", "b" "bare"])
     "must be one of: c, code, d, data, b or bare"]]

   ["-i" "--input PATH"
    "Explicitly indicate input path (file or dir)"]
   ["-o" "--output PATH"
    "Output path for --load or --compile"]

   ["-I" "--include PATH"
    "Add directories to the library search path"
    :default []
    :update-fn conj
    :multi true]
   ["-s" "--stream"
    "Output all results from a multi-document stream"]
   ["-U" "--unordered"
    "Mappings don't preserve key order (faster)"]
   ["-C" "--clojure"
    "Don't compile input. Treat as Clojure code"]

   ["-x" "--xtrace"
    "Print each expression before evaluation"]
   ["-S" "--stack"
    "Print full stack trace for errors"]
   ["-d" nil
    "Debug all compilation stages"
    :id :debug
    :update-fn (constantly stages)]
   ["-D" "--debug STAGE"
    "Debug a specific compilation stage:
    →  parse, compose, resolve, build,
    →  transform, construct, print
    →can be used multiple times"
    :default {}
    :update-fn #(if (= "all" %2) stages (assoc %1 %2 true))
    :multi true
    :validate
    [#(or (contains? stages %1) (= %1 "all"))
     (str "must be one of: "
       (str/join ", " (keys stages))
       " or all")]]

   [nil "--install"
    "Install the libys shared library"]
   [nil "--upgrade"
    "Upgrade both ys and libys"]

   ["-q" "--quiet"
    "Less output"]
   ["-v" "--verbose"
    "More output"]

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

(defn do-install [opts args]
  (command/do-install opts args))

(defn do-upgrade [opts args]
  (command/do-upgrade opts args))

(defn do-version []
  (println (str "YS (YAMLScript) " yamlscript-version)))

(def help-heading (str "
ys - The YS Command Line Tool - v" yamlscript-version "

Usage: ys [<option...>] [<input-file>]

Options:

"))

(defn do-help [help]
  (let [help (str/replace help #"^" help-heading)
        help (str/replace help #"\[\]" "  ")
        help (str/replace help #"\{\}" "  ")
        help (str/replace help #"→" "                  ")
        ;; Insert blank lines in help text
        help (str/replace help #"\n  (-[YeiIxq])" "\n\n  $1")
        help (str/replace help #"\n  (.*--install)" "\n\n  $1")
        help (str/replace help #"\n  (.*--version)" "\n\n  $1")
        help (str/replace help #"    ([A-Z])" #(second %1))]
    (println help)))

(defn add-ecode-mode-tag
  "When we use 'ys -e' we don't want to require the leading !ys-0 tag.
   We can assume code mode by default.
   We need to check if -m was used or if there is already a tag."
  [opts ecode]
  (let [mode (:mode opts)
        code (str/join ""
               [(cond
                  (or
                    (re-find #"(?m)^---\s" ecode)
                    (re-find #"(?m)^(?:---\s+)?!(?:yamlscript/v0|YS-v0|ys-0)"
                             ecode)
                    (:clojure opts))
                  ""
                  (or (= "c" mode) (= "code" mode))
                  "--- !ys-0\n"
                  (or (= "d" mode) (= "data" mode))
                  "--- !ys-0:\n"
                  (or (= "b" mode) (= "bare" mode))
                  "---\n"
                  (:load opts)
                  ""
                  :else
                  "--- !ys-0\n")
                ecode])
        code (if (or (:clojure opts) (re-find #"^---(?:[ \n]|$)" code))
               code
               (str "---\n" code))]
    code))

(defn ecode-add-stream [opts code]
  (if (not (re-find #"\n" code))
    (cond
      (re-find #"^\.[\w\$]" code)
      (str (if (:stream opts)
             "stream()"
             "stream().last()") code)
      ,
      (re-find #"^\:\w" code)
      ,
      (str (if (:stream opts)
             "stream()"
             "stream().last()")
        (str/replace code #"^:([-\w]+)" ".$1()"))
      :else code)
    code))

(defn get-code [opts]
  (let [file (get opts :input)
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
                     (ecode-add-stream opts)
                     (add-ecode-mode-tag opts))
                   "\n"))
        opts (if (and e-code f-code)
               (assoc opts :load true)
               opts)
        _ (or e-code f-code
            (binding [*out* *err*]
              (println "Warning: No input found.")))
        code (str f-code e-code)
        file (or file "--eval")]
    [code file (:load opts)]))

(defn compile-code [code opts]
  (if (:clojure opts)
    code
    (try
      (if (seq (:debug opts))
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

(defn get-build-info [opts args]
  (let [in-file (or (first args) (:input opts))
        code (when (seq (:eval opts))
               (first (get-code opts)))
        _ (or in-file code
            (err "No input file specified"))
        in-file (if code "--eval" in-file)
        ys-bin (-> (java.lang.ProcessHandle/current) .info .command .get)
        ys-bin (if (re-find #"-openjdk-" ys-bin) "ys" ys-bin)]
    (merge opts {:code code,
                 :in-file in-file,
                 :ys-bin ys-bin})))

(defn do-compile [opts args]
  (let [to (:to opts)]
    (if (= to "clj")
      (let [[code _file _args] (get-compiled-code opts)]
        (println (pretty-clojure code)))
      (let [info (get-build-info opts args)]
        (case to
          "glj"   (command/do-build-go info)
          "go"    (command/do-build-go info)
          "graal" (command/do-build-graal info)
          "wasm"  (command/do-build-go info)
          ,       (die "Unknown output format: " to))))))

(def line (str (str/join (repeat 80 "-")) "\n"))

(defn do-run [opts args]
  (try
    (let [[code file load] (get-compiled-code opts)
          _ (when (env "YS_SHOW_COMPILE")
              (util/eprint (str line (pretty-clojure code) "\n" line)))
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
              (case (or (:to opts) "cjson")
                "cjson" (println (json/write-str result json-options))
                "yaml"  (println
                          (str
                            (when (> (count results) 1) "---\n")
                            (str/trim-newline
                              (yaml/generate-string
                                result
                                :dumper-options {:flow-style :block}))))
                "json"  (json/pprint result json-options)
                "xml"   (err "XML output format is not yet implemented")
                "csv"   (println
                          (with-open [s (java.io.StringWriter.)]
                            (csv/write-csv s result :separator \,)
                            (str s)))
                "tsv"   (println
                          (with-open [s (java.io.StringWriter.)]
                            (csv/write-csv s result :separator \tab)
                            (str s)))
                "edn"   (pp/pprint result)
                ,       (die "Unknown output format: " (:to opts))))))))
    (catch Exception e
      (global/reset-error-msg-prefix! "Error: ")
      (err e))))

(defn elide-empty [opts & keys]
  (reduce
    (fn [opts key]
      (if (empty? (key opts))
        (dissoc opts key)
        opts))
    opts keys))

(def all-opts
  #{:run :load :eval :compile
    :print :output :stream
    :to :json :yaml :unordered
    :mode :clojure
    :debug :stack :xtrace
    :install :upgrade
    :version :help})

(def action-opts
  #{:run :load :compile
    :install :upgrade
    :version :help})

(def eval-action-opts
  #{:run :load :compile})

(def format-opts
  #{:to :json :yaml})

(def info-opts
  #{:version :help})

(defn mutex [opts keys]
  (let [omap (zipmap (filter #(opts %) keys) (repeat true))]
    (when (> (count omap) 1)
      (let [[first second] (seq omap)
            first (str "--" (name (key first)))
            second (str "--" (name (key second)))]
        (str "Options " first " and " second
          " are mutually exclusive.")))))

(defn mutex1 [opts opt keys]
  (let [omap (zipmap (filter #(opts %) keys) (repeat true))]
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

(defn validate-opts [opts]
  (let [opts (elide-empty opts :eval :debug)]
    (or
      (mutex opts action-opts)
      (mutex opts format-opts)
      (mutex1 opts :help (set/difference all-opts #{:help}))
      (mutex1 opts :version (set/difference all-opts #{:version}))
      (mutex1 opts :mode info-opts)
      (mutex1 opts :eval (set/difference action-opts eval-action-opts))
      (needs opts :mode #{:eval})
      (mutex1 opts :print (set/difference action-opts #{:run}))
      (mutex1 opts :run format-opts)
      (mutex1 opts :to (set/difference action-opts #{:load :compile})))))

(defn looks-like-expr [file]
  (when (and
          (string? file)
          (re-find #"(?:^(?:\ |(?:_?[.:]\w))|\ $|\(|:\ )" file))
    (if (fs/exists? file)
      (err (str "'" file "' looks like an expression,\n"
             "but is also the name of a file. Use --eval or -e."))
      true)))

(defn get-opts [argv]
  (let [orig argv
        ;; Before parsing options, handle possible input file as first argument
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
        ;; Now parse options with cli/parse-opts
        options (cli/parse-opts args cli-options)
        {opts :options
         args :arguments
         help :summary
         errs :errors} options
        ;; Check for invalid option combinations and save any errors
        error (validate-opts opts)
        ;; Combine remaining arguments with parsed options
        args (concat args argv)
        ;; Default to --help if no arguments are provided
        opts (if-not (seq orig) (assoc opts :help true) opts)
        ;; Handle --yaml and --json
        opts (if (:yaml opts) (assoc opts :to "yaml") opts)
        opts (if (:json opts) (assoc opts :to "json") opts)
        ;; Default to code mode if --eval is used
        opts (if (and (not (:mode opts)) (seq (:eval opts)))
               (assoc opts :mode "code") opts)
        ;; Honor environment variables for options
        opts (if (env "YS_FORMAT") (assoc opts :to (env "YS_FORMAT")) opts)
        opts (if (env "YS_LOAD") (assoc opts :load true) opts)
        opts (if (env "YS_OUTPUT")
               (assoc opts :output (env "YS_OUTPUT")) opts)
        out (:output opts)
        opts (if (env "YS_STREAM")
               (assoc opts :stream (env "YS_STREAM")) opts)
        ;; Default to output format based on output file extension
        opts (if (and
                   out
                   (not (:to opts))
                   (re-find
                     #"\.(?:yml|yaml|json|csv|tsv|edn|clj|glj|go|wasm)$"
                     out))
               (let [to (str/replace out #".*\.(\w+)$" "$1")
                     to (if (= "yml" to) "yaml" to)]
                 (assoc opts :to to))
               opts)
        ;; If --to is set, default to --load or --compile
        opts (if-not (seq (filter #(contains? opts %1) action-opts))
               (cond
                 (re-matches #"(?:yaml|json|xml|csv|tsv|edn)"
                   (or (:to opts) ""))
                 (assoc opts :load true)
                 ,
                 (re-matches #"(?:clj|glj|go|graal|wasm)"
                   (or (:to opts) ""))
                 (assoc opts :compile true)
                 ,
                 :else opts)
               opts)
        ;; If first argument is a file, default to "run" action
        opts (if (and filed
                   (not (seq (filter #(contains? opts %1) action-opts))))
               (assoc opts :run true)
               opts)

        ;; Honor environment variables for options
        opts (if (env "YS_PRINT") (assoc opts :print true) opts)
        opts (if (and (env "YS_PRINT_EVAL")
                   (seq (:eval opts))) (assoc opts :print true) opts)
        opts (if (env "YS_STACK_TRACE") (assoc opts :stack true) opts)
        opts (if (env "YS_UNORDERED") (assoc opts :unordered true) opts)
        opts (if (env "YS_XTRACE") (assoc opts :xtrace true) opts)

        ;; Handle --eval
        is-e (seq (:eval opts))
        ;; Assume --eval if first argument looks like an expression
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
                                          [(assoc opts :input arg2) (rest args)]
                                          [opts args])
                            file (or (:input opts)
                                   (when is-- "-"))
                            opts (if file
                                   (assoc opts :input file)
                                   opts)]
                        [opts args])
                      [opts args])
        ;; If no action is determined by now, default to --run
        opts (if-not (seq (filter #(contains? opts %1) action-opts))
               (assoc opts :run true)
               opts)
        ;; Set default output format for --load or --compile
        opts (cond
               (:load opts) (assoc opts :to (or (:to opts) "cjson"))
               (:compile opts) (assoc opts :to (or (:to opts) "clj"))
               :else opts)
        ;; Assume stdin ("-") if input file is not provided
        file (:input opts)
        file (cond
               file file
               (and (seq (:eval opts)) (not is-e) (not is--)) "-"
               filed filed)
        ;; Determine input file and arguments
        [file args] (if file
                      [file args]
                      (if (and (not is--)
                            (seq args)
                            (not (re-find #"^-." (first args))))
                        [(first args) (rest args)]
                        [nil args]))
        file (or file
               (and (not (seq (:eval opts))) (:load opts) "-"))
        opts (if file (assoc opts :input file) opts)
        args (vec (if filed (rest args) args))]
    (when (env "YS_SHOW_OPTS")
      (util/eprintln (yaml/generate-string {:opts opts :args args})))
    [opts args error errs help]))

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
        :run (do-run opts args)
        :compile (do-compile opts args)
        :load (do-run opts args)
        (do-run opts args)))))

(defn -main [& argv]
  (global/reset-env nil)
  (let [[opts args error errs help] (get-opts argv)
        out (:output opts)
        to (:to opts)
        out (if (contains? build-fmts to) *out* out)]
    (reset! global/opts opts)
    (if out
      (with-open [out (io/writer out)]
        (binding [*out* out]
          (do-main opts args help error errs)))
      (do-main opts args help error errs))))

(comment
  )
