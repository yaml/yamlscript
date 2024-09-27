; Copyright 2023-2024 Ingy dot Net
; This code is licensed under MIT license (See License for details)

(ns yamlscript.resolver-test
  (:require
   [clojure.edn :as edn]
   [yamlscript.common]
   [yamlscript.composer :as composer]
   [yamlscript.parser :as parser]
   [yamlscript.resolver :as resolver]
   [yamltest.core :as test]))

(test/load-yaml-test-files
  ["test/compiler-stack.yaml"
   "test/data-mode.yaml"
   "test/resolver.yaml"
   "test/compiler.yaml"]
  {:pick #(test/has-keys? [:yamlscript :resolve] %1)
   :test (fn [test]
           (try
             (-> test
               :yamlscript
               parser/parse
               composer/compose
               resolver/resolve)
             (catch Exception e
               (if (:error test)
                 (.getMessage e)
                 (throw e)))))
   :want (fn [test]
           (-> test
             :resolve
             edn/read-string))})
