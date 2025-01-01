; Copyright 2023-2025 Ingy dot Net
; This code is licensed under MIT license (See License for details)

(ns yamlscript.composer-test
  (:require
   [clojure.edn :as edn]
   [yamlscript.common]
   [yamlscript.composer :as composer]
   [yamlscript.parser :as parser]
   [yamltest.core :as test]))

(test/load-yaml-test-files
  ["test/compiler-stack.yaml"
   "test/resolver.yaml"
   "test/compiler.yaml"]
  {:pick #(test/has-keys? [:yamlscript :compose] %1)
   :test (fn [test]
           (-> test
             :yamlscript
             parser/parse
             composer/compose))
   :want (fn [test]
           (-> test
             :compose
             edn/read-string))})
