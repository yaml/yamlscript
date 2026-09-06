;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.runtime-test
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [sci.core :as sci]
   [yamlscript.compiler :as compiler]
   [yamlscript.runtime :as runtime]
   [ys.v0.manifest :as manifest]
   [yamltest.core :as test]))

(defn fresh-context []
  (sci/init
    {:namespaces runtime/namespaces
     :classes runtime/classes
     :features #{:clj}
     :load-fn runtime/load-fn}))

(deftest uses-public-modules
  (testing "public modules are absent from a new context"
    (let [ctx (fresh-context)]
      (doseq [module (keys manifest/modules)]
        (is (not (contains? runtime/namespaces module))
          (str "module is not preloaded: " module))
        (is (nil? (sci/find-ns ctx module))
          (str "module is absent: " module)))))
  (testing "every public module loads on demand"
    (let [ctx (fresh-context)]
      (doseq [module (keys manifest/modules)]
        (sci/eval-string* ctx (str "(use (" module "))"))
        (is (some? (sci/find-ns ctx module))
          (str "module is loaded: " module)))))
  (testing "plain and aliased uses load built-in modules"
    (let [ctx (fresh-context)]
      (is (thrown? Exception
            (sci/eval-string* ctx "(ys.fs/cwd)")))
      (is (thrown? Exception
            (sci/eval-string* ctx "(fs/cwd)")))
      (sci/eval-string* ctx "(use (ys.fs))")
      (is (string? (sci/eval-string* ctx "(ys.fs/cwd)")))
      (is (thrown? Exception
            (sci/eval-string* ctx "(fs/cwd)")))
      (sci/eval-string* ctx "(use (ys.str :as str))")
      (is (= "ALIAS"
            (sci/eval-string* ctx "(str/upper-case \"alias\")")))))
  (testing "get, rename and exclusion options are preserved"
    (let [ctx (fresh-context)]
      (sci/eval-string* ctx
        "(use (ys.str :get lower-case/downcase))")
      (is (= "mixed"
            (sci/eval-string* ctx "(downcase \"MIXED\")")))
      (sci/eval-string* ctx
        "(use (ys.str :not lower-case))")
      (is (thrown? Exception
            (sci/eval-string* ctx "(lower-case \"missing\")")))
      (is (= "PRESENT"
            (sci/eval-string* ctx "(upper-case \"present\")"))))))

(deftest require-is-retired
  (let [ctx (fresh-context)]
    (is (= "The 'require' function is retired. Use 'use' instead."
          (try
            (sci/eval-string* ctx "(require 'ys.str)")
            nil
            (catch Throwable error
              (str/trimr (ex-message error))))))))

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
