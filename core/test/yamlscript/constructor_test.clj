;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.constructor-test
  (:require
   [clojure.edn :as edn]
   [yamlscript.builder :as builder]
   [yamlscript.common]
   [yamlscript.composer :as composer]
   [yamlscript.constructor :as constructor]
   [yamlscript.parser :as parser]
   [yamlscript.resolver :as resolver]
   [yamlscript.transformer :as transformer]
   [yamltest.core :as test]))

(test/load-yaml-test-files
  ["test/compiler-stack.yaml"
   "test/data-mode.yaml"
   "test/compiler.yaml"]
  {:pick #(test/has-keys? [:yamlscript :construct] %1)
   :test (fn [test]
           (-> test
             :yamlscript
             parser/parse
             composer/compose
             first
             resolver/resolve
             builder/build
             transformer/transform
             constructor/construct-ast))
   :want (fn [test]
           (-> test
             :construct
             edn/read-string))})
