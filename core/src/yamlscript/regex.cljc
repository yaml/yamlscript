;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.regex
  (:refer-clojure :exclude [compile]))

(defn compile [pattern]
  (re-pattern pattern))
