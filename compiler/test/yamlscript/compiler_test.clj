; Copyright 2023 Ingy dot Net
; This code is licensed under MIT license (See License for details)

(ns yamlscript.compiler-test
  (:use yamlscript.debug)
  (:require
   [yamlscript.compiler :as compiler]
   [yamlscript.test :as test]))

(def head "(ns main (:use ys.core))\n")

(do
  (test/remove-tests)
  (test/load-yaml-tests
    {:yaml-file "test/compiler.yaml"
     :pick-func #(test/has-keys? [:yamlscript :clojure] %)
     :test-func (fn [test]
                  (->> test
                    :yamlscript
                    yamlscript.compiler/compile))
     :want-func (fn [test]
                  (str head (:clojure test)))}))
