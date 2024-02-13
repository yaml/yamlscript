;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.transformers
  (:require
   [yamlscript.util :refer [if-lets when-lets]]
   [yamlscript.ast :refer [Sym Lst Vec Key]]
   [yamlscript.debug :refer [www]]))

(def Q {:Sym 'quote})

;;-----------------------------------------------------------------------------
;; cond
;;-----------------------------------------------------------------------------

(defn transform_cond [lhs rhs]
  (when-lets [forms (:forms rhs)
              len (count forms)
              _ (>= len 2)
              last-key-pos (- len 2)
              last-key (nth forms last-key-pos)
              _ (= '=> (:Sym last-key))
              rhs (update-in rhs [:forms last-key-pos] (fn [_] (Key "else")))]
    [lhs rhs]))

;;-----------------------------------------------------------------------------
;; require
;;-----------------------------------------------------------------------------

(def AS (Key "as"))
(def REFER (Key "refer"))

(defn require-spc-lhs? [lhs]
  (when-lets [sym (get-in lhs [0])
              _ (:Sym sym)
              spc (nth lhs 1)
              _ (:Spc spc)
              _ (= 2 (count lhs))]
    [sym spc]))

(defn require-args [rhs]
  (let [args []
        rhs (if (vector? rhs) rhs [rhs])
        [args rhs] (if-lets [_ (= '=> (get-in rhs [0 :Sym]))
                             alias (nth rhs 1)
                             _ (:Sym alias)]
                     [(conj args AS alias) (drop 2 rhs)]
                     [args rhs])
        args (if (seq rhs)
               (if (every? :Sym rhs)
                 (conj args REFER (Vec rhs))
                 (throw (Exception. "Invalid 'require' arguments")))
               args)]
    args))

(defn require-pairs [pairs]
  (reduce
    (fn [acc [spc rhs]]
      (or (:Spc spc)
        (throw (Exception. "Invalid 'require' pairs")))
      (let [args (if (nil? rhs)
                   (Lst [Q spc])
                   (if (= :all (:Key rhs))
                     (Lst [Q (Vec [spc REFER rhs])])
                     (Lst [Q (Vec (concat [spc] (require-args rhs)))])))]
        (conj acc args)))
    []
    (partition 2 pairs)))

(defn transform_require [lhs rhs]
  (or
    (when-lets [_ (:Sym lhs)
                _ (:Spc rhs)]
      [lhs (Lst [Q rhs])])
    ,
    (when-lets [[sym spc] (require-spc-lhs? lhs)
                _ (nil? rhs)]
      [sym (Lst [Q spc])])
    ,
    (when-lets [[sym spc] (require-spc-lhs? lhs)
                args (require-args rhs)]
      [sym (Lst [Q (Vec (concat [spc] args))])])
    ,
    (when-lets [_ (:Sym lhs)
                pairs (:pairs rhs)
                args (require-pairs pairs)]
      [lhs args])
    ,
    (throw (Exception. "Invalid 'require' form"))))

(comment
  www)
