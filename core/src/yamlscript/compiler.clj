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

(defn debug-print [stage data]
  (when (get-in @common/opts [:debug-stage stage])
    (println (str "*** " stage " output ***"))
    (clojure.pprint/pprint data)
    (println ""))
  data)

(defn compile-with-options
  "Convert YAMLScript code string to an equivalent Clojure code string."
  [^String yamlscript-string]
  (let [events (yamlscript.parser/parse yamlscript-string)
        _ (debug-print "parse" events)
        groups (parse-events-to-groups events)
        n (count groups)
        blocks (loop [[group & groups] groups blocks [] i 1]
                 (let [blocks (conj blocks
                                (->> group
                                  yamlscript.composer/compose
                                  (debug-print "compose")
                                  yamlscript.resolver/resolve
                                  (debug-print "resolve")
                                  yamlscript.builder/build
                                  (debug-print "build")
                                  yamlscript.transformer/transform
                                  (debug-print "transform")
                                  (#(yamlscript.constructor/construct
                                      %1 (>= i n)))
                                  (debug-print "construct")
                                  yamlscript.printer/print
                                  (debug-print "print")))]
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
