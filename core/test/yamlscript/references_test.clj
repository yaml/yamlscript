;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.references-test
  (:require
   [clojure.test :refer [deftest is]]
   [yamlscript.references :as references]))

(deftest long-dependency-chain
  (let [size 1500
        tasks (mapv #(hash-map :path [(str %1)]
                               :deps (if (= %1 (dec size)) #{} #{(inc %1)}))
                    (range size))]
    (is (= (vec (reverse (range size)))
           (references/dependency-order tasks)))))
