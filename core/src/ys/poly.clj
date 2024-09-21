;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.poly)

(defn ++map [a b]
  (if (and (ifn? b) (or (sequential? a) (string? a)))
    (map b a)
    (map a b)))

(defn ++mapv [a b]
  (if (and (ifn? b) (or (sequential? a) (string? a)))
    (mapv b a)
    (mapv a b)))


;;------------------------------------------------------------------------------
(defmacro ^:private -seq-1st [name]
  (let [dname (symbol (str "+" name))]
    `(defn ~dname [a# b#]
       (if (coll? a#)
         (~name a# b#)
         (~name b# a#)))))

(defmacro ^:private -seq-2nd [name]
  (let [dname (symbol (str "+" name))]
    `(defn ~dname [a# b#]
       (if (coll? b#)
         (~name a# b#)
         (~name b# a#)))))

(defmacro ^:private -seq-last [name]
  (let [dname (symbol (str "+" name))]
    `(defn ~dname [a# & xs#]
       (if (seqable? a#)
         (apply ~name (concat xs# [a#]))
         (apply ~name a# xs#)))))

(defmacro ^:private -seq-2nd+ [name]
  (let [dname (symbol (str "+" name))
        pname (symbol (str "++" name))]
    `(defn ~dname [a# b#]
       (if (coll? b#)
         (~pname a# b#)
         (~pname b# a#)))))

(defmacro ^:private -rgx-1st [name]
  (let [dname (symbol (str "+" name))]
    `(defn ~dname [a# b# & c#]
       (if (= java.util.regex.Pattern (type a#))
         (apply ~name a# b# c#)
         (apply ~name b# a# c#)))))

#_(defmacro ^:private -rgx-2nd [name]
  (let [dname (symbol (str "+" name))]
    `(defn ~dname [a# b# & c#]
       (if (= java.util.regex.Pattern (type a#))
         (apply ~name b# a# c#)
         (apply ~name a# b# c#)))))

(defmacro ^:private -clj-mac [name]
  (let [dname (symbol (str "+" name))]
    `(defn ~dname
       ([a#] (~name a#))
       ([a# b#] (~name a# b#))
       ([a# b# c#] (~name a# b# c#))
       ([a# b# c# d#] (~name a# b# c# d#))
       ([a# b# c# d# e#] (~name a# b# c# d# e#)))))


;;------------------------------------------------------------------------------
(-seq-2nd apply)
(-seq-2nd cons)
(-seq-1st contains?)
(-seq-2nd drop)
(-seq-2nd drop-last)
(-seq-2nd drop-while)
(-seq-2nd every?)
(-seq-2nd filter)
(-seq-2nd filterv)
(-seq-2nd interpose)
(-seq-2nd keep)
(-seq-2nd+ map)
(-seq-2nd mapcat)
(-seq-2nd+ mapv)
(-seq-2nd not-any?)
(-seq-1st nth)
(-seq-last partition)
(-seq-2nd random-sample)
(-rgx-1st re-find)
(-rgx-1st re-matches)
(-rgx-1st re-seq)
(-seq-last reduce)
(-seq-2nd remove)
(-seq-2nd repeat)
#_(-rgx-2nd replace)
(-seq-2nd some)
(-seq-2nd split-at)
(-seq-2nd split-with)
(-seq-2nd take)
(-seq-2nd take-last)
(-seq-2nd take-while)
(-clj-mac when)

(comment
  )
