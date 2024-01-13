; Copyright 2023-2024 Ingy dot Net
; This code is licensed under MIT license (See License for details)

(ns yamlscript.parser-test
  #_(:use yamlscript.debug)
  (:require
   [clojure.string :as str]
   [yamlscript.parser :as parser]
   [yamltest.core :as test]))

(test/load-yaml-test-files
  ["test/compiler-stack.yaml"
   "test/resolver.yaml"
   "test/data-mode.yaml"]
  {:pick-func #(test/has-keys? [:yamlscript :parse] %1)
   :test-func (fn [test]
                (->> test
                  :yamlscript
                  parser/parse
                  (map pr-str)
                  (map #(subs %1 4 (dec (count %1))))))
   :want-func (fn [test]
                (->> test
                  :parse
                  str/split-lines))})
