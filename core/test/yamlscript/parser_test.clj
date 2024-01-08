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
  {:pick-func #(test/has-keys? [:yamlscript :parse] %)
   :test-func (fn [test]
                (->> test
                  :yamlscript
                  parser/parse
                  (map pr-str)
                  (map #(subs % 4 (dec (count %))))))
   :want-func (fn [test]
                (->> test
                  :parse
                  str/split-lines))})
