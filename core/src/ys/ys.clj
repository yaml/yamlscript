;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.ys
  (:require
   [yamlscript.compiler]
   [yamlscript.debug :refer [www]])
  (:refer-clojure :exclude [compile]))

(defn compile [code]
  (yamlscript.compiler/compile code))

(comment
  www
  )
