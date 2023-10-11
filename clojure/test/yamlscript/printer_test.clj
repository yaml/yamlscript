; Copyright 2023 Ingy dot Net
; This code is licensed under MIT license (See License for details)

(ns yamlscript.printer-test
  (:use yamlscript.debug)
  (:require
   [yamlscript.parser :as parser]
   [yamlscript.composer :as composer]
   [yamlscript.resolver :as resolver]
   [yamlscript.builder :as builder]
   [yamlscript.expander :as expander]
   [yamlscript.constructor :as constructor]
   [yamlscript.printer :as printer]
   [yamlscript.test :as test]))

(do
  (test/remove-tests)
  (test/load-yaml-tests
    {:yaml-file "test/data.yaml"
     :pick-func #(test/has-keys? [:yamlscript :clojure] %)
     :test-func (fn [test]
                  (->> test
                    :yamlscript
                    parser/parse
                    composer/compose
                    resolver/resolve
                    builder/build
                    expander/expand
                    constructor/construct
                    printer/print))
     :want-func (fn [test]
                  (->> test
                    :clojure))}))
