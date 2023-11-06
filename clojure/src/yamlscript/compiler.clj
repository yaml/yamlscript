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
   [yamlscript.printer]
   [clojure.pprint :as pp])
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
   #_""
   #_"!yamlscript/v0"
   "!yamlscript/v0
a =: 123"

   yamlscript.parser/parse
   (www :parse)
   yamlscript.composer/compose
   (www :compose)
   yamlscript.resolver/resolve
   (www :resolve)
   yamlscript.builder/build
   (www :build)
   yamlscript.expander/expand
   (www :expand)
   yamlscript.constructor/construct
   (www :construct)
   yamlscript.printer/print
   (www :print)
   #__)

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
