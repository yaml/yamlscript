; Copyright 2023 Ingy dot Net
; This code is licensed under MIT license (See License for details)

(ns yamlscript.printer-test
  (:use yamlscript.debug)
  (:require
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
   "test/yaml-mode.yaml"]
  {:pick-func #(test/has-keys? [:yamlscript :print] %)
   :test-func (fn [test]
                (->> test
                  :yamlscript
                  parser/parse
                  composer/compose
                  resolver/resolve
                  builder/build
                  transformer/transform
                  constructor/construct
                  printer/print))
   :want-func (fn [test]
                (->> test
                  :print))})
