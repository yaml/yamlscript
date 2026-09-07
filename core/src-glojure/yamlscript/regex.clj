;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.regex
  (:require [clojure.string :as str])
  (:refer-clojure :exclude [compile]))

(defn normalize [pattern]
  (-> (str pattern)
    (str/replace #"([?*+])\+" "$1")
    (str/replace #"(\{[0-9]+(?:,[0-9]*)?\})\+" "$1")))

(defn compile [pattern]
  (github.com:glojurelang:glojure:pkg:lang.CachedCompileRegexp
    (normalize pattern)))
