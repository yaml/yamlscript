;; Copyright 2023-2024 Ingy dot Net
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
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [yamlscript.re :as re]
   [yamlscript.util :refer [when-let*]]
   [yamlscript.debug :refer [www]])
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
  resolve-bare-node
  resolve-data-node
  resolve-code-node)

(defn resolve
  "Walk YAML tree and tag all nodes according to YAMLScript rules."
  [node]
  (let [tag (:! node)]
    (if (and tag (re-find #"^yamlscript/v0" tag))
      (let [tag (subs tag (count "yamlscript/v0"))]
        (case tag
          "" (resolve-code-node node)
          "/:" (resolve-data-node node)
          "/code" (resolve-code-node (dissoc node :!))
          "/data" (resolve-data-node (dissoc node :!))
          "/bare" (resolve-bare-node (dissoc node :!))
          (throw (Exception. (str "Unknown yamlscript tag: " tag)))))
      (resolve-bare-node node))))

;; ----------------------------------------------------------------------------
;; Resolve taggers for code mode:
;; ----------------------------------------------------------------------------
(defn tag-str [[key val]]
  (when-let*
    [str (:ysi key)
     _ (= "" (:ysx val))]
    [{:str str} {:str ""}]))

(defn tag-def [[key val]]
  (when-let*
    [key (:ysx key)
     old key
     rgx (re/re #"^($symw) +=$")
     key (str/replace key rgx "def $1")]
    (when (not= old key)
      [{:ysx key} val])))

(defn tag-defn [[key val]]
  (let [key (:ysx key)
        old key
        rgx (re/re #"^defn ($symb)\((.*)\)$")
        key (str/replace key rgx "defn $1 [$2]")]
    (when (not= old key)
      ; [{:defn key} val])))
      [{:ysx key} val])))

(defn tag-ysx [[key val]]
  (cond
    (and (contains? key :ysx) (contains? val :ysx)) [key val]
    (and (contains? key :ysx) (contains? val :str)) [key val]
    (and (contains? key :ysx) (contains? val :ysi)) [key val]
    (and (contains? key :ysx) (contains? val :ysm)) [key val]))

(defn tag-error [[key val]]
  (throw (Exception. (str "Don't know how to tag pair" [key val]))))

;; ----------------------------------------------------------------------------
;; Resolve dispatchers for code mode:
;; ----------------------------------------------------------------------------
(defn resolve-mode-swap [key val]
  (let [key-text (:= key)
        [key val] (if (and key-text (re-find #":$" key-text))
                    (let [key (assoc key
                                :=
                                (str/replace key-text #"\s*:$" ""))
                          val (assoc val :! "")]
                      [key val])
                    [key val])]
    [key val]))

(defn resolve-code-pair [key val]
  (let [[key val] (resolve-mode-swap key val)
        pair [(resolve-code-node key)
              (resolve-code-node val)]]
    ((some-fn
       tag-str
       tag-def
       tag-defn
       tag-ysx
       identity) pair)))

(defn resolve-code-mapping [node]
  (when (:%% node)
    (throw (Exception. "Flow mappings not allowed in code mode")))
  {:ysm (vec
          (mapcat
            (fn [[key val]] (resolve-code-pair key val))
            (partition 2 (:% node))))})

(defn resolve-code-sequence [_]
  (throw (Exception. "Sequences (block and flow) not allowed in code mode")))

(defn resolve-code-scalar [node]
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

(defn resolve-code-node
  "Resolve nodes recursively in code mode"
  [node]
  (let [tag (:! node)
        node (dissoc node :&)]
    (if (= tag "")
      (resolve-data-node (dissoc node :!))
      (case (node-type node)
        :map (resolve-code-mapping node)
        :seq (resolve-code-sequence node)
        :val (resolve-code-scalar node)))))

;; ----------------------------------------------------------------------------
;; Resolve dispatchers for data mode:
;; ----------------------------------------------------------------------------
(defn resolve-data-mapping [node]
  {:map (vec
          (mapcat
            (fn [[key val]]
              (let [[key val]
                    (resolve-mode-swap key val)]
                 [(resolve-data-node key)
                  (resolve-data-node val)]))
            (partition 2 (or (:% node) (:%% node)))))})

(defn resolve-data-sequence [node]
  {:seq (map resolve-data-node
          (or (:- node) (:-- node)))})

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

(defn resolve-data-scalar [node]
  (let [style (-> node first key)]
    (case style
      := (set/rename-keys node {:= (resolve-plain-scalar node)})
      :$ (set/rename-keys node {:$ :str})
      :' (set/rename-keys node {:' :str})
      :| (set/rename-keys node {:| :str})
      :> (set/rename-keys node {:> :str})
      ,  (throw (Exception. (str "Scalar has unknown style: " style))))))

(defn resolve-data-node
  "Resolve nodes recursively in 'yaml' mode"
  [node]
  (let [tag (:! node)
        node (dissoc node :&)]
    (if (= tag "")
      (resolve-code-node (dissoc node :!))
      (case (node-type node)
        :map (resolve-data-mapping node)
        :seq (resolve-data-sequence node)
        :val (resolve-data-scalar node)))))


;; ----------------------------------------------------------------------------
;; Resolve dispatchers for bare mode:
;; ----------------------------------------------------------------------------
(defn resolve-bare-mapping [node]
  {:map (vec (map resolve-bare-node
               (or (:% node) (:%% node))))})

(defn resolve-bare-sequence [node]
  {:seq (map resolve-bare-node
          (or (:- node) (:-- node)))})

(defn resolve-bare-scalar [node]
  (let [style (-> node first key)]
    (case style
      := (set/rename-keys node {:= (resolve-plain-scalar node)})
      :$ (set/rename-keys node {:$ :str})
      :' (set/rename-keys node {:' :str})
      :| (set/rename-keys node {:| :str})
      :> (set/rename-keys node {:> :str})
      ,  (throw (Exception. (str "Scalar has unknown style: " style))))))

(def bare-mode-tags
  ["tag:yaml.org,2002:map"
   "tag:yaml.org,2002:seq"])

(defn resolve-bare-node
  "Resolve nodes recursively in 'bare' mode"
  [node]
  (let [tag (:! node)
        node (dissoc node :&)]
    (when (and tag (not (some #{tag} bare-mode-tags)))
      (throw (Exception.
               (str "Unrecognized tag in bare mode: !" tag))))
    (case (node-type node)
      :map (resolve-bare-mapping node)
      :seq (resolve-bare-sequence node)
      :val (resolve-bare-scalar node))))

(comment
  www
  (resolve
    #_{:! "yamlscript/v0", :% [{:= "a"} {:= "b c"}]}
    {:! "yamlscript/v0", := ""}
    #__)
  (set/rename-keys {:> 42} {:> :str})
  )
