;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.common)

(def opts (atom {}))

(def $ (atom {}))
(def $# (atom 0))

(def stream-anchors_ (atom {}))
(def doc-anchors_ (atom {}))

(comment
  )
