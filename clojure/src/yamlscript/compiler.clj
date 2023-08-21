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

nil
