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
  (set/rename-keys node {:exprs tag}))

(defn node-type [node]
  (cond
    (:% node)  :map
    (:%% node) :map
    (:- node)  :seq
    (:-- node) :seq
    :else      :val))

(defn script-mode [node]
  (= (:! node) "yamlscript/v0"))

;; ----------------------------------------------------------------------------
;; Resolve taggers for ys mode:
;; ----------------------------------------------------------------------------
(defn tag-let [[key val]]
  (let [m (re-matches #"\w+ +=" (:exprs key))]
    (when m
      [(tag-node key :let) val])))

(defn tag-defn [[key val]]
  (let [m (re-matches #"XXX \w+ +=" (:exprs key))]
    (when m
      [(tag-node key :def) val])))

(defn tag-exprs [[key val]]
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
       tag-let
       tag-defn
       tag-exprs) pair)))

(defn resolve-ys-mapping [node]
  (when (:%% node)
    (throw (Exception. "Flow mappings not allowed in script-mode")))
  (let [node (dissoc node :! :&)]
    (loop [coll (:% node)
           new []]
      (let [[key val & coll] coll]
        (if key
          (recur
            coll
            (apply conj new (resolve-ys-pair key val)))
          {:pairs new})))))

(defn resolve-ys-sequence [node]
  (throw (Exception. "Sequences (block and flow) not allowed in script-mode")))

(defn resolve-ys-scalar [node]
  (let [node (dissoc node :! :&)
        style (-> node keys first)]
    (case style
      := (set/rename-keys node {:= :exprs})
      :$ (set/rename-keys node {:$ :istr})
      :' (set/rename-keys node {:' :str})
      :| (set/rename-keys node {:| :istr})
      :> (set/rename-keys node {:> :str})
      ,  (throw (Exception. (str "Scalar has unknown style: " style))))))

(defn resolve-ys-node
  "Resolve nodes recursively in 'ys' mode"
  [node]
  (case (node-type node)
    :map (resolve-ys-mapping node)
    :seq (resolve-ys-sequence node)
    :val (resolve-ys-scalar node)))

;; ----------------------------------------------------------------------------
;; Resolve dispatchers for yaml mode:
;; ----------------------------------------------------------------------------
(declare resolve-yaml-node)

(defn resolve-yaml-mapping [node]
  (let [node (dissoc node :! :&)]
    (loop [coll (or (:% node) (:%% node))
           new []]
      (let [[key val & coll] coll]
        (if key
          (let [key (resolve-yaml-node key)
                val (resolve-yaml-node val)]
            (recur coll (apply conj new [key val])))
          {:map new})))))

(defn resolve-yaml-sequence [node]
  (let [node (dissoc node :! :&)]
    (loop [coll (or (:- node) (:-- node))
           new []]
      (let [[val & coll] coll]
        (if val
          (let [val (resolve-yaml-node val)]
            (recur coll (conj new val)))
          {:seq new})))))

(defn resolve-plain-scalar [node]
  (let [val (:= node)]
    (cond
      (re-matches #"-?\d+" val) :int
      (re-matches #"-?\d+\.\d+" val) :float
      (re-matches #"(true|True|TRUE|false|False|FALSE)" val) :bool
      (re-matches #"(|~|null|Null|NULL)" val) :null
      :else :str)))

(defn resolve-yaml-scalar [node]
  (let [node (dissoc node :! :&)
        style (-> node keys first)]
    (case style
      := (set/rename-keys node {:= (resolve-plain-scalar node)})
      :$ (set/rename-keys node {:$ :str})
      :' (set/rename-keys node {:' :str})
      :| (set/rename-keys node {:| :str})
      :> (set/rename-keys node {:> :str})
      ,  (throw (Exception. (str "Scalar has unknown style: " style))))))

(defn resolve-yaml-node
  "Resolve nodes recursively in 'yaml' mode"
  [node]
  (case (node-type node)
    :map (resolve-yaml-mapping node)
    :seq (resolve-yaml-sequence node)
    :val (resolve-yaml-scalar node)))

;; ----------------------------------------------------------------------------
;; Public API functions:
;; ----------------------------------------------------------------------------
(defn resolve
  "Walk YAML tree and tag all nodes according to YAMLScript rules."
  [node]
  (if (script-mode node)
    (resolve-ys-node node)
    (resolve-yaml-node node)))

(comment
  (resolve
   #_{:! "yamlscript/v0", :% [{:= "a"} {:= "b c"}]}
   {:! "yamlscript/v0", := ""}
   #__))
