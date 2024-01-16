;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; A collection of YAMLScript macros that are used to transform certain mappings
;; into other mappings.

(ns yamlscript.macros
  (:require
   [yamlscript.util :refer [when-let*]]
   [yamlscript.debug :refer [www]]))

(defn ysm-is-defn [node]
  (when-let*
    [pair (:ysm node)
     _ (= 2 (count pair))
     [key val] pair
     _ (vector? key)
     _ (map? val)
     [key1 key2 key3] key
     _ (= 'defn (:Sym key1))
     _ (:Sym key2)
     _ (:Vec key3)
     body (:ysm val)
     _ (vector? body)]

    [[key1 key2 key3] body]))

(defn ysm-defn-docstring [node]
  (when-let*
    [[[key1 key2 key3]
      [doc-string empty & body]] (ysm-is-defn node)
     _ (:Str doc-string)
     _ (= "" (:Str empty))]

    {:ysm
     [[key1 key2 doc-string key3]
      {:ysm body}]}))

(defn ysm-defn-docstring-arrow [node]
  (when-let*
    [[[key1 key2 key3]
      [arrow doc-string & body]] (ysm-is-defn node)
     _ (= '=> (:Sym arrow))
     _ (:Str doc-string)]

    {:ysm
     [[key1 key2 doc-string key3]
      {:ysm body}]}))

(def macros-by-tag
  {:ysm
   [ysm-defn-docstring
    ysm-defn-docstring-arrow]})

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
