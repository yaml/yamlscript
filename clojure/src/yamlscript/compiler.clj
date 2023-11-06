;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.compiler
  (:use yamlscript.debug)
  (:require
   [clojure.edn]
   [yamlscript.parser]
   [yamlscript.composer]
   [yamlscript.resolver]
   [yamlscript.builder]
   [yamlscript.expander]
   [yamlscript.constructor]
   [yamlscript.printer])
  (:refer-clojure :exclude [compile]))

(defn compile
  "Convert YAMLScript code string to an equivalent Clojure code string."
  [^String yamlscript-string]
  (let [^String clojure-string
        (->> yamlscript-string
          yamlscript.parser/parse
          yamlscript.composer/compose
          yamlscript.resolver/resolve
          yamlscript.builder/build
          yamlscript.expander/expand
          yamlscript.constructor/construct
          yamlscript.printer/print)]
    clojure-string))

(comment
  (->>
   ""
   #_"!yamlscript/v0"

   yamlscript.parser/parse
   yamlscript.composer/compose
   yamlscript.resolver/resolve
   yamlscript.builder/build
   yamlscript.expander/expand
   yamlscript.constructor/construct
   yamlscript.printer/print

   www)

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
