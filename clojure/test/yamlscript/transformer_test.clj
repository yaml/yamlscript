; Copyright 2023 Ingy dot Net
; This code is licensed under MIT license (See License for details)

(ns yamlscript.transformer-test
  (:use yamlscript.debug)
  (:require
   [yamlscript.parser :as parser]
   [yamlscript.composer :as composer]
   [yamlscript.resolver :as resolver]
   [yamlscript.builder :as builder]
   [yamlscript.transformer :as transformer]
   [yamlscript.test :as test]
   [clojure.edn :as edn]))

(def test-files
  ["test/compiler-stack.yaml"])

(test/remove-tests)

(doseq [test-file test-files]
  (test/load-yaml-tests
    {:yaml-file test-file
     :pick-func #(test/has-keys? [:yamlscript :transform] %)
     :test-func (fn [test]
                  (->> test
                    :yamlscript
                    parser/parse
                    composer/compose
                    resolver/resolve
                    builder/build
                    transformer/transform))
     :want-func (fn [test]
                  (->> test
                    :transform
                    edn/read-string))}))
