;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.poly)

(defmacro ^:private -def-seq-1st [defn-name core-name]
  `(defn ~defn-name [a# b#]
     (if (seqable? a#)
       (~core-name a# b#)
       (~core-name b# a#))))

(defmacro ^:private -def-seq-2nd [defn-name core-name]
  `(defn ~defn-name [a# b#]
     (if (seqable? b#)
       (~core-name a# b#)
       (~core-name b# a#))))

(-def-seq-1st +nth nth)
(-def-seq-2nd +take take)
(-def-seq-2nd +drop drop)

(comment)
