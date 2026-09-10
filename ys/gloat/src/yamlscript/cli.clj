;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.cli
  (:require
   [clojure.string :as str]
   [yamlscript.compiler :as compiler]
   [yamlscript.glojure-runtime :as runtime]
   [yamlscript.global :as global]
   [yamlscript.module.pprint :as pprint]
   [yamlscript.process :as process]
   [yamlscript.util.install :as util]
   [yamlscript.util-platform :as util-platform]
   [yamlscript.module.csv :as csv]
   [ys.v0.global :as v0-global]
   [ys.v0.ipc :as ipc]
   [ys.v0.json :as json]
   [ys.v0.yaml :as yaml]))

(def yamlscript-version "0.2.32")

(def usage-text
  (str
    "\nys - The YS Command Line Tool - v" yamlscript-version "\n\n"
    "Usage: ys [<option...>] [<file>]\n\n"
    "Options:\n\n"
    "  -e, --eval YSEXPR        Evaluate a YS expression\n"
    "                             multiple -e values are joined by newline\n"
    "  -l, --load               Output the (compact) JSON of YS evaluation\n"
    "  -f, --file FILE          Explicitly indicate input file\n\n"
    "  -c, --compile            Compile YS to Clojure\n"
    "  -p, --print              Print the final evaluation result value\n"
    "  -o, --output FILE        Write load or compile output to FILE\n"
    "  -s, --stream             Output all multi-document results\n\n"
    "  -T, --to FORMAT          Output format or compile target\n"
    "                             json, yaml, csv, tsv, edn\n"
    "                             bb, clj, star\n"
    "  -J, --json               Output pretty JSON for --load\n"
    "  -Y, --yaml               Output YAML for --load\n"
    "  -U, --unordered          Do not preserve mapping key order\n\n"
    "  -m, --mode MODE          Add code, data, or bare mode for -e\n"
    "  -C, --clojure            Treat input as Clojure code\n\n"
    "  -d                       Debug all compilation stages\n"
    "  -D, --debug-stage STAGE  Debug one compilation stage\n"
    "  -S, --stack-trace        Print full stack traces for errors\n"
    "  -x, --xtrace             Trace expressions before evaluation\n\n"
    "      --install            Install the libys shared library\n"
    "      --upgrade            Upgrade ys and libys\n"
    "      --install-m2         Install the ys.v0 jars into ~/.m2\n\n"
    "      --version            Print version and exit\n"
    "  -h, --help               Print this help and exit"))

