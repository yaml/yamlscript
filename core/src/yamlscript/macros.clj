;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; A collection of YAMLScript macros that are used to transform certain mappings
;; into other mappings.

(ns yamlscript.macros
  (:require
   [yamlscript.ast :refer [Sym]]
   [yamlscript.util :refer [when-lets]]
   [yamlscript.debug :refer [www]]))

(defn check-is-defn [node]
  (when-lets
    [pair (:pairs node)
     _ (= 2 (count pair))
     [key val] pair
     _ (vector? key)
     _ (map? val)
     [key1 key2 key3] key
     _ (= 'defn (:Sym key1))
     _ (:Sym key2)
     _ (:Vec key3)
     body (:pairs val)
     _ (vector? body)]

    [[key1 key2 key3] body]))

(defn check-cond-case [node]
  (when-lets
    [pair (:pairs node)
     _ (= 2 (count pair))
     [key val] pair
     _ (map? key)
     _ (map? val)
     sym (:Sym key)
     _ (contains? #{'case 'cond 'condp} sym)
     body (:pairs val)]

    [key body]))

(defn pairs-case-cond-condp [node]
  (when-lets
    [[sym body] (check-cond-case node)
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

(defn pairs-defn-docstring [node]
  (when-lets
    [[[key1 key2 key3]
      [doc-string empty & body]] (check-is-defn node)
     _ (:Str doc-string)
     _ (= "" (:Str empty))]

    {:pairs
     [[key1 key2 doc-string key3]
      {:pairs body}]}))

(defn pairs-defn-docstring-arrow [node]
  (when-lets
    [[[key1 key2 key3]
      [arrow doc-string & body]] (check-is-defn node)
     _ (= '=> (:Sym arrow))
     _ (:Str doc-string)]

    {:pairs
     [[key1 key2 doc-string key3]
      {:pairs body}]}))

(def macros-by-tag
  {:pairs
   [pairs-defn-docstring
    pairs-defn-docstring-arrow
    pairs-case-cond-condp]})

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
