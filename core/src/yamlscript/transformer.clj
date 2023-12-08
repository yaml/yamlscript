;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.transformer is responsible for transforming the YAMLScript AST
;; according to certain special rules.

(ns yamlscript.transformer
  (:use yamlscript.debug))

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

(defn transform-ysm [node]
  (let [list (-> node first val)]
    (->> list
      (map-vec
        #(if (vector? %)
           (map-vec transform-node %)
           (transform-node %)))
      (hash-map :ysm))))

(defn transform-list [node]
  (or
    (add-num-or-string node)
    (string-repeat node)
    node))

(defn transform-map [node]
  (let [list (:Map node)]
    {:Map (map-vec transform-node list)}))

; TODO:
; Change def to let if not in top level.
; Collapse multiple consecutive lets into one.
; Turn :ysm mappings into :ysg groups when appropriate.

(defn transform-node [node]
  (let [[key] (first node)]
    (case key
      :ysm (transform-ysm node)
      :Lst (transform-list node)
      :Map (transform-map node)
      ,    node)))

(comment
  (->>
    {:Map
     [{:Str "my-seq"}
      {:Lst
       [{:Sym '+}
        {:Lst [{:Sym 'load} {:Str "seq1.yaml"}]}
        {:Lst [{:Sym 'load} {:Str "seq2.yaml"}]}]}]}
    transform)
  )