(def data-formats #{"json" "yaml" "csv" "tsv" "edn"})
(def code-formats #{"bb" "clj" "star"})
(def stages
  #{"parse" "compose" "resolve" "build" "transform" "construct" "print"})

(def current-options (atom {}))

(defn env [name]
  (System/getenv name))

(defn die [message]
  (fmt.Fprintln os.Stderr (str "Error: " message))
  (os.Exit 1))

(defn need-value [option more inline]
  (if (seq inline)
    [inline more]
    (if (seq more)
      [(first more) (rest more)]
      (die (str option " requires a value")))))

(defn parse-short [text more opts]
  (loop [chars (seq text) more more opts opts]
    (if-not chars
      [more opts]
      (let [flag (first chars)
            inline (apply str (rest chars))]
        (case flag
          \c (recur (next chars) more (assoc opts :compile true))
          \l (recur (next chars) more (assoc opts :load true))
          \p (recur (next chars) more (assoc opts :print true))
          \s (recur (next chars) more (assoc opts :stream true))
          \J (recur (next chars) more (assoc opts :json true))
          \Y (recur (next chars) more (assoc opts :yaml true))
          \U (recur (next chars) more (assoc opts :unordered true))
          \C (recur (next chars) more (assoc opts :clojure true))
          \d (recur (next chars) more (assoc opts :debug true))
          \S (recur (next chars) more (assoc opts :stack-trace true))
          \x (recur (next chars) more (assoc opts :xtrace true))
          \h (recur (next chars) more (assoc opts :help true))
          (if (#{\e \f \o \T \m \D} flag)
            (let [[value more] (need-value (str "-" flag) more inline)
                  key ({\e :eval \f :file \o :output
                        \T :to \m :mode \D :debug-stage} flag)
                  opts (if (= key :eval)
                         (update opts key (fnil conj []) value)
                         (if (= key :debug-stage)
                           (update opts key (fnil conj []) value)
                           (assoc opts key value)))]
              [more opts])
            (die (str "unknown option: -" flag))))))))

(def long-options
  {"--compile" :compile "--load" :load "--print" :print
   "--stream" :stream "--json" :json "--yaml" :yaml
   "--unordered" :unordered "--clojure" :clojure
   "--stack-trace" :stack-trace "--xtrace" :xtrace
   "--help" :help "--version" :version "--install" :install
   "--upgrade" :upgrade "--install-m2" :install-m2})

(def long-values
  {"--eval" :eval "--file" :file "--output" :output
   "--to" :to "--mode" :mode "--debug-stage" :debug-stage})

(declare shebang-script?)

(defn parse-args [argv]
  (loop [args argv opts {} positional []]
    (if-not (seq args)
      (assoc opts :arguments positional)
      (let [arg (first args) more (rest args)]
        (cond
          (= arg "--")
          (assoc opts :arguments (into positional more) :double-dash true)

          (long-options arg)
          (recur more (assoc opts (long-options arg) true) positional)

          (long-values arg)
          (let [[value more] (need-value arg more nil)
                key (long-values arg)
                opts (if (#{:eval :debug-stage} key)
                       (update opts key (fnil conj []) value)
                       (assoc opts key value))]
            (recur more opts positional))

          (str/starts-with? arg "--")
          (let [[option value] (str/split arg #"=" 2)]
            (if (and value (long-values option))
              (let [key (long-values option)
                    opts (if (#{:eval :debug-stage} key)
                           (update opts key (fnil conj []) value)
                           (assoc opts key value))]
                (recur more opts positional))
              (die (str "unknown option: " arg))))

          (and (str/starts-with? arg "-") (not= arg "-"))
          (let [[more opts] (parse-short (subs arg 1) more opts)]
            (recur more opts positional))

          :else
          (if (and (empty? positional) (shebang-script? arg))
            (assoc opts :arguments (into [arg] more))
            (recur more opts (conj positional arg))))))))

(defn option-name [key]
  (str "--" (name key)))

(defn conflict [opts keys]
  (let [enabled (filter #(get opts %1) keys)]
    (when (> (count enabled) 1)
      (str "Options " (option-name (first enabled)) " and "
        (option-name (second enabled)) " are mutually exclusive."))))

(def all-options
  #{:clojure :compile :debug :debug-stage :eval :file :help :install :install-m2
    :json :load :mode :output :print :stack-trace :stream :to :unordered
    :upgrade :version :xtrace :yaml})

(def action-options
  #{:compile :help :install :install-m2 :load :upgrade :version})

(def format-options #{:json :to :yaml})

(defn conflicts-with [opts key keys]
  (when (get opts key)
    (when-let [other (first (filter #(get opts %1) keys))]
      (str "Options " (option-name key) " and " (option-name other)
        " are mutually exclusive."))))

(defn validate-options [opts]
  (let [opts (cond-> opts
               (empty? (:eval opts)) (dissoc :eval)
               (empty? (:debug-stage opts)) (dissoc :debug-stage))]
    (or
    (when (and (some opts [:install :upgrade :install-m2])
               (seq (:arguments opts)))
      "Installation commands do not accept file arguments.")
    (some #(conflicts-with opts % (disj all-options % :stack-trace))
      [:install :upgrade :install-m2])
    (when (and (code-formats (:to opts)) (:clojure opts))
      (str "Options --to=" (:to opts)
        " and --clojure are mutually exclusive."))
    (when (and (code-formats (:to opts)) (:load opts))
      (str "Options --to=" (:to opts)
        " and --load are mutually exclusive."))
    (conflict opts action-options)
    (conflict opts format-options)
    (conflicts-with opts :help (disj all-options :help))
    (conflicts-with opts :version (disj all-options :version))
    (conflicts-with opts :mode #{:help :install :install-m2 :upgrade :version})
    (conflicts-with opts :eval #{:help :install :install-m2 :upgrade :version})
    (conflicts-with opts :print
      #{:compile :help :install :install-m2 :load :upgrade :version})
    (conflicts-with opts :to #{:help :install :install-m2 :upgrade :version})
    (when (and (:to opts)
           (not ((into data-formats code-formats) (:to opts))))
      (str "--to must be one of:\n"
        "  json, yaml, csv, tsv, edn (for --load)\n"
        "  bb, clj, star (for --compile)"))
    (when (and (:mode opts) (not (seq (:eval opts))))
      "Option --mode requires --eval.")
    (when (and (:mode opts)
           (not (#{"c" "code" "d" "data" "b" "bare"}
                  (:mode opts))))
      "--mode must be one of: c, code, d, data, b, bare")
    (when-let [bad (first (remove #(or (= %1 "all") (stages %1))
                            (:debug-stage opts)))]
      (str "Invalid debug stage: " bad)))))

(defn apply-environment [opts]
  (cond-> opts
    (env "YS_FORMAT") (assoc :to (env "YS_FORMAT"))
    (env "YS_LOAD") (assoc :load true)
    (env "YS_OUTPUT") (assoc :output (env "YS_OUTPUT"))
    (env "YS_STREAM") (assoc :stream true)
    (env "YS_PRINT") (assoc :print true)
    (and (env "YS_PRINT_EVAL") (seq (:eval opts))) (assoc :print true)
    (env "YS_STACK_TRACE") (assoc :stack-trace true)
    (env "YS_UNORDERED") (assoc :unordered true)
    (env "YS_XTRACE") (assoc :xtrace true)))

(defn infer-output-format [opts]
  (let [output (:output opts)]
    (if (and output (not (:to opts))
          (re-find #"\.(?:yml|yaml|json|csv|tsv|edn)$" output))
      (let [format (str/replace output #".*\.(\w+)$" "$1")]
        (assoc opts :to (if (= format "yml") "yaml" format)))
      opts)))

(defn read-stdin []
  (let [[content error] (io.ReadAll os.Stdin)]
    (if (nil? error)
      (fmt.Sprintf "%s" content)
      (throw error))))

(defn file-exists? [path]
  (let [[_ error] (os.Stat path)]
    (nil? error)))

(defn shebang-script? [path]
  (and (file-exists? path)
    (try
      (str/starts-with? (slurp path) "#!")
      (catch go/any _ false))))

(defn looks-like-expression? [text]
  (and text
    (not= text "...")
    (or (str/starts-with? text ".")
        (str/starts-with? text ":")
        (str/includes? text "(")
        (str/includes? text ": ")
        (str/starts-with? text " ")
        (str/ends-with? text " "))))

(defn positional-expression? [opts]
  (let [argument (first (:arguments opts))]
    (and (not (seq (:eval opts)))
      (looks-like-expression? argument)
      (not (file-exists? argument)))))

(defn mode-tag [opts code]
  (if (or (:clojure opts)
          (re-find #"(?m)^(?:---\s+)?!(?:yamlscript/v0|YS-v0|ys-0)"
            code))
    code
    (let [mode (:mode opts)]
      (str (cond
             (#{"c" "code"} mode) "--- !ys-0\n"
             (#{"d" "data"} mode) "--- !ys-0:\n"
             (#{"b" "bare"} mode) "---\n"
             (:load opts) "---\n"
             :else "--- !ys-0\n")
        code))))

(defn expression-code [opts expressions]
  (when (seq expressions)
    (let [code (str/join "\n" expressions)
          stream-expression?
          (and (not (str/includes? code "\n"))
            (or (re-find #"^\.[\w\$]" code)
                (re-find #"^\:\w" code)))
          code (if stream-expression?
                 (cond
                   (re-find #"^\.[\w\$]" code)
                   (str (if (:stream opts)
                          "stream()"
                          "stream().last()") code)

                   (re-find #"^\:\w" code)
                   (str (if (:stream opts)
                          "stream()"
                          "stream().last()")
                     (str/replace code #"^:([-\w]+)" ".$1()"))

                   :else code)
                 code)
          opts (if stream-expression?
                 (assoc opts :mode "code" :load false)
                 opts)]
      (str (mode-tag opts code) "\n"))))

(defn input-info [opts]
  (let [positionals (:arguments opts)
        first-arg (first positionals)
        expr-arg? (and (not (seq (:eval opts)))
                    (looks-like-expression? first-arg)
                    (not (file-exists? first-arg)))
        expressions (if expr-arg? [first-arg] (:eval opts))
        positionals (if expr-arg? (rest positionals) positionals)
        file (or (:file opts)
               (when (and (seq expressions)
                       (not (:double-dash opts)))
                 (first positionals))
               (when-not (seq expressions) (first positionals))
               (when (and (:load opts)
                       (or expr-arg? (not (seq expressions)))) "-"))
        args (if file (rest positionals) positionals)
        file-code (when file
                    (str (if (= file "-")
                           (read-stdin)
                           (slurp file)) "\n"))
        expr-code (expression-code opts expressions)
        load? (or (:load opts) (and file-code expr-code))]
    {:code (str file-code expr-code)
     :file (or file "NO-NAME")
     :args (vec args)
     :load load?}))

(defn compile-code [code opts]
  (reset! global/opts
    {:compile (:compile opts)
     :debug-stage
     (if (:debug opts)
       (zipmap stages (repeat true))
       (zipmap (:debug-stage opts) (repeat true)))
     :xtrace (:xtrace opts)
     :unordered (:unordered opts)})
  (if (:clojure opts)
    code
    (if (or (:debug opts) (seq (:debug-stage opts)))
      (compiler/compile-with-options code)
      (compiler/compile code))))

(def v0-header
  "(ns main (:require ys.v0))\n(ys.v0/init)\n")

(def data-json-version "2.4.0")

(def v0-bb-header
  (str
    "(when (System/getProperty \"babashka.version\")\n"
    "  (let [m2 (str (System/getProperty \"user.home\")"
    " \"/.m2/repository/\")\n"
    "        jars [(str m2 \"org/yamlscript/ys.v0/"
    yamlscript-version "/ys.v0-" yamlscript-version ".jar\")\n"
    "              (str m2 \"org/clojure/data.json/"
    data-json-version "/data.json-" data-json-version ".jar\")]]\n"
    "    (if (every? #(.exists (java.io.File. %)) jars)\n"
    "      ((requiring-resolve 'babashka.classpath/add-classpath)\n"
    "       (clojure.string/join java.io.File/pathSeparator jars))\n"
    "      ((requiring-resolve 'babashka.deps/add-deps)\n"
    "       '{:deps {org.yamlscript/ys.v0 {:mvn/version \""
    yamlscript-version "\"}}}))))\n"))

(def v0-star-header
  (str
    "(when-not (find-ns 'ys.v0)\n"
    "  (require 'clojurestar.deps)\n"
    "  ((resolve 'clojurestar.deps/add-deps)\n"
    "   '{:deps {org.yamlscript/ys.v0 {:mvn/version \""
    yamlscript-version "\"}}}))\n\n"))

(def v0-clj-header
  (str
    "(when-not (or (System/getProperty \"babashka.version\")\n"
    "              (System/getProperty \"jolt.version\"))\n"
    "  (or (find-ns 'ys.v0)\n"
    "      (try (require 'ys.v0) true (catch Exception _ false))\n"
    "      (eval\n"
    "        '(let [t (Thread/currentThread)\n"
    "               cl (clojure.lang.DynamicClassLoader.\n"
    "                    (.getContextClassLoader t))]\n"
    "           (.setContextClassLoader t cl)\n"
    "           (with-bindings"
    " {(requiring-resolve 'clojure.core/*repl*) true}\n"
    "             ((requiring-resolve 'clojure.repl.deps/add-libs)\n"
    "              '{org.yamlscript/ys.v0 {:mvn/version \""
    yamlscript-version "\"}}))\n"
    "           (with-bindings {clojure.lang.Compiler/LOADER cl}\n"
    "             (require 'ys.v0)\n"
    "             (doseq [lib '[flatland.ordered.map clj-yaml.core\n"
    "                           clojure.data.json clojure.data.csv\n"
    "                           babashka.process babashka.http-client]]\n"
    "               (try (require lib) (catch Throwable _))))))))\n"))

(def code-headers
  {"bb" v0-bb-header "clj" v0-clj-header "star" v0-star-header})

(defn external-format [code formatter]
  (when (= runtime.GOOS "wasip1")
    (throw
      (ex-info "YS_FORMATTER is not available in the WASI build" {})))
  (let [{:keys [exit out err]} (process/sh {:in code} formatter)]
    (if (= exit 0)
      out
      (throw
        (ex-info
          (str "Compiler formatter error in '" formatter "':\n" err) {})))))

(defn pretty-code [code]
  (str/trimr
    (let [code (try
                 (compiler/pretty-format code)
                 (catch go/any _ code))]
      (if-let [formatter (env "YS_FORMATTER")]
        (external-format code formatter)
        code))))

(defn compile-output [opts code]
  (let [code (pretty-code code)
        target (:to opts)]
    (if-let [header (code-headers target)]
      (str (when (and (= target "bb") (:output opts))
             "#!/usr/bin/env bb\n")
        header v0-header "\n" code)
      code)))

(declare pretty-json)

(defn indent [level]
  (apply str (repeat (* level 2) " ")))

(defn pretty-json [value level]
  (cond
    (map? value)
    (if (empty? value)
      "{}"
      (let [rows (map (fn [[key item]]
                        (str (indent (inc level))
                          (json/dump (str key)) ": "
                          (pretty-json item (inc level))))
                   value)]
        (str "{\n" (str/join ",\n" rows) "\n" (indent level) "}")))
    (sequential? value)
    (if (empty? value)
      "[]"
      (let [rows (map #(str (indent (inc level))
                         (pretty-json %1 (inc level))) value)]
        (str "[\n" (str/join ",\n" rows) "\n" (indent level) "]")))
    :else (json/dump value)))

(defn format-result [value format pretty?]
  (case format
    "yaml" (str/trim-newline (yaml/dump value))
    "csv" (str/trim-newline (csv/write-csv value))
    "tsv" (str/trim-newline (csv/write-tsv value))
    "edn" (pprint/write value :stream nil)
    "json" (if pretty? (pretty-json value 0) (json/dump value))
    (json/dump value)))

(defn write-output [text opts executable?]
  (if-let [path (:output opts)]
    (let [mode (if executable? 0755 0644)
          error
          (github.com:yaml:yamlscript:internal:goyamlparser.WriteTextFile
            path (str text "\n") mode)]
      (when error (throw error)))
    (println text)))

(defn run-code [opts info code]
  (runtime/enter-main!)
  (runtime/set-runtime! (:file info) (:args info) (:original-argv opts))
  (try
    (let [result (runtime/eval-code code)
          results (if (and (:stream opts) (:load info))
                    @v0-global/stream-values
                    [result])]
      (cond
        (:print opts)
        (write-output (pprint/write result :stream nil) opts false)

        (:load info)
        (let [values (remove nil? results)
              count (count values)
              texts (map #(let [text (format-result %1
                                        (or (:to opts) "json")
                                        (:json opts))]
                            (if (and (= (:to opts) "yaml") (> count 1))
                              (str "---\n" text)
                              text))
                      values)]
          (when (seq texts)
            (write-output (str/join "\n" texts) opts false)))

        :else nil))
    (finally
      (runtime/unload-pods))))

(defn run-installer [command]
  (util/run-installer (util-platform/context) command yamlscript-version))

(defn main* [argv]
  (let [opts (-> (parse-args argv)
               (assoc :original-argv (vec argv))
               apply-environment
               infer-output-format)
        positional? (positional-expression? opts)
        opts (if positional?
               (assoc opts :load true)
               opts)
        opts (if (and (seq (:eval opts)) (not (:mode opts)))
               (assoc opts :mode "code") opts)
        _ (when-let [error (validate-options opts)] (die error))
        opts (cond-> opts
               positional? (assoc :to (or (:to opts) "yaml"))
               (:json opts) (assoc :load true :to "json")
               (:yaml opts) (assoc :load true :to "yaml")
               (data-formats (:to opts)) (assoc :load true)
               (code-formats (:to opts)) (assoc :compile true))
        _ (reset! current-options opts)]
    (when (env "YS_SHOW_OPTS")
      (println (yaml/dump {:opts opts :args (:arguments opts)})))
    (cond
      (:help opts) (println usage-text)
      (:version opts)
      (println (str "YS (YAMLScript) " yamlscript-version))
      (:install opts) (run-installer :install)
      (:upgrade opts) (run-installer :upgrade)
      (:install-m2 opts) (run-installer :install-m2)
      (and (empty? argv) (not (:load opts))) (println usage-text)
      :else
      (let [info (input-info opts)
            code (compile-code (:code info) opts)]
        (when (env "YS_SHOW_COMPILE")
          (binding [*out* *err*]
            (println (apply str (repeat 80 "-")))
            (println (pretty-code code))
            (println (apply str (repeat 80 "-")))))
        (if (:compile opts)
          (write-output
            (compile-output opts code) opts
            (and (= "bb" (:to opts)) (:output opts)))
          (run-code opts info code))))))

(defn error-message [error stack-trace?]
  (let [message (if (string? error)
                  error
                  (or (ex-message error) (str error)))
        message (if stack-trace?
                  message
                  (first (str/split message #"\n\nGLJ Stack:" 2)))]
    (if-let [[_ symbol]
             (or
               (re-find #"EvalASTMaybeHostForm: ([^\n]+)" message)
               (re-find #"unable to resolve symbol: ([^\n]+)" message))]
      (str "Could not resolve symbol: " symbol)
      message)))

(defn -main [& argv]
  (alter-var-root #'*in* (constantly os.Stdin))
  (alter-var-root #'*out* (constantly os.Stdout))
  (alter-var-root #'*err* (constantly os.Stderr))
  (try
    (main* argv)
    (catch go/any error
      (die (error-message error (:stack-trace @current-options))))))
