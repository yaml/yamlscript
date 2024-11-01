;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; YAMLScript is a programming language that is hosted by Clojure platforms.
;; It can be used to add scripting to abilities to YAML files.

;; The yamlscript.compiler is responsible for converting YAMLScript code to
;; Clojure code. It does this by sending the input through a stack of 7
;; transformation libraries.

(ns yamlscript.compiler
  (:require
   [clojure.pprint]
   [clojure.edn]
   [clojure.string :as str]
   [yamlscript.builder]
   [yamlscript.common]
   [yamlscript.composer]
   [yamlscript.constructor]
   [yamlscript.global]
   [yamlscript.parser]
   [yamlscript.printer]
   [yamlscript.resolver]
   [yamlscript.transformer])
  (:refer-clojure :exclude [compile]))

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

(defmacro value-time [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*out* s#]
       [(time ~@body)
        (.replaceAll (str s#) "[^0-9\\.]" "")])))

(defn stage-with-options [stage-name stage-fn input-args]
  (if (get-in @yamlscript.global/opts [:debug-stage stage-name])
    (let [[value time] (value-time (apply stage-fn input-args))]
      (printf "*** %-9s *** %s ms\n\n" stage-name time)
      (clojure.pprint/pprint value)
      (println)
      value)
    (apply stage-fn input-args)))

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
  )
