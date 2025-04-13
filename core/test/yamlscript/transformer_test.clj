;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.transformer-test
  (:require
   [yamlscript.builder :as builder]
   [yamlscript.common]
   [yamlscript.composer :as composer]
   [yamlscript.parser :as parser]
   [yamlscript.resolver :as resolver]
   [yamlscript.transformer :as transformer]
   [yamltest.core :as test]
   [clojure.edn :as edn]))

(test/load-yaml-test-files
  ["test/compiler-stack.yaml"
   "test/compiler.yaml"
   "test/transformer.yaml"]
  {:pick #(test/has-keys? [:yamlscript :transform] %1)
   :test (fn [test]
           (-> test
             :yamlscript
             parser/parse
             composer/compose
             first
             resolver/resolve
             builder/build
             transformer/transform))
   :want (fn [test]
           (-> test
             :transform
             edn/read-string))})

(test/load-yaml-test-files
  ["test/transformer.yaml"]
  {:add-tests true
   :pick #(test/has-keys? [:yamlscript :error] %1)
   :test (fn [test]
           (try
             (-> test
               :yamlscript
               parser/parse
               composer/compose
               first
               resolver/resolve
               builder/build
               transformer/transform)
             ""
             (catch Exception e
               (:cause (Throwable->map e)))))
   :want :error})
