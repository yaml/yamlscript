; Copyright 2023-2024 Ingy dot Net
; This code is licensed under MIT license (See License for details)

(ns yamlscript.compiler-test
  #_(:use yamlscript.debug)
  (:require
   [yamlscript.compiler :as compiler]
   [yamltest.core :as test]))

(test/load-yaml-test-files
  ["test/compiler.yaml"
   "test/compiler-stack.yaml"]
  {:pick-func #(test/has-keys? [:yamlscript :clojure] %1)
   :test-func (fn [test]
                (->> test
                  :yamlscript
                  compiler/compile))
   :want-func (fn [test]
                (:clojure test))})
