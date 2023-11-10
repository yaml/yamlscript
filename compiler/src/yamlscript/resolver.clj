;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.resolver is responsible for tagging each node in the YAML
;; node tree.
;;
;; The tags used by YAMLScript are:
;; * !map - YAML mapping
;; * !seq - YAML sequence
;; * !str - YAML string scalar
;; * !int - YAML integer scalar
;; * !flt - YAML floating point scalar
;; * !bln - YAML boolean scalar
;; * !nil - YAML null scalar
;;
;; * !ysm - YAMLScript mapping
;; * !ysx - YAMLScript expression
;; * !ysi - YAMLScript interpolated string
;;
;; * !empty - YAML empty stream
;;
;; The resolver transforms the keys of the YAMLScript special forms:
;;
;; * def - 'foo =' -> !ysx 'def foo'
;; * defn - 'defn foo(...)' -> !ysx 'defn foo [...]'

(ns yamlscript.resolver
  (:use yamlscript.debug)
  (:require
   [clojure.set :as set]
   [clojure.string :as str])
  (:refer-clojure :exclude [resolve]))

;; ----------------------------------------------------------------------------
;; Generic helpers:
;; ----------------------------------------------------------------------------
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
(defn tag-def [[key val]]
  (let [key (:ysx key)
        old key
        key (str/replace key #"^(\w+) +=$" "def $1")]
    (when (not= old key)
      ; [{:def key} val])))
      [{:ysx key} val])))

(defn tag-defn [[key val]]
  (let [key (:ysx key)
        old key
        key (str/replace key #"^defn (\w+)\((.*)\)$" "defn $1 [$2]")]
    (when (not= old key)
      ; [{:defn key} val])))
      [{:ysx key} val])))

(defn tag-ysx [[key val]]
  (cond
    (and (contains? key :ysx) (contains? val :ysx)) [key val]
    (and (contains? key :ysx) (contains? val :str)) [key val]
    (and (contains? key :ysx) (contains? val :ysi)) [key val]))

(defn tag-error [[key val]]
  (throw (Exception. (str "Don't know how to tag pair" [key val]))))

;; ----------------------------------------------------------------------------
;; Resolve dispatchers for ys mode:
;; ----------------------------------------------------------------------------
(declare
  resolve-ys-node
  resolve-yaml-node)

(defn resolve
  "Walk YAML tree and tag all nodes according to YAMLScript rules."
  [node]
  (if (script-mode node)
    (resolve-ys-node node)
    (resolve-yaml-node node)))

(defn resolve-ys-pair [key val]
  (let [pair [(resolve-ys-node key)
              (resolve-ys-node val)]]
    ((some-fn
       tag-def
       tag-defn
       tag-ysx) pair)))

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
          {:ysm new})))))

(defn resolve-ys-sequence [node]
  (throw (Exception. "Sequences (block and flow) not allowed in script-mode")))

(defn resolve-ys-scalar [node]
  (let [node (dissoc node :! :&)
        style (-> node keys first)]
    (case style
      := (set/rename-keys node {:= :ysx})
      :$ (set/rename-keys node {:$ :ysi})
      :' (set/rename-keys node {:' :str})
      :| (set/rename-keys node {:| :ysi})
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
      (re-matches #"-?\d+\.\d+" val) :flt
      (re-matches #"(true|True|TRUE|false|False|FALSE)" val) :bln
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

(comment
  (resolve
    #_{:! "yamlscript/v0", :% [{:= "a"} {:= "b c"}]}
    {:! "yamlscript/v0", := ""}
    #__))
