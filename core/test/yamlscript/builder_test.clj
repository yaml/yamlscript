; Copyright 2023-2025 Ingy dot Net
; This code is licensed under MIT license (See License for details)

(ns yamlscript.builder-test
  (:require
   [clojure.edn :as edn]
   [yamlscript.builder :as builder]
   [yamlscript.common]
   [yamlscript.composer :as composer]
   [yamlscript.parser :as parser]
   [yamlscript.resolver :as resolver]
   [yamltest.core :as test]))

(test/load-yaml-test-files
  ["test/compiler-stack.yaml"
   "test/data-mode.yaml"
   "test/compiler.yaml"
   "test/transformer.yaml"]
  {:pick #(test/has-keys? [:yamlscript :build] %1)
   :test (fn [test]
           (-> test
             :yamlscript
             parser/parse
             composer/compose
             resolver/resolve
             builder/build))
   :want (fn [test]
           (-> test
             :build
             edn/read-string))})
