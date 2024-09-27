; Copyright 2023-2024 Ingy dot Net
; This code is licensed under MIT license (See License for details)

(ns yamlscript.printer-test
  (:require
   [yamlscript.builder :as builder]
   [yamlscript.common]
   [yamlscript.compiler :as compiler]
   [yamlscript.composer :as composer]
   [yamlscript.constructor :as constructor]
   [yamlscript.parser :as parser]
   [yamlscript.printer :as printer]
   [yamlscript.resolver :as resolver]
   [yamlscript.transformer :as transformer]
   [yamltest.core :as test]))

(test/load-yaml-test-files
  ["test/compiler-stack.yaml"
   "test/data-mode.yaml"
   "test/compiler.yaml"]
  {:pick #(test/has-keys? [:yamlscript :print] %1)
   :test (fn [test]
           (-> test
             :yamlscript
             parser/parse
             composer/compose
             resolver/resolve
             builder/build
             transformer/transform
             constructor/construct-ast
             printer/print
             compiler/pretty-format))
   :want :print})
