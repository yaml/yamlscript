; Copyright 2023 Ingy dot Net
; This code is licensed under MIT license (See License for details)

(ns yamlscript.resolver-test
  (:use yamlscript.debug)
  (:require
   [clojure.edn :as edn]
   [yamlscript.parser :as parser]
   [yamlscript.composer :as composer]
   [yamlscript.resolver :as resolver]
   [yamlscript.test :as test]))

(def test-files
  ["test/compiler-stack.yaml"
   "test/yaml-mode.yaml"
   "test/resolver.yaml"])

(test/remove-tests)

(doseq [test-file test-files]
  (test/load-yaml-tests
    {:yaml-file test-file
     :pick-func #(test/has-keys? [:yamlscript :resolve] %)
     :test-func (fn [test]
                  (->> test
                    :yamlscript
                    parser/parse
                    composer/compose
                    resolver/resolve))
     :want-func (fn [test]
                  (->> test
                    :resolve
                    edn/read-string))}))
