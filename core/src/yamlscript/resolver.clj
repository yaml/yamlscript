;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.resolver is responsible for tagging each node in the YAML
;; node tree.
;;
;; The tags used by YAMLScript are:
;;
;; Bare mode:
;; * !!map - YAML mapping
;; * !!seq - YAML sequence
;; * !!str - YAML string scalar
;; * !!int - YAML integer scalar
;; * !!float - YAML floating point scalar
;; * !!bool - YAML boolean scalar
;; * !!null - YAML null scalar
;;
;; Data mode (bare mode tags, plus):
;; * !map - YAML mapping
;; * !seq - YAML sequence
;; * !str - YAML string scalar
;; * !int - YAML integer scalar
;; * !flt - YAML floating point scalar
;; * !bln - YAML boolean scalar
;; * !nil - YAML null scalar
;;
;; Code mode (data mode tags, plus):
;; * !xmap - YAMLScript expression mapping - pair creates form
;; * !fmap - YAMLScript forms mapping - lhs and rhs create separate forms
;; * !cmap - YAMLScript code mapping - each node is a form
;; * !cseq - YAMLScript code sequence - each node is a form
;; * !expr - YAMLScript expression scalar
;; * !xstr - YAMLScript expression string (w/ interpolation)
;;
;; * !empty - YAML empty stream
;;
;; The resolver transforms the keys of the YAMLScript special forms:
;;
;; * def  - 'foo =' -> !expr 'def foo'
;; * defn - 'defn foo(...)' -> !expr 'defn foo [...]'
;; * form - 'foo(...) |' -> key is entire form (so is value)
;; * cmap - 'foo !: <block-map>' -> foo: !cmap <block-map>
;; * cseq - 'foo !: <block-seq>' -> foo: !cseq <block-seq>

(ns yamlscript.resolver
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [yamlscript.common]
   [yamlscript.re :as re])
  (:refer-clojure :exclude [resolve]))

(declare
  resolve-code-node
  resolve-data-node
  resolve-data-node-top
  resolve-bare-node)

;; TODO:
;; * If !yamlscript/v0 is on first document, we can start other docs with short
;;   tags like !code, !data, !bare !xmap
;; * !yamlscript/v0/xxxx can use any valid !xxxx tag for xxxx
;; * Support function call tags at top level: !yamlscript/v0/data/merge*:

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


;; ----------------------------------------------------------------------------
;; Generic helpers:
;; ----------------------------------------------------------------------------
(defn node-kind [node]
  (condf node
    :%  :map
    :%% :map
    :-  :seq
    :-- :seq
    :*  :ali
        :val))

