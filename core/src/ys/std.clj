;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; This is the YAMLScript standard library.

(ns ys.std
  (:require
   [yamlscript.debug]
   [babashka.fs :as fs]
   [babashka.http-client :as http]
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [yamlscript.util :as util])
  (:refer-clojure :exclude [print]))

(defn www [& xs]
  (apply yamlscript.debug/www xs))

(defn xxx [& xs]
  (apply yamlscript.debug/xxx xs))

(defn yyy [& xs]
  (apply yamlscript.debug/yyy xs))

(defn zzz [& xs]
  (apply yamlscript.debug/zzz xs))

(defn toBool [x] (boolean x))

(defn toFloat [x] (parse-double x))

(defn toInt [x] (parse-long x))

(defn toMap
  ([] {})
  ([x] (apply hash-map x))
  ([k v & xs] (apply hash-map k v xs)))

(defn toStr [& xs] (apply str xs))

; toList
; toNum
; toVec

(defn _dot [ctx key]
  (cond
    (symbol? key) (or
                    (get ctx (keyword key))
                    (get ctx (str key))
                    (get ctx key))
    (string? key) (get ctx key)
    (int? key) (nth ctx key)
    (keyword? key) (get ctx key)
    (list? key) (let [[fn & args] key]
                     (apply (resolve fn) ctx args))
    :else (throw (Exception. (str "Invalid key: " key)))))

(defn __ [x & xs]
  (reduce _dot x xs))

(defn _+ [x & xs]
  (cond
    (string? x) (apply str x xs)
    (vector? x) (apply concat x xs)
    (map? x) (apply merge x xs)
    :else (apply + x xs)))

(defn _* [x y]
  (if (and (string? x) (number? y))
    (apply str (repeat y x))
    (if (and (string? y) (number? x))
      (apply str (repeat x y))
      (* x y))))

(defn =-- [str rgx]
  (re-find rgx str))

(defn abspath [& args]
  (apply util/abspath args))

(defn cwd [& args]
  (str (apply babashka.fs/cwd args)))

(defn curl [url]
  (let [url (if (re-find #":" url)
              url
              (str "https://" url))
        resp (http/get url)]
    (if-let [body (:body resp)]
      (str body)
      (throw (Exception. (str resp))))))

(defn die [msg]
  (throw (Exception. ^String msg)))

(defn dirname [& args]
  (apply util/dirname args))

(defmacro each [bindings & body]
  `(do
     (doall (for [~@bindings] (do ~@body)))
     nil))

(defn err [& xs]
  (binding [*out* *err*]
    (apply clojure.core/print xs)
    (flush)))

(defn join
  ([xs] (join "" xs))
  ([sep & xs]
    (if (= 1 (count xs))
      (str/join sep (first xs))
      (str/join sep xs))))

(defn out [& xs]
  (apply clojure.core/print xs)
  (flush))

(defn pow [x y]
  (Math/pow x y))

(defn pp [o]
  (pp/pprint o))

(defn pretty [o]
  (str/trim-newline
    (with-out-str
      (pp/pprint o))))

(defn print [o]
  (clojure.core/print o)
  (flush))

(defn rng [x y]
  (if (> y x)
    (range x (inc y))
    (range x (dec y) -1)))

(defn say [& xs]
  (apply clojure.core/println xs))

(defn sleep [s]
  (Thread/sleep (int (* 1000 s))))

(defn warn [& xs]
  (binding [*out* *err*]
    (apply clojure.core/println xs)
    (flush)))

(comment
  (require '[yamlscript.runtime :as rt])
  (require '[yamlscript.compiler :as comp])
  (->
    "!yamlscript/v0\ndefn sub(a b): a - b\n=>: .{:foo 7}.:foo.sub(20).inc()"
    comp/compile
    (str/replace #"\._" "__")
    rt/eval-string)
  )
