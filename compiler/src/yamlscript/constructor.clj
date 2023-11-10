;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.constructor is responsible for converting the YAMLScript AST
;; into a Clojure AST.

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

(defn construct-call [pair]
  (Lst (flatten [pair])))

(defn construct-err [o]
  (throw (Exception. (str "Can't construct " o))))

(defn construct-pair [pair]
  ((some-fn
     construct-call
     construct-err)
   pair))

(defn construct-do [node]
  (let [ysm (node :do)]
    (->> ysm
      (reduce #(conj %1 (construct-pair %2)) [])
      (#(if (= 1 (count %))
          (first %)
          (Lst (flatten [(Sym "do") %])))))))

(defn construct-ysm [node]
  (let [ysm (-> node first val)]
    (->> ysm
      (reduce #(conj %1 (construct-node %2)) [])
      (partition 2)
      (map vec)
      (hash-map :do)
      construct-do)))

(defn construct-node [node]
  (if (vector? node)
    (->> node
      (map construct-node))
    (let [key (-> node first key)]
      (case key
        :ysm (construct-ysm node)
             node))))

(comment
  (construct :Nil)
  (construct {:do [{:Sym 'a} [{:Sym 'b} {:Sym 'c}]]})
  *e
  )
