;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.builder
  (:use yamlscript.debug)
  (:require
   [clojure.string :as str]
   [yamlscript.ast :refer :all]
   [yamlscript.ysreader :as ysreader]))

(declare build-node)

(defn build-pairs [node]
  (let [pairs (-> node first second)]
    (loop [[key val & pairs] pairs
           new []]
      (if key
        (let [key (if (:let key)
                    (let [sym (str/replace (:let key) #" +=" "")]
                      {:Let sym})
                    (build-node key))
              val (build-node val)]
          (recur pairs (conj new key val)))
        {:pairs new}))))

(defn build-exprs [node]
  (let [string (-> node first second)]
    (if (= string "")
      :Empty
      (ysreader/read-string (-> node first second)))))

(defn build-map [node]
  (loop [coll (:map node)
         new []]
    (let [[key val & coll] coll]
      (if key
        (let [key (build-node key)
              val (build-node val)]
          (recur coll (apply conj new [key val])))
        (Map new)))))

(defn build-vect [node]
  (loop [coll (:seq node)
         new []]
    (let [[val & coll] coll]
      (if val
        (let [val (build-node val)]
          (recur coll (conj new val)))
        (Vect new)))))

(defn build-node [node]
  (let [[key] (first node)]
    (case key
      :pairs (build-pairs node)
      :exprs (build-exprs node)
      :istr (Str (:istr node))
      :str (Str (:str node))
      :map (build-map node)
      :seq (build-vect node)
      :int (LNum (:int node))
      :float (DNum (:float node))
      :bool (Bool (:bool node))
      :null (Nil)
      (throw (Exception. (str "Don't know how to build node: " node))))))

(defn build [node] (build-node node))

(comment
  (build {:exprs ""})

  (build {:pairs [{:exprs "println"} {:str "Hello"}]})

  (build {:pairs [{:exprs "inc"} {:exprs "(6 * 7)"}]})

  (build {:pairs [{:exprs "a"} {:exprs "b c"}]})

  (build
   {:pairs [{:exprs "a"}
            {:exprs "b c"}]})
  )
