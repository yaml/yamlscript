; Copyright 2023-2024 Ingy dot Net
; This code is licensed under MIT license (See License for details)

(ns yamlscript.compiler-test
  #_(:use yamlscript.debug)
  (:require
   [clojure.string :as str]
   [yamlscript.compiler :as compiler]
   [yamltest.core :as test]
   [yamlscript.debug :refer [www]]))

(defn testing-fix-clojure [clj]
  (-> clj
    (str/replace #"(?m)^\(\+\+\+ +(.*)\)$" "$1")
    (str/replace #"(?s)^\(\+\+\+[ \n]+(.*)\)$" "$1")))

(test/load-yaml-test-files
  ["test/compiler.yaml"
   "test/compiler-stack.yaml"
   "test/transformer.yaml"]
  {:pick #(test/has-keys? [:yamlscript :clojure] %1)
   :test (fn [test]
           (->> test
             :yamlscript
             compiler/compile
             compiler/pretty-format
             testing-fix-clojure))
   :want :clojure})

(test/load-yaml-test-files
  ["test/compiler.yaml"]
  {:add-tests true
   :pick #(test/has-keys? [:yamlscript :error] %1)
   :test (fn [test]
           (try
             (->> test
               :yamlscript
               compiler/compile)
             ""
             (catch Exception e
               (:cause (Throwable->map e)))))
   :want :error})
