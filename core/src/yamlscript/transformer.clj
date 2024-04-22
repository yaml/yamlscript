;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.transformer is responsible for transforming the YAMLScript AST
;; according to certain special rules.

(ns yamlscript.transformer
  (:require
   [yamlscript.ast :refer [Lst]]
   [yamlscript.util :refer [die if-lets]]
   #_[yamlscript.ast :refer [Sym]]
   [yamlscript.transformers]
   [yamlscript.debug :refer [www]]))

(declare
  transform-node
  transform-node-top)

(defn transform
  "Transform special rules for YAMLScript AST."
  [node] (transform-node-top node))

(defn map-vec [f coll] (->> coll (map f) vec))

#_(defn add-num-or-string [{list :Lst}]
  (when (and
          (>= (count list) 3)
          (= (first list) {:Sym '+}))
    (let [list (map-vec transform-node list)
          [_ & rest] list]
      {:Lst (cons {:Sym '+_} rest)})))

#_(defn string-repeat [{list :Lst}]
  (when (and
          (= (count list) 3)
          (= (first list) {:Sym '*}))
    (let [list (map-vec transform-node list)
          [_ v2 v3] list]
      {:Lst [{:Sym '*_} v2 v3]})))

(def transformers-ns (the-ns 'yamlscript.transformers))

(defn apply-transformer [key val]
  (if-lets [name (or
                   (get-in key [:Sym])
                   (get-in key [0 :Sym]))
            sym (symbol (str "transform_" name))
            transformer (ns-resolve transformers-ns sym)]
    (or (transformer key val) [key val])
    [key val]))

(defn transform-pairs [node key]
  (->> node
    first
    val
    (map-vec
      #(if (vector? %1)
         (map-vec transform-node %1)
         (transform-node %1)))
    (partition 2)
    (reduce
      (fn [acc [k v]]
        (let [[k v] (if (= :pairs key)
                      (apply-transformer k v)
                      [k v])]
          (conj acc k v)))
      [])
    (hash-map key)))

(def poly (do (require 'ys.poly)
              (-> 'ys.poly
                ns-publics
                keys
                (->> (map str)
                  (map #(subs %1 1))
                  (map symbol))
                set)))

(defn transform-dot-chain [node] node
  (if-lets [args (:Lst node)
            _ (and
                (= '__ (get-in args [0 :Sym]))
                (> (count args) 2))
            args (map (fn [arg]
                        (if-lets [_ (= 'list (get-in arg [:Lst 0 :Sym]))
                                  _ (not-any?
                                      #(= {:Lst
                                           [{:Sym 'quote} {:Sym '_}]}
                                         %1) (:Lst arg))
                                  sym (get-in arg [:Lst 1 :Sym])
                                  _ (poly sym)
                                  sym (symbol (str "+" sym))]
                          (update-in arg [:Lst 1 :Sym] (constantly sym))
                          arg)) args)]
    (Lst args)
    node))

(defn transform-list [node]
  (let [node (transform-dot-chain node)
        val (map-vec transform-node (:Lst node))]
    (assoc node :Lst val)))

(defn transform-map [node]
  {:Map (map-vec
          transform-node
          (:Map node))})

(defn transform-sym [node]
  (let [sym (str (:Sym node))]
    (when (= sym "%")
      (die "Invalid symbol '%'. Did you mean '%1'?"))
    node))

; TODO:
; Turn :pairs mappings into :forms groups when appropriate.

(defn transform-node [node]
  (let [anchor (:& node)
        node (cond
               (:pairs node) (transform-pairs node :pairs)
               (:forms node) (transform-pairs node :forms)
               (:Lst node) (transform-list node)
               (:Map node) (transform-map node)
               (:Sym node) (transform-sym node)
               :else node)]
    (if anchor
      (assoc node :& anchor)
      node)))

(defn transform-node-top [node]
  (if-lets [[key1 val1 & rest] (:Map node)
            _ (= key1 {:Sym '=>})
            pairs (:pairs val1)]
    {:pairs (concat pairs [{:Sym '=>} {:Map rest}])}
    (if-lets [[first & rest] (:Vec node)
              pairs (get-in first [:pairs 1 :pairs])]
      {:pairs (concat pairs [{:Sym '=>} {:Vec rest}])}
      (transform-node node))))

(comment
  www
  (->>
    {:Map
     [{:Str "my-seq"}
      {:Lst
       [{:Sym '+}
        {:Lst [{:Sym 'load} {:Str "seq1.yaml"}]}
        {:Lst [{:Sym 'load} {:Str "seq2.yaml"}]}]}]}
    transform)
  )
