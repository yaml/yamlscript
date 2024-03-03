; Copyright 2023-2024 Ingy dot Net
; This code is licensed under MIT license (See License for details)

(ns yamlscript.printer-test
  #_(:use yamlscript.debug)
  (:require
   [yamlscript.compiler :as compiler]
   [yamlscript.parser :as parser]
   [yamlscript.composer :as composer]
   [yamlscript.resolver :as resolver]
   [yamlscript.builder :as builder]
   [yamlscript.transformer :as transformer]
   [yamlscript.constructor :as constructor]
   [yamlscript.printer :as printer]
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
