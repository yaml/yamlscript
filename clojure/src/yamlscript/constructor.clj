;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.constructor
  (:use yamlscript.debug)
  (:require
   [clojure.string :as str]
   [yamlscript.ast :refer :all]))

(declare construct-node)

(defn construct
  "Construct resolved YAML tree into a YAMLScript AST."
  [node]
  (construct-node node))

(defn construct-let [pair]
  (let [[key val] pair]
    (when (:Let key)
      (List [(Sym "def") (Sym (:Let key)) val]))))

(defn construct-call [pair]
  (List (flatten [pair])))

(defn construct-err [o]
  (throw (Exception. (str "Can't construct " o))))

(defn construct-pair [pair]
  ((some-fn
     construct-let
     construct-call
     construct-err)
    pair))

(defn construct-do [node]
  (let [pairs (node :do)]
    (loop [[[key val] & pairs] pairs
           body []]
      (if key
        (recur pairs
          (conj body (construct-pair [key val])))
        (if (= 1 (count body))
          (first body)
          (List (flatten [(Sym "do") body])))))))

(defn construct-node [node]
  (if (keyword? node)
    node
    (let [[key] (first node)]
      (case key
        :do (construct-do node)
        ,   node))))

(comment
  (construct :Nil)
  (construct {:do [{:Sym 'a} [{:Sym 'b} {:Sym 'c}]]})
  *e
  )
