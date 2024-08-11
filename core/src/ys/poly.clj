;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.poly
  (:require
   [clojure.string :refer [join]]))

(defn ++map [f coll]
  (cond
    (string? f) (map #(get %1 f) coll)
    :else (map f coll)))

(defmacro ^:private -def-seq-1st [name]
  (let [dname (symbol (str "+" name))]
    `(defn ~dname [a# b#]
       (if (coll? a#)
         (~name a# b#)
         (~name b# a#)))))

(defmacro ^:private -def-seq-2nd [name]
  (let [dname (symbol (str "+" name))]
    `(defn ~dname [a# b#]
       (if (coll? b#)
         (~name a# b#)
         (~name b# a#)))))

(defmacro ^:private -def-seq-2nd+ [name]
  (let [dname (symbol (str "+" name))
        pname (symbol (str "++" name))]
    `(defn ~dname [a# b#]
       (if (coll? b#)
         (~pname a# b#)
         (~pname b# a#)))))

(defmacro ^:private -def-rgx-1st [name]
  (let [dname (symbol (str "+" name))]
    `(defn ~dname [a# b# & c#]
       (if (= java.util.regex.Pattern (type a#))
         (apply ~name a# b# c#)
         (apply ~name b# a# c#)))))

(defmacro ^:private -def-rgx-2nd [name]
  (let [dname (symbol (str "+" name))]
    `(defn ~dname [a# b# & c#]
       (if (= java.util.regex.Pattern (type b#))
         (apply ~name a# b# c#)
         (apply ~name b# a# c#)))))

#_(defmacro ^:private -def-mac [name]
  (let [dname (symbol (str "+" name))]
    `(defn ~dname [& xs#]
       (apply ~name xs#))))

(-def-seq-2nd apply)
(-def-seq-1st contains?)
(-def-seq-2nd drop)
(-def-seq-2nd drop-last)
(-def-seq-2nd drop-while)
(-def-seq-2nd every?)
(-def-seq-2nd filter)
(-def-seq-2nd filterv)
(-def-seq-2nd join)
(-def-seq-2nd keep)
(-def-seq-2nd+ map)
(-def-seq-2nd mapv)
(-def-seq-2nd not-any?)
(-def-seq-1st nth)
(-def-seq-2nd partition)
(-def-seq-2nd random-sample)
(-def-rgx-1st re-find)
(-def-rgx-1st re-matches)
(-def-rgx-1st re-seq)
(-def-seq-2nd remove)
#_(-def-rgx-2nd replace)
(-def-seq-2nd some)
(-def-seq-2nd split-at)
(-def-seq-2nd split-with)
(-def-seq-2nd take)
(-def-seq-2nd take-last)
(-def-seq-2nd take-while)
#_(-def-mac when)

(comment)
