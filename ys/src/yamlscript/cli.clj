;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.cli library compiles into the `ys` command line binary for
;; YAMLScript.

(ns yamlscript.cli
  (:gen-class)
  (:require
   ;; This goes first for pprint/graalvm patch (prevents binary bloating)
   [yamlscript.compiler :as compiler]
   ;; For www debugging
   [yamlscript.debug :refer [www]]
   ;; Data printers
   [clj-yaml.core :as yaml]
   [clojure.data.json :as json]
   [clojure.set :as set]
   [clojure.pprint :as pp]
   ;; String helper functions
   [clojure.string :as str]
   ;; Command line options parser
   [clojure.tools.cli :as cli]
   ;; Small Clojure interpreter runtime for YAMLScript evaluation
   [sci.core :as sci]
   [ys.core]))

;; ----------------------------------------------------------------------------
(defn in-repl []
  (some #(and
           (= "clojure.main$repl" (.getClassName ^StackTraceElement %))
           (= "doInvoke" (.getMethodName ^StackTraceElement %)))
    (.getStackTrace (Thread/currentThread))))

(defn exit [n]
  (if (in-repl)
    (str "*** exit " n " ***")
    (System/exit n)))

(defn die
  ([^String s] (die (Exception. s) {} nil))
  ([^String s opts] (die (Exception. s) opts nil))
  ([^Exception e opts _]
   (let [msg (str "Error: " (or (.getCause e) (.getMessage e)))
         msg (if (:debug opts)
               (apply
                 str
                 msg
                 (interpose
                   "\n"
                   (apply conj ["Stack trace:"] (.getStackTrace e))))
               msg)]
     (throw (Exception. ^String msg)))))

(defn todo [s]
  (die "--" s " not implemented yet."))

(defn stdin-ready? []
  (try
    (.ready ^clojure.lang.LineNumberingPushbackReader *in*)
    (catch Exception e false)))

