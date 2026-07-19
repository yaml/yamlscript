;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.v0-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [ys.v0]
   [ys.v0.manifest :as manifest]))

;; Symbols the compiler emits that must resolve after (ys.v0/init)
(def compiled-output-syms
  '[+++ +++* % +concat +def +merge
    _& _* _**
    add+ sub+ mul+ div+ pow rng sum
    say die print condf pprint stream
    eq ne gt ge lt le or? and?
    each value call q qw omap
    +map +filter +apply ++map
    to-num to-str to-vec get+
    DBG PPP WWW XXX
    load use])

(deftest init-in-fresh-namespace
  (let [err (java.io.StringWriter.)
        scratch (create-ns 'ys.v0-test.scratch)]
    (binding [*err* err
              *ns* scratch]
      (refer-clojure)
      (ys.v0/init))

    (testing "init produces no replace warnings"
      (is (= "" (str err))))

    (testing "compiled-output symbols resolve"
      (doseq [sym compiled-output-syms]
        (is (some? (ns-resolve scratch sym))
          (str "symbol resolves: " sym))))

    (testing "every manifest export resolves"
      (doseq [sym (manifest/exported-syms)]
        (is (some? (ns-resolve scratch sym))
          (str "manifest export resolves: " sym))))

    (testing "runtime vars are bound"
      (doseq [sym manifest/runtime-vars]
        (is (some? (ns-resolve scratch sym))
          (str "runtime var resolves: " sym)))
      (is (map? @(ns-resolve scratch 'ENV)))
      (is (string? @(ns-resolve scratch 'CWD)))
      (is (= ys.v0/VERSION @(ns-resolve scratch 'VERSION))))

    (testing "namespace aliases are set up"
      (doseq [[a target] manifest/aliases]
        (is (= target (ns-name (get (ns-aliases scratch) a)))
          (str "alias: " a))))))

(deftest init-twice-is-idempotent
  (let [err (java.io.StringWriter.)
        scratch (create-ns 'ys.v0-test.scratch2)]
    (binding [*err* err
              *ns* scratch]
      (refer-clojure)
      (ys.v0/init)
      (ys.v0/init))
    (is (= "" (str err)))))

(deftest version-skew-warning
  (let [err (java.io.StringWriter.)
        scratch (create-ns 'ys.v0-test.scratch3)]
    (binding [*err* err
              *ns* scratch]
      (refer-clojure)
      (ys.v0/init {:v "0.0.1"}))
    (is (re-find #"WARNING: code compiled by ys 0\.0\.1" (str err)))))
