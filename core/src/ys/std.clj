;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; This is the YAMLScript standard library.

(ns ys.std
  (:require
   [yamlscript.debug]
   [clojure.pprint :as pp]
   [clojure.string :as str])
  (:refer-clojure :exclude [print]))

(defn www [& xs]
  (apply yamlscript.debug/www xs))

(defn toB [x] boolean x)

(defn toF [x] (parse-double x))

(defn toI [x] (parse-long x))

(defn toM
  ([] {})
  ([x] (apply hash-map x))
  ([k v & xs] (apply hash-map k v xs)))

(defn toS [& xs] (apply str xs))

(defn => [this] this)

(defn _get [coll key]
  (if (string? key)
    (or (get coll (keyword key))
      (get coll key))
    (get coll key))
  )

(defn _. [x & xs]
  (reduce _get x xs))

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

(defn warn [& xs]
  (binding [*out* *err*]
    (apply clojure.core/println xs)
    (flush)))

(comment)
