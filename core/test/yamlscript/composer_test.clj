; Copyright 2023-2024 Ingy dot Net
; This code is licensed under MIT license (See License for details)

(ns yamlscript.composer-test
  (:use yamlscript.debug)
  (:require
   [clojure.edn :as edn]
   [yamlscript.parser :as parser]
   [yamlscript.composer :as composer]
   [yamltest.core :as test]))

(test/load-yaml-test-files
  ["test/compiler-stack.yaml"
   "test/resolver.yaml"
   "test/compiler.yaml"]
  {:pick-func #(test/has-keys? [:yamlscript :compose] %)
   :test-func (fn [test]
                (->> test
                  :yamlscript
                  parser/parse
                  composer/compose))
   :want-func (fn [test]
                (->> test
                  :compose
                  edn/read-string))})
