;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; YAMLScript is a programming language that is hosted by Clojure platforms.
;; It can be used to add scripting to abilities to YAML files.
;;
;; This library reads YAMLScript code and converts it into Clojure code."

(ns yamlscript.core
  (:use yamlscript.debug)
  (:require
   [clojure.edn :as edn]
   [yamlscript.compiler :as compiler])
  (:refer-clojure :exclude [compile load-file read-string]))

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