(defn scalar-style [node]
  (some #(when (%1 node) %1) [:= :$ :' :| :>]))

(def re-int #"(?:[-+]?[0-9]+|0o[0-7]+|0x[0-9a-fA-F]+)")
(def re-float #"[-+]?(\.[0-9]+|[0-9]+(\.[0-9]*)?)([eE][-+]?[0-9]+)?")
(def re-bool #"(?:true|True|TRUE|false|False|FALSE)")
(def re-null #"(?:|~|null|Null|NULL)")
(def re-inf-nan #"(?:[-+]?(?:\.inf|\.Inf|\.INF)|\.nan|\.NaN|\.NAN)")
(def re-keyword (re/re #":$symw"))

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


;; ----------------------------------------------------------------------------
;; Resolve taggers for code mode:
;; ----------------------------------------------------------------------------
(defn tag-str [[key val]]
  (when-lets [str (or (:xstr key) (:str key))
              _ (= "" (:expr val))]
    [{:str str} nil]))

(defn tag-fn [[{key :expr} val]]
  (when (re-matches re/afnk key)
    [{:fn key} val]))

(defn tag-def [[{key :expr} val]]
  (when (re-matches re/defk key)
    [{:def key} val]))

(defn tag-defn [[{key :expr} val]]
  (when (re-matches re/dfnk key)
    [{:defn key} val]))

(defn tag-fmap [[key val]]
  (when-lets [_ (or
                  (re-find #" +%$" (:expr key))
                  (re-matches #"(cond|condp .+|case .+)" (:expr key)))
              _ (contains? val :xmap)
              key (assoc key :expr (str/replace (:expr key) #" +%$" ""))
              val (set/rename-keys val {:xmap :fmap})]
    [key val]))

(defn tag-expr [[key val]]
  (when-lets [_ (contains? key :expr)
              _ (some val [:expr :str :xstr :xmap])]
    (let [key (if (re-find #" +\|$" (:expr key))
                {:form (assoc key :expr (str/replace (:expr key) #" +\|$" ""))}
                key)]
      [key val])))

(defn tag-error [[key val]]
  (die "Don't know how to tag pair" [key val]))

;; ----------------------------------------------------------------------------
;; Resolve dispatchers for code mode:
;; ----------------------------------------------------------------------------
(defn resolve-code-pair [key val]
  (let [[key val] (resolve-mode-swap key val)
        pair [(resolve-code-node key)
              (resolve-code-node val)]]
    ((some-fn
       tag-str
       tag-fn
       tag-def
       tag-defn
       tag-fmap
       tag-expr
       identity) pair)))

(defn resolve-code-mapping [node]
  (when (:%% node)
    (die "Flow mappings not allowed in code mode"))
  (let [anchor (:& node)
        node {:xmap (vec
                       (mapcat
                         (fn [[key val]] (resolve-code-pair key val))
                         (partition 2 (:% node))))}
        node (if anchor (assoc node :& anchor) node)]
    (if-lets [[key val] (:xmap node)
              key-str (:expr key)
              _ (re-matches re/dfnk key-str)]
      {:defn [key val]}
      node)))

(defn resolve-code-sequence [_]
  (die "Sequences (block and flow) not allowed in code mode"))

(defn resolve-code-scalar [node]
  (let [style (scalar-style node)
        val (style node)]
    (case style
      := (let [node
               ;; Remove leading escape character from value
               (if
                (or
                  (re-find #"^[\.\+][\`\!\@\#\%\&\*\-\{\[\|\:\'\"\,\>\?]" val)
                  (re-find #"^-[\`\!\@\#\%\&\*\-\{\[\|\:\'\"\,\?]" val))
                 (assoc node := (subs val 1))
                 node)]
           (set/rename-keys node {:= :expr}))
      :$ (set/rename-keys node {:$ :xstr})
      :' (set/rename-keys node {:' :str})
      :| (set/rename-keys node {:| :xstr})
      :> (die "Folded scalars not allowed in code mode")
      ,  (die "Scalar has unknown style"))))

(defn resolve-code-alias [node]
  (set/rename-keys node {:* :Ali}))

(defn resolve-code-node
  "Resolve nodes recursively in code mode"
  [node]
  (let [tag (:! node)
        node (dissoc node :!)]
    (if (= tag "")
      (resolve-data-node node)
      (let [kind (node-kind node)]
        (condp = [kind tag]
          [:map nil] (resolve-code-mapping node)
          [:seq nil] (resolve-code-sequence node)
          [:val nil] (resolve-code-scalar node)
          [:ali nil] (resolve-code-alias node)
          [:val "clj"] (let [style (scalar-style node)]
                         (set/rename-keys node {style (keyword tag)}))
          (die "Unknown tag in code mode: '!" tag "'"))))))


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
  (let [style (scalar-style node)]
    (case style
      := (set/rename-keys node {:= (resolve-plain-scalar node)})
      :$ (set/rename-keys node {:$ :str})
      :' (set/rename-keys node {:' :str})
      :| (set/rename-keys node {:| :str})
      :> (set/rename-keys node {:> :str})
      ,  (die "Scalar has unknown style: " style))))

(defn resolve-data-alias [node]
  (set/rename-keys node {:* :Ali}))

(defn resolve-data-node-top [node]
  (if-lets [xmap (or (:% node) (:%% node))
            key (get-in xmap [0 :=])
            _ (= "=>" key)
            [key1 val1 & rest] xmap]
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

(defn resolve-data-node
  "Resolve nodes recursively in 'yaml' mode"
  [node]
  (let [tag (:! node)
        anchor (:& node)
        node (if (= tag "")
               (resolve-code-node (dissoc node :!))
               (case (node-kind node)
                 :map (resolve-data-mapping node)
                 :seq (resolve-data-sequence node)
                 :val (resolve-data-scalar node)
                 :ali (resolve-data-alias node)))
        node (if anchor (assoc node :& anchor) node)]
    (if (and tag (re-find #":$" tag))
      (assoc node :! tag)
      node)))


;; ----------------------------------------------------------------------------
;; Resolve dispatchers for bare mode:
;; ----------------------------------------------------------------------------
(def bare-mode-tag-map
  {"tag:yaml.org,2002:map" :map
   "tag:yaml.org,2002:seq" :seq
   "tag:yaml.org,2002:str" :str
   "tag:yaml.org,2002:int" :int
   "tag:yaml.org,2002:float" :flt
   "tag:yaml.org,2002:bool" :bln
   "tag:yaml.org,2002:null" :nil})

(defn resolve-bare-mapping [node]
  (let [tag (:! node)
        _ (when (and tag (not= tag "tag:yaml.org,2002:map"))
            (die "Invalid tag for (bare-mode) mapping: '" tag "'"))
        node (dissoc node :!)]
    {:map (vec (map resolve-bare-node
                 (or (:% node) (:%% node))))}))

(defn resolve-bare-sequence [node]
  (let [tag (:! node)
        _ (when (and tag (not= tag "tag:yaml.org,2002:seq"))
            (die "Invalid tag for (bare-mode) sequence: '" tag "'"))]
    {:seq (map resolve-bare-node
            (or (:- node) (:-- node)))}))

(defn resolve-bare-scalar [node]
  (let [tag (:! node)
        style (some #(when (%1 node) %1) [:= :$ :' :| :>])
        value (get node style)
        type
        (when tag
          (case (bare-mode-tag-map tag)
            :str :str
            :int (if (re-matches re-int value) :int
                     (die "Invalid value for (bare-mode) !!int: '" value "'"))
            :flt (if (re-matches re-float value) :flt
                     (die "Invalid value for (bare-mode) !!float: '" value "'"))
            :bln (if (re-matches re-bool value) :bln
                     (die "Invalid value for (bare-mode) !!bool: '" value "'"))
            :nil (if (re-matches re-null value) :nil
                     (die "Invalid value for (bare-mode) !!null: '" value "'"))
            :map (die "Invalid tag for (bare-mode) scalar: '" tag "'")
            :seq (die "Invalid tag for (bare-mode) scalar: '" tag "'")
            (XXX "node" node "tag" tag "style" style "value" value)))]
    (if type
      {type value}
      (case style
        := (set/rename-keys node {:= (resolve-plain-scalar node)})
        :$ (set/rename-keys node {:$ :str})
        :' (set/rename-keys node {:' :str})
        :| (set/rename-keys node {:| :str})
        :> (set/rename-keys node {:> :str})
        ,  (die "Scalar has unknown style: " style)))))

(defn resolve-bare-alias [node]
  (set/rename-keys node {:* :ali}))

(defn resolve-bare-node
  "Resolve nodes recursively in 'bare' mode"
  [node]
  (let [tag (:! node)
        _ (when (and tag (not (get bare-mode-tag-map tag)))
            (die "Unrecognized tag in bare mode: !" tag))]
    (case (node-kind node)
      :map (resolve-bare-mapping node)
      :seq (resolve-bare-sequence node)
      :val (resolve-bare-scalar node)
      :ali (resolve-bare-alias node))))

(comment
  )
