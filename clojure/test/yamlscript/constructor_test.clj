; Copyright 2023 Ingy dot Net
; This code is licensed under MIT license (See License for details)

(ns yamlscript.constructor-test
  (:use yamlscript.debug)
  (:require
   [clojure.edn :as edn]
   [yamlscript.parser :as parser]
   [yamlscript.composer :as composer]
   [yamlscript.resolver :as resolver]
   [yamlscript.builder :as builder]
   [yamlscript.expander :as expander]
   [yamlscript.constructor :as constructor]
   [yamlscript.test :as test]))

(do
  (test/remove-tests)
  (test/load-yaml-tests
    {:yaml-file "test/data.yaml"
     :pick-func #(test/has-keys? [:yamlscript :construct] %)
     :test-func (fn [test]
                  (->> test
                    :yamlscript
                    parser/parse
                    composer/compose
                    resolver/resolve
                    builder/build
                    expander/expand
                    constructor/construct))
     :want-func (fn [test]
                  (->> test
                    :construct
                    edn/read-string))}))
