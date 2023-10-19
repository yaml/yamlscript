;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.resolver
  (:use yamlscript.debug)
  (:require [clojure.set :as set])
  (:refer-clojure :exclude [resolve]))

;; ----------------------------------------------------------------------------
;; Generic helpers:
;; ----------------------------------------------------------------------------
(defn tag-node [node tag]
  (assoc node :tag tag))

;; ----------------------------------------------------------------------------
;; Resolve taggers for ys mode:
;; ----------------------------------------------------------------------------
(defn tag-def [[key val]]
  (let [m (re-matches #"\w+ +=" (:exprs key))]
    (when m
      [(tag-node key "def") val])))

(defn tag-expr [[key val]]
  (cond
    (and (contains? key :exprs) (contains? val :exprs)) [key val]
    (and (contains? key :exprs) (contains? val :str)) [key val]
    (and (contains? key :exprs) (contains? val :istr)) [key val]))

(defn tag-error [[key val]]
  (throw (Exception. (str "Don't know how to tag pair" [key val]))))

;; ----------------------------------------------------------------------------
;; Resolve dispatchers for ys mode:
;; ----------------------------------------------------------------------------
(declare resolve-ys-node)

(defn resolve-ys-pair [key val]
  (let [pair [(resolve-ys-node key)
              (resolve-ys-node val)]]
    ((some-fn
       tag-def
       tag-expr) pair)))

(defn resolve-ys-mapping [node]
  (let [node (dissoc node :tag)]
    (loop [coll (:% node)
           new []]
      (let [[key val & coll] coll]
        (if key
          (recur
            coll
            (apply conj new (resolve-ys-pair key val)))
          {:pairs new})))))

(defn resolve-ys-sequence [node]
  (let [node (dissoc node :tag)]
    (loop [coll (:coll node)
           new []]
      (let [[val & coll] coll]
        (if val
          (recur
            coll
            (conj new (resolve-ys-node val)))
          (assoc node :coll new))))))

(defn resolve-ys-scalar [node]
  (let [style (-> node keys first)]
    (case style
      := (set/rename-keys node {:= :exprs})
      :$ (set/rename-keys node {:$ :istr})
      :' (set/rename-keys node {:' :str})
      :| (set/rename-keys node {:| :istr})
      :> (set/rename-keys node {:> :str})
      ,  (throw (Exception. (str "Scalar has unknown style: " style))))))


;; ----------------------------------------------------------------------------
;; Public API functions:
;; ----------------------------------------------------------------------------
(defn node-type [node]
  (cond
    (:% node) :map
    (:# node) :seq
    :else     :scalar))

(defn resolve-ys-node
  "Resolve nodes recursively in 'ys' mode"
  [node]
  (case (node-type node)
    :map (resolve-ys-mapping node)
    :seq (resolve-ys-sequence node)
    :scalar (resolve-ys-scalar node)))

(defn resolve-data-mapping [node]
  (throw (Exception. "TODO")))
(defn resolve-data-sequence [node]
  (throw (Exception. "TODO")))
(defn resolve-data-scalar [node]
  (throw (Exception. "TODO")))

(defn resolve-data-node
  "Resolve nodes recursively in 'data' mode"
  [node]
  (let [node (dissoc node :anchor)
        node (case (:type node)
               "Mapping" (resolve-data-mapping node)
               "Scalar" (resolve-data-scalar node)
               "Sequence" (resolve-data-sequence node))]
    node))

(defn resolve
  "Walk YAML tree and tag all nodes according to YAMLScript rules."
  [node]
  (resolve-ys-node node))

(comment
  (resolve
    {:! "yamlscript/v0", :% [{:= "a"} {:= "b c"}]})
  )
