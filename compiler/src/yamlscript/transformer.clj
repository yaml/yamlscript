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

; Change def to let if not in top level.
; Collapse multiple consecutive lets into one.
; Turn :ysm mappings into :ysg groups when appropriate.

(defn transform-node [node] node)

(comment
  )
