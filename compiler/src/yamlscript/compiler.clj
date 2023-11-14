;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; YAMLScript is a programming language that is hosted by Clojure platforms.
;; It can be used to add scripting to abilities to YAML files.

;; The yamlscript.compiler is responsible for converting YAMLScript code to
;; Clojure code. It does this by sending the input through a stack of 7
;; transformation libraries.

(ns yamlscript.compiler
  (:use yamlscript.debug)
  (:require
   [a0.patch-pprint]
   [clojure.pprint]
   [clojure.edn]
   [yamlscript.parser]
   [yamlscript.composer]
   [yamlscript.resolver]
   [yamlscript.builder]
   [yamlscript.transformer]
   [yamlscript.constructor]
   [yamlscript.printer]
   [yamlscript.debug :refer :all])
  (:refer-clojure :exclude [compile]))

(def ^:dynamic *debug* {})

(def stages
  {"parse" true
   "compose" true
   "resolve" true
   "build" true
   "transform" true
   "construct" true
   "print" true
   "final" true})

(defn final [code]
  (str
    "(ns main (:use ys.core))\n"
    code))

(defn compile
  "Convert YAMLScript code string to an equivalent Clojure code string."
  [^String yamlscript-string]
  (let [^String clojure-string
        (->> yamlscript-string
          yamlscript.parser/parse
          yamlscript.composer/compose
          yamlscript.resolver/resolve
          yamlscript.builder/build
          yamlscript.transformer/transform
          yamlscript.constructor/construct
          yamlscript.printer/print
          final)]
    clojure-string))

(defn debug-print [stage data]
  (when (get *debug* stage)
    (println (str "*** " stage " output ***"))
    (clojure.pprint/pprint data)
    (println ""))
  data)

(defn compile-debug
  "Convert YAMLScript code string to an equivalent Clojure code string."
  [^String yamlscript-string]
  (let [^String clojure-string
        (->> yamlscript-string
          yamlscript.parser/parse
          (debug-print "parse")
          yamlscript.composer/compose
          (debug-print "compose")
          yamlscript.resolver/resolve
          (debug-print "resolve")
          yamlscript.builder/build
          (debug-print "build")
          yamlscript.transformer/transform
          (debug-print "transform")
          yamlscript.constructor/construct
          (debug-print "construct")
          yamlscript.printer/print
          (debug-print "print")
          final
          (debug-print "final"))]
    clojure-string))

(comment
; {:do [[{:Sym a} {:Sym b}]]}
; (:construct {:Lst [{:Sym a} {:Sym b}]})
  (binding [*debug* stages]
    (->>
      "!yamlscript/v0
foo =: 123
defn bar(a b):
  c =: (a + b)
  .*: 2 c
bar: 10 20"
      compile-debug
      print))

  (-> "" compile)
  (-> "!yamlscript/v0" compile)
  (-> "a: b" compile)

  (-> [#__
       "foo: bar baz"
       "if (x > y): x (inc y)"
       "if(x > y): x (inc y)"
       #__]
    (nth 2)
    compile
    println)

  (->> "test/hello.ys"
    slurp
    compile
    println)
  )
