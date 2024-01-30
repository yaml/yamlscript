;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; A collection of YAMLScript macros that are used to transform certain mappings
;; into other mappings.

(ns yamlscript.macros
  (:require
   [yamlscript.ast :refer [Sym]]
   [yamlscript.util :refer [when-lets]]
   [yamlscript.debug :refer [www]]))

(defn single-pair? [node]
  (when-lets
    [nodes (:pairs node)
     _ (= 2 (count nodes))
     [key val] nodes]
    [key val]))

(defn cond-or-case? [node]
  (when-lets
    [[key val] (single-pair? node)
     _ (map? key)
     _ (map? val)
     sym (:Sym key)
     _ (contains? #{'case 'cond 'condp} sym)
     body (:pairs val)]

    [key body]))

(defn do-case-cond-condp [node]
  (when-lets
    [[sym body] (cond-or-case? node)
     len (count body)
     _ (>= len 2)
     last-key-pos (- len 2)
     last-key (nth body last-key-pos)
     name (:Sym sym)
     body (if (and (contains? #{'cond 'condp} name)
                (= '=> (:Sym last-key)))
            (update-in body [last-key-pos] (fn [_] (Sym "true")))
            body)]
    {:pairs [[sym] {:forms body}]}))

(def macros-by-tag
  {:pairs
   [do-case-cond-condp]})

(defn apply-macros [tag node]
  (let
   [macro-list (get macros-by-tag tag)]
    (reduce
      (fn [node macro]
        (or (macro node) node))
      node
      macro-list)))

(comment
  www)
