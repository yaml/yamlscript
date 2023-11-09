;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.cli library compiles into the `ys` command line binary for
;; YAMLScript.

(ns yamlscript.cli
  (:gen-class)
  (:require
   ;; This goes first for pprint/graalvm patch (prevents binary bloating)
   [yamlscript.core]
   ;; For www debugging
   [yamlscript.debug :refer [www]]
   ;; Data printers
   [clj-yaml.core :as yaml]
   [clojure.data.json :as json]
   [clojure.pprint :as pp]
   ;; String helper functions
   [clojure.string :as str]
   ;; Command line options parser
   [clojure.tools.cli :as cli]
   ;; Small Clojure interpreter runtime for YAMLScript evaluation
   [sci.core :as sci]
   ;; YAMLScript compiler stack stages (in order)
   [yamlscript.parser]
   [yamlscript.composer]
   [yamlscript.resolver]
   [yamlscript.builder]
   [yamlscript.transformer]
   [yamlscript.constructor]
   [yamlscript.printer]))

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

(defn die [& msg]
  (println (apply str msg))
  (exit 1))

(defn todo [s]
  (die "--" s " not implemented yet."))

(defn stdin-ready? []
  (try
    (.ready ^clojure.lang.LineNumberingPushbackReader *in*)
    (catch Exception e false)))

(defn print-exception
  ([^String s opts] (print-exception (Exception. s) opts nil))
  ([^Exception e opts _]
   (println (str "Error: " (or (.getCause e) (.getMessage e))))
   (when (:debug opts)
     (println
       (apply
         str
         (interpose
           "\n"
           (apply conj ["Stack trace:"] (.getStackTrace e))))))
   (exit 1)))

;; ----------------------------------------------------------------------------
(def to-fmts #{"json" "yaml" "edn"})
(def stages
  {"parse" yamlscript.parser/parse
   "compose" yamlscript.composer/compose
   "resolve" yamlscript.resolver/resolve
   "build" yamlscript.builder/build
   "transform" yamlscript.transformer/transform
   "construct" yamlscript.constructor/construct
   "print" yamlscript.printer/print})

;; See https://clojure.github.io/tools.cli/#clojure.tools.cli/parse-opts
(def cli-options
  [;
   ["-r" "--run"
    "Compile and evaluate a YAMLScript file"]
   ["-l" "--load"
    "Output the evaluated YAMLScript value"]
   ["-c" "--compile"
    "Compile YAMLScript to Clojure"]
   ["-e" "--eval YSEXPR"
    "Evaluate a YAMLScript expression"
    :default []
    :update-fn conj
    :multi true]

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
    :update-fn #(if (= "all" %2) stages (assoc %1 %2 true))
    :multi true
    :validate
    [#(or (contains? stages %) (= % "all"))
     (str "must be one of: "
       (str/join ", " (keys stages))
       " or all")]]

   [nil "--version"
    "Print version and exit"]
   ["-h" "--help"
    "Print this help and exit"]])

(defn unsupported-opts [opts unsupported]
  (doall
    (for [opt unsupported
          :let [kw (keyword (subs opt 2))]
          :when (kw opts)]
      (print-exception
        (str "Option " opt " is not supported in this context.")
        opts))))

(defn do-error [errs help]
  (let [errs (apply list "Error(s):" errs)]
    (println (str/join "\n* " errs))
    (println (str "\nOptions:\n" help))
    (exit 1)))

(defn do-version []
  (println "YAMLScript 0.1.0"))

(defn do-help [help]
  (println help))

(defn get-code [opts args]
  (let [code ""
        code (if (seq (:eval opts))
               (let [eval (->> opts :eval (str/join "\n"))
                     eval (if (or (= "" eval)
                                (re-find #"^(--- |!yamlscript)" eval))
                            eval
                            (str "--- !yamlscript/v0\n" eval))]
                 eval)
               code)
        code (str code "\n"
               (str/join "\n"
                 (map
                   #(if (= "-" %) (slurp *in*) (slurp %))
                   args)))
        code (if (stdin-ready?)
               (str code "\n" (slurp *in*))
               code)]
    code))

(defn compile-code [code opts]
  (try
    (reduce
      (fn [stage-input [stage-name stage-fn]]
        (when (get (:debug-stage opts) stage-name)
          (println (str "*** " stage-name " output ***")))
        (let [stage-output (stage-fn stage-input)]
          (when (get (:debug-stage opts) stage-name)
            (pp/pprint stage-output)
            (println ""))
          stage-output)
        (stage-fn stage-input))
      code stages)
    (catch Exception e (print-exception e opts nil))))

(defn run-clj [clj]
  (let [sw (java.io.StringWriter.)
        clj (str/trim-newline clj)]
    (if (= "" clj)
      ""
      (sci/binding [sci/out sw]
        (let [result (sci/eval-string clj)]
          (print (str sw))
          (flush)
          result)))))

(defn do-run [opts args]
  (try
    (let [clj (-> (get-code opts args)
                (compile-code opts))
          is-empty (= "" clj)
          result (run-clj clj)]
      (when (and (not is-empty) (seq (filter #(% opts) [:load :to])))
        (case (:to opts)
          "yaml" (println
                   (str/trim-newline
                     (yaml/generate-string
                       result
                       :dumper-options {:flow-style :block})))
          "json" (json/pprint result)
          "edn"  (pp/pprint result)
          ,      (println (json/write-str result)))))
    (catch Exception e (print-exception e opts nil))))

(defn do-compile [opts args]
  (-> (get-code opts args)
    (compile-code opts)
    str/trim-newline
    println))

(defn do-repl [opts]
  (unsupported-opts opts ["--to"])
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

(defn -main [& args]
  (let [options (cli/parse-opts args cli-options)
        {opts :options
         args :arguments
         help :summary
         errs :errors} options
        opts (if (:json opts) (assoc opts :to "json") opts)
        opts (if (:yaml opts) (assoc opts :to "yaml") opts)
        opts (if (:edn opts) (assoc opts :to "edn") opts)
        help (str/replace help #"^"
               "Usage: ys [options] [file]\n\nOptions:\n")
        help (str/replace help #"\[\]" "  ")
        help (str/replace help #"\{\}" "  ")
        help (str/replace help #"\n  -o" "\n\n  -o")
        help (str/replace help #"\n  -J" "\n\n  -J")
        help (str/replace help #"\n  -R" "\n\n  -R")
        help (str/replace help #"\n  -X" "\n\n  -X")
        help (str/replace help #"    ([A-Z])" #(second %))]
    (cond (seq errs) (do-error errs help)
          (:help opts) (do-help help)
          (:version opts) (do-version)
          (:run opts) (do-run opts args)
          (:load opts) (do-run opts args)
          (:repl opts) (do-repl opts)
          (:connect opts) (do-connect opts args)
          (:kill opts) (do-kill opts args)
          (:compile opts) (do-compile opts args)
          (:nrepl opts) (do-nrepl opts args)
          (:compile-to opts) (do-compile-to opts args)
          :else (do-default opts args help))))

(comment
  (-> "(do (println \"abcd\") 123)\n"
    (#(let [sw (java.io.StringWriter.)]
        (sci/binding [sci/out sw]
          (let [result (sci/eval-string %)]
            (println (str sw))
            result)))))
  (-main "-e" "println: 12345" "-e" "identity: 67890")
  (-main "--compile=foo")
  (-main "--compile-to=parse")
  (-main "--help")
  (-main "--version")
  (-main "-Je" "range: 30")
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
  (-main)
  *file*
  )
