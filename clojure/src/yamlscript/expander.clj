;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.expander
  (:use yamlscript.debug))

(declare expand)

(defn expand-pairs [node]
  (let [pairs (-> node first second)]
    (loop [[key val & pairs] pairs
           new []]
      (if key
        (recur pairs (conj new (expand key) (expand val)))
        {:do new}))))

(defn expand [node]
  (if (vector? node)
    (->> node
      (map expand)
      (vec))
    (let [[key] (first node)]
      (case key
        :pairs (expand-pairs node)
        ,      node))))

(comment
  (expand {:pairs [{:Sym 'a} [{:Sym 'b} {:Sym 'c}]]})
  )

nil
