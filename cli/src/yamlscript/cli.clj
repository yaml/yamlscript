(ns yamlscript.cli
  (:gen-class)
  (:require
   [yamlscript.core :as core]
   [sci.core :as sci]
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [clojure.tools.cli :as cli]))

;; ----------------------------------------------------------------------------
(defn in-repl []
  (some
   #(and
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

(defn www [o] (println (str "---\n" o "\n...")) o)

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
(def to-fmts #{"json" "yaml"})
(def stages
  {"parse" yamlscript.parser/parse
   "compose" yamlscript.composer/compose
   "resolve" yamlscript.resolver/resolve
   "build" yamlscript.builder/build
   "expand" yamlscript.expander/expand
   "construct" yamlscript.constructor/construct
   "print" yamlscript.printer/print
   })

;; See https://clojure.github.io/tools.cli/#clojure.tools.cli/parse-opts
(def cli-options
  [;
   [nil "--run" "Compile and run a YAMLScript file"]
   ["-e" "--eval YSEXPR" "Evaluate a YAMLScript expression"
    :default []
    :update-fn conj
    :multi true]
   ["-c" "--compile" "Compile YAMLScript to Clojure"]
   ["-r" "--return" "Print the return value"]

   ["-N" "--nrepl" "Start a new nREPL server"]
   ["-R" "--repl" "Connect console to the current nREPL server"]
   ["-K" "--kill" "Stop the nREPL server"]

   ["-o" "--file" "YAMLScript file to compile into"]
   ["-t" "--to FORMAT" "Output format"
    :validate
    [#(contains? to-fmts %)
     (str "must be one of: " (str/join ", " to-fmts))]]
   ["-s" "--stage STAGE" "Display the result of stage(s)"
    :default {}
    :update-fn #(if (= "all" %2) stages (assoc %1 %2 true))
    :multi true
    :validate
    [#(or (contains? stages %) (= % "all"))
     (str "must be one of: " (str/join ", " (keys stages)))]]

   ["-X" "--debug" "Debug mode: print full stack trace for errors"]

   ["-V" "--version" "Print version and exit"]
   ["-h" "--help"]])

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
        code (if (:eval opts)
               (->> opts :eval (str/join "\n"))
               code)
        code (str code "\n" (str/join "\n" (map slurp args)))]
    code))

(defn compile-code [code opts]
  (try
    (reduce
     (fn [stage-input [stage-name stage-fn]]
       (let [stage-output (stage-fn stage-input)]
         (when (get (:stage opts) stage-name)
           (println (str "*** " stage-name " output ***"))
           (pp/pprint stage-output)
           (println ""))
         stage-output)
       (stage-fn stage-input))
     code stages)
    (catch Exception e (print-exception e opts nil))))

(comment
  (-main "-s" "all" "-e" "println: 123"))

(defn run-clj [clj opts]
  (let [sw (java.io.StringWriter.)
        clj (str/trim-newline clj)]
    (sci/binding [sci/out sw]
      (let [result (sci/eval-string clj)]
        (print (str sw))
        (flush)
        result))))

(defn do-run [opts args]
  (let [result
        (-> (get-code opts args)
            (compile-code opts)
            (run-clj opts))]
    (when (or (seq (:eval opts)) (:return opts))
      (when (or result (:return opts))
        (pp/pprint result)))))

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

(defn do-default [opts args]
  (if (or (seq (:eval opts)) (seq args))
    (do-run opts args)
    (do-repl opts)))

(defn -main [& args]
  (let [options (cli/parse-opts args cli-options)
        {opts :options
         args :arguments
         help :summary
         errs :errors} options]
    (when-not (cond (seq errs) (do-error errs help)
                    (:help opts) (do-help help)
                    (:version opts) (do-version)
                    (:run opts) (do-run opts args)
                    (:repl opts) (do-repl opts)
                    (:connect opts) (do-connect opts args)
                    (:kill opts) (do-kill opts args)
                    (:compile opts) (do-compile opts args)
                    (:nrepl opts) (do-nrepl opts args)
                    (:compile-to opts) (do-compile-to opts args)
                    :else (do-default opts args)))))

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
  (-main "--to=json")
  (-main "-c" "test/hello.ys")
  (-main "test/hello.ys")
  (-main "test/hello.ys")
  (-main "-e" "println: 123")
  (-main "-ce" "foo:")
  (-main "-Xce" "foo:")
  (-main "--repl" "-t" "json")
  (-main "--repl")
  (-main "-Q")
  (-main)
  *file*
  )
