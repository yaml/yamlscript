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
   [ys.v0.ys :as ys]
   [yamltest.core :as test]))

(defn fresh-context []
  (runtime/init-context))

(deftest umbrella-imports
  (doseq [spelling ["v0" "ys.v0"]]
    (let [ctx (fresh-context)]
      (is (= "{}" (sci/eval-string* ctx
                    (str "(use " spelling ") (json/dump {})"))))
      (is (= "{}" (sci/eval-string* ctx "(use v0) (json/dump {})")))
      (doseq [module (keys manifest/modules)]
        (is (some? (sci/eval-string* ctx
                     (str "(get (ns-aliases *ns*) '"
                       (subs (str module) 3) ")")))))))
  (let [ctx (fresh-context)]
    (is (= "OK" (sci/eval-string* ctx
                  "(use str :as json) (use v0) (json/upper-case \"ok\")"))))
  (let [ctx (fresh-context)]
    (is (= "{}" (sci/eval-string* ctx
                  "(use ys.json) (use v0) (json/dump {})"))))
  (doseq [allowed [#{} '#{ys.json}]]
    (with-redefs [ys/configured-modules (constantly allowed)]
      (let [ctx (fresh-context)]
        (sci/eval-string* ctx "(use v0)")
        (is (= (contains? allowed 'ys.json)
              (boolean (sci/eval-string* ctx
                         "(get (ns-aliases *ns*) 'json)"))))
        (is (thrown? Exception (sci/eval-string* ctx "(use http)"))))))
  (doseq [form ["(use v0 :all)" "(use v0 :as x)" "(use ys.v0 :none)"]]
    (is (thrown-with-msg? Exception #"does not accept modifiers"
          (sci/eval-string* (fresh-context) form)))))

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
  (testing "short uses add matching aliases"
    (let [ctx (fresh-context)]
      (sci/eval-string* ctx "(use http fs ipc io)")
      (doseq [sym '[http/curl fs/read ipc/sh io/readline]]
        (is (some? (sci/eval-string* ctx (str "(resolve '" sym ")")))
          (str "short use resolves " sym)))))
  (testing "short uses with options do not add aliases"
    (let [ctx (fresh-context)]
      (sci/eval-string* ctx "(use http :get curl)")
      (is (some? (sci/eval-string* ctx "(resolve 'curl)")))
      (is (nil? (sci/eval-string* ctx "(resolve 'http/curl)")))))
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

(deftest restricts-core-side-effects
  (let [ctx (fresh-context)]
    (doseq [sym manifest/hidden-core]
      (is (nil? (sci/eval-string* ctx
                  (str "(resolve 'clojure.core/" sym ")")))
        (str "core function is hidden: " sym)))
    (sci/eval-string* ctx "(use clj)")
    (doseq [sym '[compile flush line-seq load-file load-reader newline
                  pr prn printf println read-line]]
      (is (some? (sci/eval-string* ctx (str "(resolve 'clj/" sym ")")))
        (str "core function is available from ys::clj: " sym)))
    (is (nil? (sci/eval-string* ctx "(resolve 'clj/slurp)")))
    (is (nil? (sci/eval-string* ctx "(resolve 'clj/spit)")))))

(deftest reselects-standard-functions
  (testing "selection removes only automatic standard mappings"
    (let [ctx (fresh-context)]
      (sci/eval-string* ctx "(use std :not read write)")
      (is (nil? (sci/eval-string* ctx "(resolve 'read)")))
      (is (nil? (sci/eval-string* ctx "(resolve 'write)")))
      (is (some? (sci/eval-string* ctx "(resolve 'say)")))))
  (testing "get and all select exact standard sets"
    (let [ctx (fresh-context)]
      (sci/eval-string* ctx "(use std :get say)")
      (is (some? (sci/eval-string* ctx "(resolve 'say)")))
      (is (nil? (sci/eval-string* ctx "(resolve 'read)")))
      (sci/eval-string* ctx "(use std :all)")
      (is (some? (sci/eval-string* ctx "(resolve 'read)")))))
  (testing "bare std use only adds an alias"
    (let [ctx (fresh-context)]
      (sci/eval-string* ctx "(use std)")
      (is (some? (sci/eval-string* ctx "(resolve 'read)")))
      (is (some? (sci/eval-string* ctx "(resolve 'std/read)")))))
  (testing "selection preserves user definitions"
    (let [ctx (fresh-context)]
      (sci/eval-string* ctx "(def read :local)")
      (sci/eval-string* ctx "(use std :none)")
      (is (= :local (sci/eval-string* ctx "read"))))))

(deftest owns-stream-and-file-io
  (let [ctx (fresh-context)]
    (is (= ["one" "two" nil]
          (sci/eval-string* ctx
            (str "(with-in-str \"one\\ntwo\\n\" "
              "[(readline) (readline) (readline)])"))))
    (is (= "explicit"
          (sci/eval-string* ctx
            "(with-in-str \"explicit\\n\" (readline *in*))")))
    (sci/eval-string* ctx "(use io)")
    (is (nil? (sci/eval-string* ctx "(resolve 'io/reader)")))
    (is (some? (sci/eval-string* ctx "(resolve 'io/readline)")))
    (is (= "line\n"
          (sci/eval-string* ctx
            "(with-out-str (io/say \"line\"))")))
    (is (= "nested\n"
          (with-out-str
            (ys/eval "!ys-0\nsay: \"nested\"\n"))))))

(deftest delegates-standard-file-io
  (let [ctx (fresh-context)
        file (java.io.File/createTempFile "ys-fs-" ".txt")
        path (.getCanonicalPath file)]
    (try
      (sci/eval-string* ctx
        (str "(write " (pr-str path) " \"standard\")"))
      (sci/eval-string* ctx "(use fs)")
      (is (= "standard"
            (sci/eval-string* ctx
              (str "(fs/read " (pr-str path) ")"))))
      (sci/eval-string* ctx
        (str "(fs/write " (pr-str path) " \"module\")"))
      (is (= "module"
            (sci/eval-string* ctx
              (str "(read " (pr-str path) ")"))))
      (finally
        (.delete file)))))

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
