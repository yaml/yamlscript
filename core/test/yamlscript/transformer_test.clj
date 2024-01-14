; Copyright 2023-2024 Ingy dot Net
; This code is licensed under MIT license (See License for details)

(ns yamlscript.transformer-test
  #_(:use yamlscript.debug)
  (:require
   [yamlscript.parser :as parser]
   [yamlscript.composer :as composer]
   [yamlscript.resolver :as resolver]
   [yamlscript.builder :as builder]
   [yamlscript.transformer :as transformer]
   [yamltest.core :as test]
   [clojure.edn :as edn]))

(test/load-yaml-test-files
  ["test/compiler-stack.yaml"
   "test/compiler.yaml"]
  {:pick #(test/has-keys? [:yamlscript :transform] %1)
   :test (fn [test]
           (->> test
             :yamlscript
             parser/parse
             composer/compose
             resolver/resolve
             builder/build
             transformer/transform))
   :want (fn [test]
           (->> test
             :transform
             edn/read-string))})
