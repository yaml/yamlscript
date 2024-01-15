;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; A collection of YAMLScript macros that are used to transform certain mappings
;; into other mappings.

(ns yamlscript.macros
  (:require
   [clojure.string :as str]
   [yamlscript.debug :refer [www]]))

(defn defn-docstring [node]
  (if-let
   [new-node
    (when-let [ysm (get-in node [:ysm])]
      (when-let [[key val] (and (= 2 (count ysm)) ysm)]
        (when (and
                (= 'defn (get-in key [0 :Sym]))
                (get-in key [1 :Sym])
                (get-in key [2 :Vec]))
          (when-let [doc-string (and
                                  (= '=> (get-in val [:ysm 0 :Sym]))
                                  (get-in val [:ysm 1 :Str]))]
            (let [[a b c] key
                  node (update-in node [:ysm 1 :ysm] #(drop 2 %1))
                  node (update-in node [:ysm 0]
                         (fn [_] [a b {:Str doc-string} c]))]
              node)))))]
    new-node
    node))

(comment
  www)
