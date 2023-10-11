; Copyright 2023 Ingy dot Net
; This code is licensed under MIT license (See License for details)

(ns yamlscript.composer-test
  (:use yamlscript.debug)
  (:require
   [clojure.edn :as edn]
   [yamlscript.parser :as parser]
   [yamlscript.composer :as composer]
   [yamlscript.test :as test]))

(do
  (test/remove-tests)
  (test/load-yaml-tests
      {:yaml-file "test/data.yaml"
       :pick-func #(test/has-keys? [:yamlscript :compose] %)
       :test-func (fn [test]
                    (->> test
                      :yamlscript
                      parser/parse
                      composer/compose))
       :want-func (fn [test]
                    (->> test
                      :compose
                      edn/read-string))}))
