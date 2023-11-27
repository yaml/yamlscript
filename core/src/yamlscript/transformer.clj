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

(defn string-repeat [{lst :Lst}]
  (let [[v1 v2 v3] lst]
    (when (and
            (= (count lst) 3)
            (= v1 {:Sym '*}))
      {:Lst [{:Sym 'times} v2 v3]})))

(defn transform-ysm [node]
  (let [ysm (-> node first val)]
    (->> ysm
      (map transform-node)
      (hash-map :ysm))))

(defn transform-lst [node]
  (or
    (string-repeat node)
    node))

; TODO:
; Change def to let if not in top level.
; Collapse multiple consecutive lets into one.
; Turn :ysm mappings into :ysg groups when appropriate.

(defn transform-node [node]
  (if (vector? node)
    (vec (map transform-node node))
    (let [[key] (first node)]
      (case key
        :ysm (transform-ysm node)
        :Lst (transform-lst node)
        ,    node))))

(comment)
