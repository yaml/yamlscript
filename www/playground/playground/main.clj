(ns playground.bridge
  (:require
   [clojure.string :as str]
   [yamlscript.compiler :as compiler]
   [yamlscript.global :as global]
   [yamlscript.glojure-runtime :as runtime]
   [ys.v0.json :as json]
   [ys.v0.pprint :as pprint]
   [ys.v0.yaml :as yaml]))

(def EXPORT
  {"compile-source" ["str" "str"]
   "run" ["str" "str"]})

(defonce base-namespaces (atom nil))

(defn main []
  nil)

(defn- playground-say [& values]
  (apply println values))

(defn- playground-warn [& values]
  (binding [*out* *err*]
    (apply println values)
    (flush)))

(defn- playground-err [& values]
  (binding [*out* *err*]
    (apply print values)
    (flush)))

(defn- ignore-host-effect [& _]
  nil)

(defn- unsafe-control? [character]
  (let [code (int character)]
    (and (< code 32)
      (not (contains? #{9 10 13} code)))))

(defn- top-level-definition? [line]
  (or (str/starts-with? line "(def ")
    (str/starts-with? line "(defn ")))

(defn- clean-compiled-format [formatted]
  (let [formatted (-> formatted
                    (str/replace
                      #"(?m)^\((defn|def)\n ([^\n]+)"
                      "($1 $2")
                    (str/replace
                      #"(?m)^\(defn ([^\n]+)\n (\[[^\n]+\])"
                      "(defn $1 $2"))
        lines (str/split-lines formatted)
        lines (reduce
                (fn [result line]
                  (if (and (top-level-definition? line)
                        (seq result)
                        (not= "" (peek result)))
                    (conj result "" line)
                    (conj result line)))
                [] lines)]
    (str (str/join "\n" lines) "\n")))

(defn- format-compiled [compiled]
  (let [formatted (compiler/pretty-format compiled)
        formatted (if (some unsafe-control? formatted)
                    compiled
                    formatted)]
    (clean-compiled-format formatted)))

(defn compile-source [source]
  (format-compiled (compiler/compile source)))

(defn- format-value [value format]
  (if (nil? value)
    ""
    (case format
      "yaml" (yaml/dump value)
      "json" (json/dump value)
      "edn" (pprint/write value :stream nil)
      "text" (if (sequential? value)
               (str/join "\n" value)
               (str value))
      (throw
        (ex-info (str "Unknown playground format: " format) {})))))

(defn- clear-runtime-ns! [ignore-terminal-effects]
  (if-let [base @base-namespaces]
    (doseq [namespace (all-ns)
            :when (not (base (ns-name namespace)))]
      (remove-ns (ns-name namespace)))
    (reset! base-namespaces (set (map ns-name (all-ns)))))
  (let [target (create-ns 'playground.user)]
    (reset! runtime/enabled-modules #{})
    (binding [*ns* target]
      (refer 'clojure.core)
      (runtime/install! target)
      (runtime/install-hooks!)
      (doseq [[name function]
              {'say playground-say
               'warn playground-warn
               'err playground-err}]
        (ns-unmap target name)
        (intern target name function))
      (when ignore-terminal-effects
        (ns-unmap target 'shell)
        (ns-unmap target 'sleep)
        (intern target 'shell ignore-host-effect)
        (intern target 'sleep ignore-host-effect)))
    target))

(def environment-name #"^[A-Za-z_][A-Za-z0-9_]*$")

(defn- invalid-environment [line-number]
  (throw
    (ex-info
      (str "Invalid Environment entry on line " line-number)
      {})))

(defn- parse-environment [source]
  (reduce
    (fn [environment [index original]]
      (let [line (str/trim original)]
        (if (or (str/blank? line)
              (str/starts-with? line "#"))
          environment
          (let [[name value] (str/split line #"=" 2)]
            (when-not (and value
                        (re-matches environment-name name))
              (invalid-environment (inc index)))
            (assoc environment name value)))))
    {}
    (map-indexed vector (str/split-lines (or source "")))))

(defn run [request-json]
  (let [request (json/load request-json)
        _modules (reset! runtime/allowed-local-modules
                   (set (map symbol (get request "projectModules"))))
        target (clear-runtime-ns!
                 (get request "ignoreTerminalEffects"))
        mode (get request "mode")
        args (get request "args")
        _runtime (runtime/set-runtime! (get request "file") args args)
        _environment-reset (global/update-environ {})
        _environment (global/update-environ
                       (parse-environment
                         (get request "environment")))
        _underscore (global/set-underscore nil)
        source (case mode
                 "query" (do
                           (global/set-underscore
                             (yaml/load (get request "input")))
                           (str "!ys-0\n"
                             (get request "source") "\n"))
                 "load" (get request "source")
                 "run" (get request "source")
                 (throw
                   (ex-info
                     (str "Unknown playground mode: "
                       mode) {})))
        compiled (compiler/compile source)
        value (binding [*ns* target
                        *out* os.Stdout
                        *err* os.Stderr]
                (runtime/eval-code compiled))
        format (get request "format")
        formats (get request "formats")
        outputs (when (seq formats)
                  (reduce
                    (fn [result output-format]
                      (assoc result output-format
                        (format-value value output-format)))
                    {} formats))
        output (if outputs
                 (get outputs format)
                 (format-value value format))]
    (json/dump
      (cond->
        {"compiled" (format-compiled compiled)
         "output" output}
        outputs (assoc "outputs" outputs)))))
