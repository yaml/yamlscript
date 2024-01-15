;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; A collection of YAMLScript macros that are used to transform certain mappings
;; into other mappings.

(ns yamlscript.macros
  (:require
   [yamlscript.debug :refer [www]]))

(defmacro if-let*
  ([bindings then]
   `(if-let* ~bindings ~then nil))
  ([bindings then else]
   (if (seq bindings)
     `(if-let [~(first bindings) ~(second bindings)]
        (if-let* ~(drop 2 bindings) ~then ~else)
        ~else)
     then)))

(defmacro when-let*
  ([bindings & body]
   (if (seq bindings)
     `(when-let [~(first bindings) ~(second bindings)]
        (when-let* ~(drop 2 bindings) ~@body))
     `(do ~@body))))

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
      [arrow doc-string & body]] (is-defn node)
     _ (= '=> (:Sym arrow))
     _ (:Str doc-string)]

    {:ysm
     [[key1 key2 doc-string key3]
      {:ysm body}]}

    node))

(comment
  www)
