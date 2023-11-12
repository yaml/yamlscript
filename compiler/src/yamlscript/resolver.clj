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

(declare
  resolve-data-node
  resolve-yaml-node
  resolve-script-node
  )

(defn resolve
  "Walk YAML tree and tag all nodes according to YAMLScript rules."
  [node]
  (let [tag (:! node)
        node-no-tag (dissoc node :!)]
    (if (and tag (re-find #"^yamlscript/v0" tag))
      (let [tag (subs tag (count "yamlscript/v0"))]
        (case tag
          "" (resolve-script-node node)
          "/" (resolve-yaml-node node)
          "/map" (resolve-yaml-node node)
          "/seq" (resolve-yaml-node node)
          "/data" (resolve-data-node node-no-tag)
          (throw (Exception. (str "Unknown yamlscript tag: " tag)))))
      (resolve-data-node node))))

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
(defn resolve-script-pair [key val]
  (let [pair [(resolve-script-node key)
              (resolve-script-node val)]]
    ((some-fn
       tag-def
       tag-defn
       tag-ysx) pair)))

(defn resolve-script-mapping [node]
  (when (:%% node)
    (throw (Exception. "Flow mappings not allowed in script-mode")))
  (let [node (dissoc node :!)]
    (loop [coll (:% node)
           new []]
      (let [[key val & coll] coll]
        (if key
          (recur
            coll
            (apply conj new (resolve-script-pair key val)))
          {:ysm new})))))

(defn resolve-script-sequence [node]
  (throw (Exception. "Sequences (block and flow) not allowed in script-mode")))

(defn resolve-script-scalar [node]
  (let [node (dissoc node :!)
        [key val] (-> node first)]
    (case key
      := (let [node
               ;; Remove leading escape character from value
               (if (re-find #"^\.[\`\!\@\#\%\&\*\-\{\[\|\:\'\"\,\>\?]" val)
                 (assoc node := (subs val 1))
                 node)]
           (set/rename-keys node {:= :ysx}))
      :$ (set/rename-keys node {:$ :ysi})
      :' (set/rename-keys node {:' :str})
      :| (set/rename-keys node {:| :ysi})
      :> (set/rename-keys node {:> :str})
      ,  (throw (Exception. (str "Scalar has unknown style: " key))))))

(defn resolve-script-node
  "Resolve nodes recursively in script-mode"
  [node]
  (let [tag (:! node)
        node (dissoc node :&)
        node-no-tag (dissoc node :!)]
    (if (= tag "")
      (resolve-yaml-node node-no-tag)
      (case (node-type node)
        :map (resolve-script-mapping node)
        :seq (resolve-script-sequence node)
        :val (resolve-script-scalar node)))))

;; ----------------------------------------------------------------------------
;; Resolve dispatchers for yaml mode:
;; ----------------------------------------------------------------------------
(defn resolve-yaml-mapping [node]
  (let [node (dissoc node :!)]
    (loop [coll (or (:% node) (:%% node))
           new []]
      (let [[key val & coll] coll]
        (if key
          (let [key (resolve-yaml-node key)
                val (resolve-yaml-node val)]
            (recur coll (apply conj new [key val])))
          {:map new})))))

(defn resolve-yaml-sequence [node]
  (let [node (dissoc node :!)]
    (loop [coll (or (:- node) (:-- node))
           new []]
      (let [[val & coll] coll]
        (if val
          (let [val (resolve-yaml-node val)]
            (recur coll (conj new val)))
          {:seq new})))))

(def re-int #"(?:[-+]?[0-9]+|0o[0-7]+|0x[0-9a-fA-F]+)")
(def re-float #"[-+]?(\.[0-9]+|[0-9]+(\.[0-9]*)?)([eE][-+]?[0-9]+)?")
(def re-bool #"(?:true|True|TRUE|false|False|FALSE)")
(def re-null #"(?:|~|null|Null|NULL)")
(def re-inf-nan #"(?:[-+]?(?:\.inf|\.Inf|\.INF)|\.nan|\.NaN|\.NAN)")

(defn resolve-plain-scalar [node]
  (let [val (:= node)]
    (when (re-matches re-inf-nan val)
      (throw (Exception. (str "Inf and NaN not supported in YAMLScript"))))
    (cond
      (re-matches re-int val) :int
      (re-matches re-float val) :flt
      (re-matches re-bool val) :bln
      (re-matches re-null val) :nil
      :else :str)))

(defn resolve-yaml-scalar [node]
  (let [node (dissoc node :!)
        style (-> node first key)]
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
  (let [tag (:! node)
        node (dissoc node :&)
        node-no-tag (dissoc node :!)]
    (if (= tag "")
      (resolve-script-node node-no-tag)
      (case (node-type node)
        :map (resolve-yaml-mapping node)
        :seq (resolve-yaml-sequence node)
        :val (resolve-yaml-scalar node)))))


;; ----------------------------------------------------------------------------
;; Resolve dispatchers for data mode:
;; ----------------------------------------------------------------------------
(defn resolve-data-mapping [node]
  (let [node (dissoc node :!)]
    (loop [coll (or (:% node) (:%% node))
           new []]
      (let [[key val & coll] coll]
        (if key
          (let [key (resolve-data-node key)
                val (resolve-data-node val)]
            (recur coll (apply conj new [key val])))
          {:map new})))))

(defn resolve-data-sequence [node]
  (let [node (dissoc node :!)]
    (loop [coll (or (:- node) (:-- node))
           new []]
      (let [[val & coll] coll]
        (if val
          (let [val (resolve-data-node val)]
            (recur coll (conj new val)))
          {:seq new})))))

(defn resolve-data-scalar [node]
  (let [node (dissoc node :!)
        style (-> node first key)]
    (case style
      := (set/rename-keys node {:= (resolve-plain-scalar node)})
      :$ (set/rename-keys node {:$ :str})
      :' (set/rename-keys node {:' :str})
      :| (set/rename-keys node {:| :str})
      :> (set/rename-keys node {:> :str})
      ,  (throw (Exception. (str "Scalar has unknown style: " style))))))

(def data-mode-tags
  ["tag:yaml.org,2002:map"
   "tag:yaml.org,2002:seq"])

(defn resolve-data-node
  "Resolve nodes recursively in 'data' mode"
  [node]
  (let [tag (:! node)
        node (dissoc node :&)]
    (when (and tag (not (some #{tag} data-mode-tags)))
      (throw (Exception.
               (str "Unrecognized tag in data mode: !" tag))))
    (case (node-type node)
      :map (resolve-data-mapping node)
      :seq (resolve-data-sequence node)
      :val (resolve-data-scalar node))))

(some #{"xmap"} ["map" "seq"])
(comment
  (resolve
    #_{:! "yamlscript/v0", :% [{:= "a"} {:= "b c"}]}
    {:! "yamlscript/v0", := ""}
    #__)
  (set/rename-keys {:> 42} {:> :str})
  )
