;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.transformer is responsible for transforming the YAMLScript AST
;; according to certain special rules.

(ns yamlscript.transformer
  (:require
   [yamlscript.macros]
   [yamlscript.debug :refer [www]]))

(declare transform-node)

(defn transform
  "Transform special rules for YAMLScript AST."
  [node] (transform-node node))

(defn map-vec [f coll] (->> coll (map f) vec))

(defn add-num-or-string [{list :Lst}]
  (when (and
          (>= (count list) 3)
          (= (first list) {:Sym '+}))
    (let [list (map-vec transform-node list)
          [_ & rest] list]
      {:Lst (cons {:Sym '_+} rest)})))

(defn string-repeat [{list :Lst}]
  (when (and
          (= (count list) 3)
          (= (first list) {:Sym '*}))
    (let [list (map-vec transform-node list)
          [_ v2 v3] list]
      {:Lst [{:Sym '_*} v2 v3]})))

(defn transform-pairs [node key]
  (->> node
    first
    val
    (map-vec
      #(if (vector? %1)
         (map-vec transform-node %1)
         (transform-node %1)))
    (hash-map key)
    (yamlscript.macros/apply-macros key)))

(defn transform-list [node]
  (or
    (add-num-or-string node)
    (string-repeat node)
    ;(if (= 'fn (get-in node [:Lst 0 :Sym]))
    (if (:Lst node)
      {:Lst (map-vec transform-node (:Lst node))}
      node)))

(defn transform-map [node]
  {:Map (map-vec
          transform-node
          (:Map node))})

(defn transform-sym [node]
  (let [sym (str (:Sym node))]
    (when (= sym "%")
      (throw (Exception. "Invalid symbol '%'. Did you mean '%1'?")))
    node))

; TODO:
; Turn :pairs mappings into :forms groups when appropriate.

(defn transform-node [node]
  (let [[key] (first node)]
    (case key
      :pairs (transform-pairs node :pairs)
      :forms (transform-pairs node :forms)
      :Lst (transform-list node)
      :Map (transform-map node)
      :Sym (transform-sym node)
      ,    node)))

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