;; ----------------------------------------------------------------------------
(def to-fmts #{"json" "yaml" "edn"})

;; See https://clojure.github.io/tools.cli/#clojure.tools.cli/parse-opts
(def cli-options
  [;
   ["-r" "--run"
    "Compile and evaluate a YAMLScript file (default)"]
   ["-l" "--load"
    "Output the evaluated YAMLScript value"]
   ["-c" "--compile"
    "Compile YAMLScript to Clojure"]
   ["-e" "--eval YSEXPR"
    "Evaluate a YAMLScript expression"
    :default []
    :update-fn conj
    :multi true]
   [nil "--clj CLJEXPR"
    "Evaluate a Clojure expression"]

   ["-m" "--mode MODE"
    "Add a mode tag: script, yaml, or data"
    :validate
    [#(some #{%} ["s" "script", "y" "yaml", "d" "data"])
     (str "must be one of: s, script, y, yaml, d or data")]]
   ["-p" "--print"
    "Print the result of --run in script mode"]

   ["-o" "--output"
    "Output file for --load or --compile"]
   ["-t" "--to FORMAT"
    "Output format for --load"
    :validate
    [#(contains? to-fmts %)
     (str "must be one of: " (str/join ", " to-fmts))]]

   ["-J" "--json"
    "Output JSON for --load"]
   ["-Y" "--yaml"
    "Output YAML for --load"]
   ["-E" "--edn"
    "Output EDN for --load"]

   ["-R" "--repl"
    "Start an interactive YAMLScript REPL"]
   ["-N" "--nrepl"
    "Start a new nREPL server"]
   ["-K" "--kill"
    "Stop the nREPL server"]

   ["-X" "--debug"
    "Debug mode: print full stack trace for errors"]
   ["-x" "--debug-stage STAGE"
    "Display the result of stage(s)"
    :default {}
    :update-fn #(if (= "all" %2) compiler/stages (assoc %1 %2 true))
    :multi true
    :validate
    [#(or (contains? compiler/stages %) (= % "all"))
     (str "must be one of: "
       (str/join ", " (keys compiler/stages))
       " or all")]]

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

(defn do-version []
  (println "YAMLScript 0.1.0"))

(defn do-help [help]
  (println help))

(defn add-ys-mode-tag [opts code]
  (let [mode (:mode opts)]
    (str/join ""
      [(cond
         (or (str/starts-with? code "--- !yamlscript/v0")
           (str/starts-with? code "!yamlscript/v0"))
         ""
         (or (= "s" mode) (= "script" mode))
         "--- !yamlscript/v0\n"
         (or (= "y" mode) (= "yaml" mode))
         "--- !yamlscript/v0/\n"
         (or (= "d" mode) (= "data" mode))
         "--- !yamlscript/v0/data\n"
         (some opts [:load :to :json :yaml :edn])
         ""
         :else "--- !yamlscript/v0\n")
       code])))

(defn get-code [opts args]
  (let [
        code (if (seq (:eval opts))
               (->> opts :eval (str/join "\n") (add-ys-mode-tag opts))
               "")
        code (str
               (when-not (empty? code) (str code "\n"))
               (str/join "\n"
                 (map
                   #(if (= "-" %) (slurp *in*) (slurp %))
                   args)))
        code (if (stdin-ready?)
               (str
                 (when-not (empty? code) (str code "\n"))
                 (slurp *in*))
               code)]
    code))

(defn compile-code [code opts]
  (try
    (if (empty? (:debug-stage opts))
      (compiler/compile code)
      (binding [compiler/*debug* (:debug-stage opts)]
        (compiler/compile-debug code)))
    (catch Exception e (die e opts nil))))

(sci/alter-var-root sci/out (constantly *out*))
(sci/alter-var-root sci/err (constantly *err*))
(sci/alter-var-root sci/in (constantly *in*))

(def ys-ns (sci/create-ns 'ys))
(def ys-ns-vars
  {#__
   'compile (sci/copy-var ys.core/ys-compile ys-ns)
   'eval (sci/copy-var ys.core/ys-eval ys-ns)
   'load (sci/copy-var ys.core/ys-load ys-ns)
   #__})

(def clojure-core (sci/create-ns 'clojure.core))
(def clojure-core-vars
  {#__
   'pprint (sci/copy-var clojure.pprint/pprint clojure-core)
   'slurp (sci/copy-var clojure.core/slurp clojure-core)
   'spit (sci/copy-var clojure.core/spit clojure-core)
   'say (sci/copy-var ys.core/say clojure-core)
   #__})

(def ys-core (sci/create-ns 'clojure.core))
(def ys-core-vars (sci/copy-ns ys.core ys-core))

(def sci-ctx
  (sci/init {:namespaces
             {#__
              'clojure.core clojure-core-vars
              'ys.core ys-core-vars
              'ys ys-ns-vars
              #__}}))

(defn run-clj [clj]
  (let [clj (str/trim-newline clj)]
    (if (= "" clj)
      ""
      (let [result (sci/eval-string* sci-ctx clj)]
        result))))

(def json-options
  {:escape-unicode false
   :escape-js-separators false
   :escape-slash false})

(defn do-run [opts args]
  (try
    (let [clj (if (:clj opts)
                (:clj opts)
                (-> (get-code opts args)
                  (compile-code opts)))
          result (run-clj clj)]
      (if (:print opts)
        (println result)
        (when (or (:print opts)
                (and (not (= "" clj))
                  (some opts [:load :to :json :yaml :edn])))
          (case (:to opts)
            "yaml" (println
                     (str/trim-newline
                       (yaml/generate-string
                         result
                         :dumper-options {:flow-style :block})))
            "json" (json/pprint result json-options)
            "edn"  (pp/pprint result)
            ,      (println (json/write-str result json-options))))))
    (catch Exception e (die e opts nil))))

(defn do-compile [opts args]
  (-> (get-code opts args)
    (compile-code opts)
    str/trim-newline
    println))

(defn do-repl [opts]
  (todo "repl"))

(defn do-nrepl [opts args]
  (todo "nrepl"))

(defn do-connect [opts args]
  (todo "connect"))

(defn do-kill [opts args]
  (todo "kill"))

(defn do-compile-to [opts args]
  (todo "compile-to"))

(defn do-default [opts args help]
  (if (or
        (seq (:eval opts))
        (seq args)
        (stdin-ready?))
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
        (die (str "Options " first " and " second
               " are mutually exclusive."))))))

(defn mutex1 [opts opt keys]
  (let [omap (reduce #(if (%2 opts)
                        (assoc %1 %2 true)
                        %1) {} keys)]
    (when (and (opt opts) (> (count omap) 0))
      (let [[second] (seq omap)
            first (str "--" (name opt))
            second (str "--" (name (key second)))]
        (die (str "Options " first " and " second
               " are mutually exclusive."))))))

(comment (-main "--mode=script"))

(defn k2o [k]
  (str "--" (name k)))

(defn needs [opts opt keys]
  (let [omap (set keys)]
    (when (and (opt opts) (not (some opts keys)))
      (let [first (str "--" (name opt))]
        (die (str "Option " first " requires "
               (if (= 1 (count keys))
                 (k2o (clojure.core/first keys))
                 " one of ...")))))))

(def all-opts
  #{:run :load :compile :eval
    :clj :mode :print :output
    :to :json :yaml :edn
    :repl :nrepl :kill
    :debug :debug-stage
    :version :help})

(def action-opts
  #{:run :load :compile :clj
    :repl :nrepl :kill
    :version :help})

(def eval-action-opts
  #{:run :load :compile :clj})

(def format-opts
  #{:to :json :yaml :edn})

(def repl-opts
  #{:repl :nrepl :kill})

(def info-opts
  #{:version :help})

(defn elide-empty [opt opts]
  (if (empty? (get opts opt))
    (dissoc opts opt)
    opts))

(defn validate-opts [opts]
  (let [opts (elide-empty :eval opts)
        opts (elide-empty :debug-stage opts)]
    (try
      (mutex opts action-opts)
      (mutex opts format-opts)
      (mutex1 opts :help (set/difference all-opts #{:help}))
      (mutex1 opts :version (set/difference all-opts #{:version}))
      (mutex1 opts :mode (set/union info-opts repl-opts))
      (mutex1 opts :eval (set/difference action-opts eval-action-opts))
      (needs opts :mode #{:eval})
      (mutex1 opts :print (set/difference action-opts #{:run}))
      (mutex1 opts :to (set/difference action-opts #{:load :compile}))
      nil
      (catch Exception e (.getMessage e)))))

(defn -main [& args]
  (let [options (cli/parse-opts args cli-options)
        {opts :options
         args :arguments
         help :summary
         errs :errors} options
        opts (if (not (some opts (vec (seq action-opts))))
               (assoc opts :run true)
               opts)
        error (validate-opts opts)
        opts (if (:json opts) (assoc opts :to "json") opts)
        opts (if (:yaml opts) (assoc opts :to "yaml") opts)
        opts (if (:edn opts) (assoc opts :to "edn") opts)
        help (str/replace help #"^"
               "Usage: ys [options] [file]\n\nOptions:\n")
        help (str/replace help #"\[\]" "  ")
        help (str/replace help #"\{\}" "  ")
          ;; Insert blank lines in help text
        help (str/replace help #"\n  (-[omJRX])" "\n\n  $1")
        help (str/replace help #"    ([A-Z])" #(second %))]
    (cond
      error (do-error [error] help)
      (seq errs) (do-error errs help)
      (:help opts) (do-help help)
      (:version opts) (do-version)
      (:run opts) (do-run opts args)
      (:load opts) (do-run opts args)
      (:clj opts) (do-run opts args)
      (:repl opts) (do-repl opts)
      (:connect opts) (do-connect opts args)
      (:kill opts) (do-kill opts args)
      (:compile opts) (do-compile opts args)
      (:nrepl opts) (do-nrepl opts args)
      (:compile-to opts) (do-compile-to opts args)
      :else (do-default opts args help))))

(comment
  (-main)
  (-main
    ;"--run"
    ;"--load"
    ;"--to=json"
    ;"--compile"
    "--mode=script"
    ;"--help"
    "-e" "say: 123"
    ;"ys.load"
    )
  (-main "-ms" "-le" "println: 12345" "-e" "identity: 67890")
  (-main "--compile=foo")
  (-main "--compile-to=parse")
  (-main "--help")
  (-main "--version")
  (-main "-ms" "-Je" "range: 30")
  (-main "-c" "test/hello.ys")
  (-main "test/hello.ys")
  (-main "-e" "println: 123")
  (-main "-ce" "foo:")
  (-main "-Xce" "foo:")
  (-main "--repl" "-t" "json")
  (-main "--repl")
  (-main "-Q")
  (-main "-xall" "-e" "println: 123")
  (-main "--load" "test/hello.ys")
  (-main "--load" "-ms" "-e" "identity: inc(41)")
  (-main "-e" "say((ys.compile \"[2,4,6]\"))")
  *file*
  )
