;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.plus)

(defn map+ [f coll]
  (cond
    (string? f) (map #(get %1 f) coll)
    :else (map f coll)))

(defn reduce+
  ([a b]
   (cond
     (fn? b) (reduce b a)
     :else (reduce a b)))
  ([a b c]
   (cond
     (fn? c) (reduce c a b)
     :else (reduce a b c))))

(comment
  )
