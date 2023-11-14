;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; This is the YAMLScript standard library.

(ns ys.core
  (:require
   [clj-yaml.core]
   [sci.core]
   [sci.ctx-store]
   [yamlscript.compiler]
   #__)
  (:refer-clojure :exclude [compile]))

(defn say [& more]
  (apply clojure.core/println more))

(defn ys-compile [code]
  (->> code
    yamlscript.compiler/compile))

(defn ys-eval [code]
  (->> code
    (sci.core/eval-string* sci.ctx-store/get-ctx)))

(defn ys-load [file]
  (->> file
    slurp
    ys-compile
    ys-eval))

(comment
  )
