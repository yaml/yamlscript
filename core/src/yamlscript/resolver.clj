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
;; * !pairs - YAMLScript mapping - pair creates form
;; * !forms - YAMLScript mapping - lhs and rhs create separate forms
;; * !exp   - YAMLScript expression
;; * !form  - YAMLScript expression - node creates form
;; * !vstr  - YAMLScript interpolated string
;;
;; * !empty - YAML empty stream
;;
;; The resolver transforms the keys of the YAMLScript special forms:
;;
;; * def  - 'foo =' -> !exp 'def foo'
;; * defn - 'defn foo(...)' -> !exp 'defn foo [...]'

(ns yamlscript.resolver
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [yamlscript.common]
   [yamlscript.re :as re])
  (:refer-clojure :exclude [resolve]))

;; ----------------------------------------------------------------------------
;; Generic helpers:
;; ----------------------------------------------------------------------------
(defn node-type [node]
  (condf node
    :%  :map
    :%% :map
    :-  :seq
    :-- :seq
    :*  :ali
        :val))

(declare
  resolve-bare-node
  resolve-data-node
  resolve-data-node-top
  resolve-code-node)

(defn resolve
  "Walk YAML tree and tag all nodes according to YAMLScript rules."
  [node]
  (let [tag (:! node)
        node (dissoc node :!)]
    (if (and tag (re-find #"^yamlscript/v0" tag))
      (let [full-tag tag
            tag (subs tag (count "yamlscript/v0"))]
        (case tag
          "" (resolve-code-node node)
          "/" (resolve-data-node-top node)
          "/code" (resolve-code-node node)
          "/data" (resolve-data-node-top node)
          "/bare" (resolve-bare-node node)
          (die "Unknown yamlscript tag: !" full-tag)))
      (resolve-bare-node node))))

(defn get-scalar-style [node]
  (some #(when (%1 node) %1) [:= :$ :' :| :>]))

;; ----------------------------------------------------------------------------
;; Resolve taggers for code mode:
;; ----------------------------------------------------------------------------
(defn tag-str [[key val]]
  (when-lets [str (or (:vstr key) (:str key))
              _ (= "" (:exp val))]
    [{:str str} nil]))

(defn tag-fn [[{key :exp} val]]
  (when (re-matches re/afnk key)
    [{:fn key} val]))

(defn tag-def [[{key :exp} val]]
  (when (re-matches re/defk key)
    [{:def key} val]))

(defn tag-defn [[{key :exp} val]]
  (when (re-matches re/dfnk key)
    [{:defn key} val]))

(defn tag-forms [[key val]]
  (when-lets [_ (or
                  (re-find #" +%$" (:exp key))
                  (re-matches #"(cond|condp .+|case .+)" (:exp key)))
              _ (contains? val :pairs)
              key (assoc key :exp (str/replace (:exp key) #" +%$" ""))
              val (set/rename-keys val {:pairs :forms})]
    [key val]))

(defn tag-exp [[key val]]
  (when-lets [_ (contains? key :exp)
              _ (some val [:exp :str :vstr :pairs])]
    (let [key (if (re-find #" +\|$" (:exp key))
                {:form (assoc key :exp (str/replace (:exp key) #" +\|$" ""))}
                key)]
      [key val])))

(defn tag-error [[key val]]
  (die "Don't know how to tag pair" [key val]))

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
       tag-fn
       tag-def
       tag-defn
       tag-forms
       tag-exp
       identity) pair)))

(defn resolve-code-mapping [node]
  (when (:%% node)
    (die "Flow mappings not allowed in code mode"))
  (let [anchor (:& node)
        node {:pairs (vec
                       (mapcat
                         (fn [[key val]] (resolve-code-pair key val))
                         (partition 2 (:% node))))}
        node (if anchor (assoc node :& anchor) node)]
    (if-lets [[key val] (:pairs node)
              key-str (:exp key)
              _ (re-matches re/dfnk key-str)]
      {:defn [key val]}
      node)))

(defn resolve-code-sequence [_]
  (die "Sequences (block and flow) not allowed in code mode"))

(defn resolve-code-scalar [node]
  (let [tag (:! node)
        node (dissoc node :!)
        style (get-scalar-style node)
        val (style node)
        _ (when (and tag (not= "clj" tag))
            (die "Scalar has unknown tag: !" tag))]
    (if tag
      (set/rename-keys node {style (keyword tag)})
      (case style
        := (let [node
                 ;; Remove leading escape character from value
                 (if
                  (or
                    (re-find
                      #"^[\.\+][\`\!\@\#\%\&\*\-\{\[\|\:\'\"\,\>\?]" val)
                    (re-find
                      #"^-[\`\!\@\#\%\&\*\-\{\[\|\:\'\"\,\?]" val))
                   (assoc node := (subs val 1))
                   node)]
             (set/rename-keys node {:= :exp}))
        :$ (set/rename-keys node {:$ :vstr})
        :' (set/rename-keys node {:' :str})
        :| (set/rename-keys node {:| :vstr})
        :> (die "Folded scalars not allowed in code mode")
        ,  (die "Scalar has unknown style")))))

(defn resolve-code-alias [node]
  (set/rename-keys node {:* :Ali}))

(defn resolve-code-node
  "Resolve nodes recursively in code mode"
  [node]
  (let [tag (:! node)]
    (if (= tag "")
      (resolve-data-node (dissoc node :!))
      (case (node-type node)
        :map (resolve-code-mapping node)
        :seq (resolve-code-sequence node)
        :val (resolve-code-scalar node)
        :ali (resolve-code-alias node)))))

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
(def re-keyword (re/re #":$symw"))

(defn resolve-plain-scalar [node]
  (let [val (:= node)]
    (when (re-matches re-inf-nan val)
      (die "Inf and NaN not supported in YAMLScript"))
    (condp re-matches val
      re-int :int
      re-float :flt
      re-bool :bln
      re-null :nil
      re-keyword :key
      :str)))

(defn resolve-data-scalar [node]
  (let [style (get-scalar-style node)]
    (case style
      := (set/rename-keys node {:= (resolve-plain-scalar node)})
      :$ (set/rename-keys node {:$ :str})
      :' (set/rename-keys node {:' :str})
      :| (set/rename-keys node {:| :str})
      :> (set/rename-keys node {:> :str})
      ,  (die "Scalar has unknown style: " style))))

(defn resolve-data-alias [node]
  (set/rename-keys node {:* :Ali}))

(defn resolve-data-node
  "Resolve nodes recursively in 'yaml' mode"
  [node]
  (let [tag (:! node)
        anchor (:& node)
        node (if (= tag "")
               (resolve-code-node (dissoc node :!))
               (case (node-type node)
                 :map (resolve-data-mapping node)
                 :seq (resolve-data-sequence node)
                 :val (resolve-data-scalar node)
                 :ali (resolve-data-alias node)))
        node (if anchor (assoc node :& anchor) node)]
    (if (and tag (re-find #":$" tag))
      (assoc node :! tag)
      node)))

(defn resolve-data-node-top [node]
  (if-lets [pairs (or (:% node) (:%% node))
            key (get-in pairs [0 :=])
            _ (= "=>" key)
            [key1 val1 & rest] pairs]
    {:map (concat
            [(resolve-code-node key1)]
            [(resolve-code-node val1)]
            (:map (resolve-data-node {:% rest})))}
    (if-lets [list (or (:- node) (:-- node))
              key (get-in list [0 :% 0 :=])
              _ (= "=>" key)
              [first & rest] list]
      {:seq (cons (resolve-code-mapping first)
              (map resolve-data-node rest))}
      (resolve-data-node node))))


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
  (let [style (some #(when (%1 node) %1) [:= :$ :' :| :>])]
    (case style
      := (set/rename-keys node {:= (resolve-plain-scalar node)})
      :$ (set/rename-keys node {:$ :str})
      :' (set/rename-keys node {:' :str})
      :| (set/rename-keys node {:| :str})
      :> (set/rename-keys node {:> :str})
      ,  (die "Scalar has unknown style: " style))))

(def bare-mode-tags
  ["tag:yaml.org,2002:map"
   "tag:yaml.org,2002:seq"])

(defn resolve-bare-alias [node]
  (set/rename-keys node {:* :ali}))

(defn resolve-bare-node
  "Resolve nodes recursively in 'bare' mode"
  [node]
  (let [tag (:! node)
        anchor (:& node)
        _ (when (and tag (not (some #{tag} bare-mode-tags)))
            (die "Unrecognized tag in bare mode: !" tag))
        node (case (node-type node)
               :map (resolve-bare-mapping node)
               :seq (resolve-bare-sequence node)
               :val (resolve-bare-scalar node)
               :ali (resolve-bare-alias node))]
    (if anchor (assoc node :& anchor) node)))

(comment
  (resolve
    #_{:! "yamlscript/v0", :% [{:= "a"} {:= "b c"}]}
    {:! "yamlscript/v0", := ""}
    #__)
  (set/rename-keys {:> 42} {:> :str})
  )
