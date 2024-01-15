;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; A collection of YAMLScript macros that are used to transform certain mappings
;; into other mappings.

(ns yamlscript.macros
  (:require
   [yamlscript.util :refer [if-let* when-let*]]
   [yamlscript.debug :refer [www]]))

(defn is-defn [node]
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

(defn defn-docstring [node]
  (if-let*
    [[[key1 key2 key3]
      [doc-string empty & body]] (is-defn node)
     _ (:Str doc-string)
     _ (= "" (:Str empty))]
    {:ysm
     [[key1 key2 doc-string key3]
      {:ysm body}]}
    node))

(defn defn-docstring-arrow [node]
  (if-let*
    [[[key1 key2 key3]
      [arrow doc-string & body]] (is-defn node)
     _ (= '=> (:Sym arrow))
     _ (:Str doc-string)]
    {:ysm
     [[key1 key2 doc-string key3]
      {:ysm body}]}
    node))

(comment
  www)
