;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.resolver-test
  (:require
   [clojure.edn :as edn]
   [clojure.test :refer [deftest is testing]]
   [ys.v0.common]
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
               first
               resolver/resolve)
             (catch Exception e
               (if (:error test)
                 (.getMessage e)
                 (throw e)))))
   :want (fn [test]
           (-> test
             :resolve
             edn/read-string))})

(defn- resolve-code-value [value]
  (-> (str "!ys-0\nx: " value "\n")
    parser/parse
    composer/compose
    first
    resolver/resolve
    :xmap
    second
    :expr))

(deftest colon-escaping
  (testing "Colon escapes YAML syntax at the start of code values"
    (doseq [[value expr]
            [[":'single'" "'single'"]
             [":\"double\"" "\"double\""]
             [":{a 1}" "{a 1}"]
             [":[a b]" "[a b]"]
             [":|pipe" "|pipe"]
             [":>fold" ">fold"]
             [":*star" "*star"]
             [":&amp" "&amp"]
             [":`syntax" "`syntax"]
             [":!tag" "!tag"]
             [":@at" "@at"]
             [":#hash" "#hash"]
             [":%percent" "%percent"]
             [":?question" "?question"]]]
      (is (= expr (resolve-code-value value)))))
  (testing "Colon only escapes the supported adjacent syntax characters"
    (doseq [value [": [a]" ":foo" ":-dash" ":,comma" "::use"]]
      (is (= {:expr value}
             (resolver/resolve-code-scalar {:= value} nil :=))))))
