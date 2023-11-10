;; This code is licensed under MIT license (See License for details)
;; Copyright 2023 Ingy dot Net

;; YAMLScript is a programming language that is hosted by Clojure platforms.
;; It can be used to add scripting to abilities to YAML files.
;;
;; This is the main API library for YAMLScript. It provides the following:
;;
;; * `compile` - Converts YAMLScript code to Clojure code
;; * `load-file` - Reads and evaluates a YAMLScript file

(ns yamlscript.core
  (:require
   [a0.patch-pprint]
   [clojure.edn :as edn]
   [yamlscript.debug :refer [www]]
   [yamlscript.compiler :as compiler])
  (:refer-clojure :exclude [compile load-file]))

(defn compile
  "Compile a YAMLScript code string to an equivalent Clojure code string."
  [ys-string]
  (->> ys-string
    compiler/compile))

(defn load-file
  "YAMLScript equivalent of clojure.core/load-file."
  [ys-file]
  (->> ys-file
    slurp
    compile
    edn/read-string
    eval))

(comment
  (do
    (require '[yamlscript.core :as ys])
    (ys/load-file "test/hello.ys"))

  (->> "foo: bar baz"
    compile
    println)

  (->> "test/hello.ys"
    load-file)
  )
