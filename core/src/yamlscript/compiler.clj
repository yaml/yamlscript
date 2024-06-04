;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; YAMLScript is a programming language that is hosted by Clojure platforms.
;; It can be used to add scripting to abilities to YAML files.

;; The yamlscript.compiler is responsible for converting YAMLScript code to
;; Clojure code. It does this by sending the input through a stack of 7
;; transformation libraries.

(ns yamlscript.compiler
  (:require
   [a0.patch-pprint]
   [clojure.pprint]
   [clojure.edn]
   [clojure.string :as str]
   [compiler.parser :as ferret-parser]
   [compiler.core :as ferret]
   [yamlscript.parser]
   [yamlscript.composer]
   [yamlscript.resolver]
   [yamlscript.builder]
   [yamlscript.transformer]
   [yamlscript.constructor]
   [yamlscript.printer]
   [yamlscript.common :as common]
   [yamlscript.debug :refer [www #_xxx]])
  (:refer-clojure :exclude [compile]))

(defn ferret-read-clojure-string [s]
  (let [ns (gensym)
        ns-str (str ns)]
    (create-ns ns)
    (binding [*ns* (the-ns ns)]
      (refer 'clojure.core)
      (let [code-edn (read-string (str \( s \)))]
        (-> code-edn
          (ferret-parser/transform
           symbol?
           #(if (= (namespace %) ns-str)
              (-> % name symbol)
              %))
          (ferret-parser/transform
           (fn [x]
             (and (ferret-parser/form? 'quote x)
                  (or (= 'clojure.core/fn    (second x))
                      (= 'clojure.core/defn  (second x))
                      (= 'clojure.core/while (second x)))))
           (fn [[_ s]] `'~(-> s name symbol))))))))

(defn ferret-compile-clj [clj-string]
    (let [args {:options {:input "./core.clj"}, :arguments []}
          options ((ferret/build-specs (-> args :options :input) args))
          code-edn (ferret-read-clojure-string clj-string)
          source    (ferret/emit-source code-edn options)
          program   (ferret/program-template source options)]
      program))

(defn parse-events-to-groups [events]
  (->> events
    (reduce
      (fn [acc ev]
        (if (= (:+ ev) "+DOC")
          (conj acc [ev])
          (update acc (dec (count acc)) conj ev)))
      [[]])
    (map #(remove (fn [ev] (= "DOC" (subs (:+ ev) 1))) %1))))

(defn compile
  "Convert YAMLScript code string to an equivalent Clojure code string."
  [^String yamlscript-string]
  (let [events (yamlscript.parser/parse yamlscript-string)
        groups (parse-events-to-groups events)
        n (count groups)
        blocks (loop [[group & groups] groups blocks [] i 1]
                 (let [blocks (conj blocks
                                (-> group
                                  yamlscript.composer/compose
                                  yamlscript.resolver/resolve
                                  yamlscript.builder/build
                                  yamlscript.transformer/transform
                                  (yamlscript.constructor/construct (>= i n))
                                  yamlscript.printer/print))]
                   (if (seq groups)
                     (recur groups blocks (inc i))
                     blocks)))]
    (str/join "" blocks)))

(defn stage-with-options [stage-name stage-fn input-args]
  (if (get-in @common/opts [:debug-stage stage-name])
    (printf "*** %-9s *** " stage-name)
    (when (:time @common/opts)
      (printf "*** %-9s *** " stage-name)))
  (let [output-data (if (:time @common/opts)
                      (time (apply stage-fn input-args))
                      (apply stage-fn input-args))]
    (when (not (:time @common/opts)) (println ""))
    (when (get-in @common/opts [:debug-stage stage-name])
      (clojure.pprint/pprint output-data)
      (println ""))
    output-data))

(defn compile-with-options
  "Convert YAMLScript code string to an equivalent Clojure code string."
  [^String yamlscript-string]
  (let [events (stage-with-options "parse"
                 yamlscript.parser/parse [yamlscript-string])
        groups (parse-events-to-groups events)
        n (count groups)
        blocks (loop [[group & groups] groups blocks [] i 1]
                 (let [blocks (conj blocks
                                (-> group
                                  (#(stage-with-options "compose"
                                      yamlscript.composer/compose [%1]))
                                  (#(stage-with-options "resolve"
                                      yamlscript.resolver/resolve [%1]))
                                  (#(stage-with-options "build"
                                      yamlscript.builder/build [%1]))
                                  (#(stage-with-options "transform"
                                      yamlscript.transformer/transform [%1]))
                                  (#(stage-with-options "construct"
                                      yamlscript.constructor/construct
                                      [%1 (>= i n)]))
                                  (#(stage-with-options "print"
                                      yamlscript.printer/print [%1]))))]
                   (if (seq groups)
                     (recur groups blocks (inc i))
                     blocks)))]
    (str/join "" blocks)))

(defn pretty-format [code]
  (->> code
    (#(str "(do " %1 "\n)\n"))
    read-string
    rest
    (map #(str
            (with-out-str (clojure.pprint/write %1))
            "\n"))
    (apply str)))

(comment
  www
  )
