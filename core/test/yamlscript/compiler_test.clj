;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.compiler-test
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [ys.v0.common]
   [yamlscript.compiler :as compiler]
   [yamltest.core :as test]))

(defn testing-fix-clojure [clj]
  (-> clj
      (str/replace #"(?m)^\(\+\+\+ +(.*)\)$" "$1")
      (str/replace #"(?s)^\(\+\+\+[ \n]+(.*)\)$" "$1")))

(defn clojure-forms [code]
  (walk/postwalk
   #(if (instance? java.util.regex.Pattern %1)
      [:regex (.pattern ^java.util.regex.Pattern %1)] %1)
   (read-string (str "[" code "]"))))

(test/load-yaml-test-files
 ["test/compiler.yaml"
  "test/compiler-stack.yaml"
  "test/literals.yaml"
  "test/transformer.yaml"]
 {:pick #(test/has-keys? [:yamlscript :clojure] %1)
  :test (fn [test]
          (let [code (-> test
                         :yamlscript
                         compiler/compile
                         compiler/pretty-format)]
            (if (:document-wrappers test)
              (clojure-forms code)
              (clojure-forms (testing-fix-clojure code)))))
  :want (fn [test]
          (clojure-forms (:clojure test)))})

(test/load-yaml-test-files
 ["test/compiler.yaml"
  "test/literals.yaml"
  "test/transformer.yaml"]
 {:add-tests true
  :pick #(test/has-keys? [:yamlscript :error] %1)
  :test (fn [test]
          (try
            (-> test
                :yamlscript
                compiler/compile)
            ""
            (catch Exception e
              (:cause (Throwable->map e)))))
  :want :error})
