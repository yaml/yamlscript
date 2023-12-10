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

(defn get-undefined [node defn-names defined undefined]
  (if (:Lst node)
    (loop [nodes (:Lst node)
           defined defined
           undefined undefined]
      (let [[node & rest] nodes]
        (if (nil? node)
          undefined
          (let [defined (if (= 'defn (get-in node [:Lst 0 :Sym]))
                          (assoc defined (get-in node [:Lst 1 :Sym]) true)
                          defined)
                undefined (let [name (get-in node [:Lst 0 :Sym])]
                            (if (and (get defn-names name)
                                  (not (get defined name)))
                              (assoc undefined name true)
                              undefined))]
            (recur rest defined
              (merge
                undefined
                (get-undefined node defn-names defined undefined)))))))
    undefined))

(defn declare-undefined [node]
  (let [defn-names (map #(get-in % [:Lst 1 :Sym])
                     (filter #(= 'defn (get-in % [:Lst 0 :Sym]))
                       (rest (get-in node [:Lst]))))
        defn-names (zipmap defn-names (repeat true))
        undefined (map Sym
                    (keys (get-undefined node defn-names {} {})))
        form (Lst (cons (Sym 'declare) undefined))]
    (if (seq undefined)
      (update-in
        node
        [:Lst]
        #(let [[a b] (split-at 1 %)]
           (vec (concat a [form] b))))
      node)))

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
    (if (and
          (= 'defn (get-in node [:Lst 0 :Sym]))
          (= 'main (get-in node [:Lst 1 :Sym])))
      (Lst [(Sym 'do) node (call-main)])
      node)))

(defn construct
  "Construct resolved YAML tree into a YAMLScript AST."
  [node]
  (->> node
    construct-node
    declare-undefined
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
  (->> node
    :do
    (reduce #(conj %1 (construct-pair %2)) [])
    (#(if (= 1 (count %))
        (first %)
        (Lst (flatten [(Sym "do") %]))))))

(comment
  (www
    (construct
      {:ysm
       [{:Sym 'a}
        {:ysm
         [[{:Sym 'b} {:Sym 'c}]
          {:Sym 'd}
          {:Sym 'e}
          [{:Sym 'f} {:Sym 'g}]]}]}))

  {:Lst
   [{:Sym a}
    {:Lst
     [{:Sym do}
      {:Lst [{:Sym 'b} {:Sym 'c} {:Sym 'd}]}
      {:Lst [{:Sym 'e} {:Sym 'f} {:Sym 'g}]}]}]}
  )

(defn construct-ysm [node]
  (->> node
    first
    val
    (reduce
      #(conj %1
         (if (vector? %2)
           (map construct-node %2)
           (construct-node %2)))
      [])
    (partition 2)
    (map vec)
    (hash-map :do)
    construct-do))

(defn construct-node [node]
  (-> node
    first
    key
    (case
      :ysm (construct-ysm node)
      ,    node)))

(comment
  (construct
    {:ysm
     ([{:Sym 'defn} {:Sym 'foo} {:Vec [{:Sym 'x}]}]
      {:ysm
       '([{:Sym 'def} {:Sym 'y}]
        {:Lst ({:Sym 'add} {:Sym 'x} {:Int 1})}
        [{:Sym 'def} {:Sym 'x}]
        {:Lst [{:Sym 'times} {:Sym 'y} {:Sym 'x}]}
        {:Sym '=>}
        {:Sym 'y})})})
  (construct :Nil)
  (construct {:do [{:Sym 'a} [{:Sym 'b} {:Sym 'c}]]})
  )
