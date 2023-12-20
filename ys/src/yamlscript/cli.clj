;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.cli library compiles into the `ys` command line binary for
;; YAMLScript.

(ns yamlscript.cli
  (:gen-class)
  (:import java.lang.ProcessHandle)
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

(def yamlscript-version "0.1.29")

(def testing (atom false))

;; ----------------------------------------------------------------------------
(defn in-repl []
  (some #(and
           (= "clojure.main$repl" (.getClassName ^StackTraceElement %))
           (= "doInvoke" (.getMethodName ^StackTraceElement %)))
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

;; See https://clojure.github.io/tools.cli/#clojure.tools.cli/parse-opts
(def cli-options
  [;
   ["-r" "--run"
    "Compile and evaluate a YAMLScript file (default)"]
   ["-l" "--load"
    "Output the evaluated YAMLScript value"]
   ["-e" "--eval YSEXPR"
    "Evaluate a YAMLScript expression"
    :default []
    :update-fn conj
    :multi true]

   ["-c" "--compile"
    "Compile YAMLScript to Clojure"]
   ["-C" "--native"
    "Compile to a native binary executable"]
   [nil "--clj"
    "Treat input as Clojure code"]

   ["-m" "--mode MODE"
    "Add a mode tag: code, data, or bare (only for --eval/-e)"
    :validate
    [#(some #{%} ["c" "code", "d" "data", "b" "bare"])
     (str "must be one of: c, code, d, data, b or bare")]]
   ["-p" "--print"
    "Print the result of --run in code mode"]

   ["-o" "--output FILE"
    "Output file for --load, --compile or --native"]
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

   #_["-R" "--repl"
      "Start an interactive YAMLScript REPL"]
   #_["-N" "--nrepl"
      "Start a new nREPL server"]
   #_["-K" "--kill"
      "Stop the nREPL server"]

   ["-X" "--stack-trace"
    "Print full stack trace for errors"]
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

(declare add-ys-mode-tag)
(defn get-native-info [opts args]
  (let [in-file (when (seq args) (first args))
        code (when (seq (:eval opts))
               (str
                 (->> opts
                   :eval
                   (str/join "\n"))
                 "\n"))]
    (or in-file code
      (die "No input file specified"))
    (let [in-file (if code "EVAL.ys" in-file)]
      (or (re-find #"\.ys$" in-file)
        (die "Input file must end in .ys"))
      [in-file code])))

(defn do-native [opts args]
  (let [path (-> (java.lang.ProcessHandle/current) .info .command .get)
        cmd (if (re-find #"-openjdk-" path)
              (str "ys-sh-" yamlscript-version)
              (str/replace path #"/[^/]*$"
                (str "/ys-sh-" yamlscript-version)))
        [in-file code] (get-native-info opts args)
        out-file (some identity
                   [(:output opts)
                    (and in-file (str/replace in-file #"\.ys$" ""))])
        path (if (re-find #"-openjdk-" path) "ys" path)]
    #_(print
      (str
        (format "Compiling '%s' -> '%s'\n\n" in-file out-file)
        (format "$ %s --compile-to-native %s %s %s\n\n"
          cmd in-file out-file yamlscript-version)))
    (flush)
    (exec {:extra-env {"YS_BIN" path
                       "YS_CODE" (or code "")}}
      cmd "--compile-to-native"
      in-file out-file yamlscript-version)))

(defn do-version []
  (println (str "YAMLScript " yamlscript-version)))

(def help-heading
  "ys - The YAMLScript (YS) Command Line Tool

Usage: ys [options] [file]

Options:
")

(defn do-help [help]
  (let [help (str/replace help #"^" help-heading)
        help (str/replace help #"\[\]" "  ")
        help (str/replace help #"\{\}" "  ")
        ;; Insert blank lines in help text
        help (str/replace help #"\n  (-[comJRX])" "\n\n  $1")
        help (str/replace help #"    ([A-Z])" #(second %))]
    (println help)))

(defn add-ys-mode-tag [opts code]
  (let [mode (:mode opts)]
    (str/join ""
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
       code])))

(defn get-code [opts args]
  (cond
    (seq (:eval opts))
    [(str
       (->> opts
         :eval
         (str/join "\n")
         (add-ys-mode-tag opts))
       "\n")
     "/EVAL" args]
    ,
    (seq args)
    (let [[file & args] args
          code (str
                 (if (= "-" file)
                   (slurp *in*)
                   (slurp file))
                 "\n")]
      [code file args])
    ,
    (:stdin opts)
    [(:stdin opts) "/STDIN" args]
    ,
    :else
    (let [code (get-stdin)]
      (if code
        [code "/STDIN" args]
        (do
          (binding [*out* *err*]
            (println "Warning: No input found."))
          ["" "EMPTY" args])))))


(defn compile-code [code opts]
  (if (:clj opts)
    code
    (try
      (if (empty? (:debug-stage opts))
        (compiler/compile code)
        (binding [compiler/*debug* (:debug-stage opts)]
          (compiler/compile-debug code)))
      (catch Exception e (die e opts)))))

(def json-options
  {:escape-unicode false
   :escape-js-separators false
   :escape-slash false})

(defn do-run [opts args]
  (try
    (let [[code file args] (get-code opts args)
          clj (compile-code code opts)
          result (runtime/eval-string clj file args)]
      (if (:print opts)
        (pp/pprint result)
        (when (and (:load opts) (not (= "" clj)))
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
  (let [[code file args] (get-code opts args)]
    (-> code
      (compile-code opts)
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
        opts (if (:to opts) (assoc opts :load true) opts)]
    (cond
      error (do-error [(str "Error: " error)] help)
      (seq errs) (do-error errs help)
      (:help opts) (do-help help)
      (:version opts) (do-version)
      (:native opts) (do-native opts args)
      (:run opts) (do-run opts args)
      (:load opts) (do-run opts args)
      (:compile opts) (do-compile opts args)
      (:repl opts) (do-repl opts)
      (:connect opts) (do-connect opts args)
      (:kill opts) (do-kill opts args)
      (:nrepl opts) (do-nrepl opts args)
      :else (do-default opts args help))))

(comment
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
