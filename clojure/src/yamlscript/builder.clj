;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.builder
  (:use yamlscript.debug)
  (:require
   [yamlscript.ysreader :as ysreader]))

(declare build)

(defn build-pairs [node]
  (let [pairs (-> node first second)]
    (loop [[key val & pairs] pairs
           new []]
      (if key
        (recur pairs (conj new (build key) (build val)))
        {:pairs new}))))

(defn build-exprs [node]
  (ysreader/read-string (-> node first second)))

(defn build [node]
  (let [[key] (first node)]
    (case key
      :pairs (build-pairs node)
      :exprs (build-exprs node))))

(comment
  (build {:pairs [{:exprs "a"} {:exprs "b c"}]})
  )

nil
