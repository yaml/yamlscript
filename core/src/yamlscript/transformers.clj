;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.transformers
  (:require
   [yamlscript.util :refer [if-lets when-lets]]
   [yamlscript.ast :refer [Sym Lst Vec Key]]
   [yamlscript.debug :refer [www]]))

(def Q {:Sym 'quote})

(defn transform_cond [key val]
  (when-lets [forms (:forms val)
              len (count forms)
              _ (>= len 2)
              last-key-pos (- len 2)
              last-key (nth forms last-key-pos)
              _ (= '=> (:Sym last-key))
              val (update-in val [:forms last-key-pos] (fn [_] (Key "else")))]
    [key val]))

(defn transform_require [key val]
  (if-lets [_ (:Sym key)
            _ (:Spc val)]
    [key (Lst [Q val])]
    ,
    (if-lets [sym (get-in key [0])
              _ (:Sym sym)
              spc (nth key 1)
              _ (:Spc spc)
              _ (= 2 (count key))
              _ (nil? val)]
      [sym (Lst [Q spc])]
      ,
      (if-lets [sym (get-in key [0])
                _ (:Sym sym)
                spc (nth key 1)
                _ (:Spc spc)
                _ (= 2 (count key))
                _ (= '=> (get-in val [0 :Sym]))
                alias (nth val 1)
                _ (:Sym alias)]
        [sym (Lst [Q (Vec [spc (Key "as") alias])])]
        ,
        (throw (Exception. "Invalid 'require' form"))))))

(comment
  www)
