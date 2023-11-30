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

(defn call-main []
  (Lst [(Sym 'apply)
        (Sym 'main)
        (Sym 'ARGV)]))

(defn maybe-call-main [node]
  (if (and (= 'do (get-in node [:Lst 0 :Sym]))
        (seq
          (filter #(and
                     (= 'defn (get-in % [:Lst 0 :Sym]))
                     (= 'main (get-in % [:Lst 1 :Sym])))
            (node :Lst))))
    (update-in node [:Lst] conj (call-main))
    node))

(defn construct
  "Construct resolved YAML tree into a YAMLScript AST."
  [node]
  (->> node
    construct-node
    maybe-call-main))

(defn construct-call [pair]
  (let [pair (if (= '=> (get-in pair [0 :Sym]))
               (rest pair)
               pair)
        pair (if (= 1 (count pair))
               (first pair)
               (Lst (flatten [pair])))]
    pair))

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
  )
