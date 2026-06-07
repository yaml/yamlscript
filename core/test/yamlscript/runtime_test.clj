;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.runtime-test
  (:require
   [clojure.edn :as edn]
   [yamlscript.compiler :as compiler]
   [yamlscript.runtime :as runtime]
   [yamltest.core :as test]))

(test/load-yaml-test-files
  ["test/runtime.yaml"]
  {:pick #(test/has-keys? [:ys :eval] %1)
   :test (fn [test]
           (-> test
             :ys
             (->> (str "!ys-0\n"))
             compiler/compile
             runtime/eval-string))
   :want (fn [test]
           (-> test
             :eval
             edn/read-string))})
