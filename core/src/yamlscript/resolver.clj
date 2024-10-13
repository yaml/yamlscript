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
;; The resolver transforms the keys of the YAMLScript special forms:
;;
;; * def  - 'foo =' -> !expr 'def foo'
;; * defn - 'defn foo(...)' -> !expr 'defn foo [...]'
;; # XXX Maybe form should be fexp??
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
  resolve-bare-node

  resolve-data-mapping
  resolve-data-sequence
  resolve-data-scalar
  resolve-data-alias)

;; TODO:
;; * If !yamlscript/v0 is on first document, we can start other docs with short
;;   tags like !code, !data, !bare !xmap
;; * !yamlscript/v0;xxxx can use any valid !xxxx tag for xxxx
;; * Support function call tags at top level: !yamlscript/v0/data;merge*:

(defn resolve-node [node mode]
  (case mode
    :bare (resolve-bare-node node)
    :data (resolve-data-node node)
    :data-top (resolve-data-node-top node)
    :code (resolve-code-node node)))

(defn resolve
  "Walk YAML tree and tag all nodes according to YAMLScript rules."
  [node]
  (let [tag (:! node)
        node (dissoc node :!)]
    (if (and tag (re-find #"^yamlscript/v0" tag))
      (let [full-tag tag
            tag (subs tag (count "yamlscript/v0"))]
        (case tag
          "" (resolve-node node :code)
          "/" (resolve-node node :data-top)
          "/code" (resolve-node node :code)
          "/data" (resolve-node node :data-top)
          "/bare" (resolve-node node :bare)
          (die "Unknown yamlscript tag: !" full-tag)))
      (resolve-node node :bare))))


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

(def re-int #"(?:[-+]?[0-9]+|0o[0-7]+|0x[0-9a-fA-F]+)")
(def re-float #"[-+]?(\.[0-9]+|[0-9]+(\.[0-9]*)?)([eE][-+]?[0-9]+)?")
(def re-bool #"(?:true|True|TRUE|false|False|FALSE)")
(def re-null #"(?:|~|null|Null|NULL)")
(def re-inf-nan #"(?:[-+]?(?:\.inf|\.Inf|\.INF)|\.nan|\.NaN|\.NAN)")
(def re-keyword (re/re #":$symw"))

(defn check-mode-swap [key val]
  (let [key-text (:= key)
        [key val] (if (and key-text (re-find #":$" key-text))
                    (let [key (assoc key :=
                                (str/replace key-text #"\s*:$" ""))
                          val (assoc val :! "")]
                      [key val])
                    [key val])]
    [key val]))

(defn check-yaml-core-tag [tag value]
  (case tag
    :str :str
    :int (if (re-matches re-int value) :int :ERR)
    :flt (if (re-matches re-float value) :flt :ERR)
    :bln (if (re-matches re-bool value) :bln :ERR)
    :nil (if (re-matches re-null value) :nil :ERR)
    :map :map
    :seq :seq
    nil))

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

(defn tagp [tag]
  (when tag
    (str "!" (str/replace tag #"^tag:yaml.org,2002:" "!"))))


;; ----------------------------------------------------------------------------
;; Taggers for code mode:
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


;; ----------------------------------------------------------------------------
;; Dispatchers for code mode:
;; ----------------------------------------------------------------------------
(defn resolve-code-pair [key val]
  (let [[key val] (check-mode-swap key val)
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

(comment
  (YSC "!yamlscript/v0
say: !fmap
  cond:
    true: 'yes'
    false: 'no'
"))

(comment
  (YSC "
!yamlscript/v0/code
a =: b.c()
"))


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

(def esc1 #"^[\.\+][\`\!\@\#\%\&\*\-\{\[\|\:\'\"\,\>\?]")
(def esc2 #"^-[\`\!\@\#\%\&\*\-\{\[\|\:\'\"\,\?]")
(defn resolve-code-scalar [node type style]
  (if type
    (set/rename-keys node {style type})
    (let [val (style node)]
      (case style
        := (let [node  ;; Remove leading escape character from value
                 (if (or (re-find esc1 val) (re-find esc2 val))
                   (assoc node := (subs val 1))
                   node)]
             (set/rename-keys node {style :expr}))
        :$ (set/rename-keys node {style :xstr})
        :' (set/rename-keys node {style :str})
        :| (set/rename-keys node {style :xstr})
        :> (die "Folded scalars not allowed in code mode")
        ,  (die "Scalar has unknown style")))))

(defn resolve-code-alias [node]
  (set/rename-keys node {:* :Ali}))

#_(def code-form-tags
  {"xmap" :xmap
   "fmap" :fmap
   "cmap" :cmap
   "cseq" :cseq
   "expr" :expr
   "xstr" :xstr})

(defn resolve-code-node
  "Resolve nodes recursively in 'yaml' mode"
  [node]
  (let [kind (node-kind node)
        tag (:! node)
        style (some #(when (%1 node) %1) [:= :$ :' :| :>])
        value (get node style)
        node (dissoc node :!)
        check (fn [type value]
                (let [type (check-yaml-core-tag type value)]
                  (if (= type :ERR)
                    (die "Invalid value for code mode scalar with tag "
                      (tagp tag) ": '" value "'")
                    type)))]

    (cond
      (nil? tag)
      (case kind
        :val (resolve-code-scalar node nil style)
        :map (resolve-code-mapping node)
        :seq (resolve-code-sequence node)
        :ali (resolve-code-alias node))
      ,
      (= tag "")
      (case kind
        :map (resolve-data-mapping node)
        :seq (resolve-data-sequence node)
        :val (resolve-data-node node)
        :ali (resolve-data-alias node))
      ,
      (and (= tag "clj") (= :val kind))
      (resolve-code-scalar node :clj style)
      ,
      (re-find #"^tag:yaml.org,2002:" tag)
      (case kind
        :map (if (= tag "tag:yaml.org,2002:map")
               (resolve-code-mapping node)
               (die "Invalid tag for code mode mapping: " tag))
        :seq (if (= tag "tag:yaml.org,2002:seq")
               (resolve-code-sequence node)
               (die "Invalid tag for code mode sequence: " tag))
        :val (case tag
               "tag:yaml.org,2002:str"
               (resolve-code-scalar node :str style)
               "tag:yaml.org,2002:int"
               (resolve-code-scalar node (check :int value) style)
               "tag:yaml.org,2002:float"
               (resolve-code-scalar node (check :flt value) style)
               "tag:yaml.org,2002:bool"
               (resolve-code-scalar node (check :bln value) style)
               "tag:yaml.org,2002:null"
               (resolve-code-scalar node (check :nil value) style)
               "tag:yaml.org,2002:map"
               (die "Invalid tag for code mode scalar: " tag)
               "tag:yaml.org,2002:seq"
               (die "Invalid tag for code mode scalar: " tag)))
      ,
      :else
      (if (and tag (re-find #":$" tag))
        (assoc (resolve-code-node node) :! tag)
        (let [tag (tagp tag)]
          (die
            (condp = kind
              :map (str "Invalid tag for code mode mapping: " tag)
              :seq (str "Invalid tag for code mode sequence: " tag)
              :val (str "Invalid tag for code mode scalar: " tag)
              :ali (die "Can't tag an alias"))))))))


;; ----------------------------------------------------------------------------
;; Dispatchers for data mode:
;; ----------------------------------------------------------------------------
(defn resolve-data-mapping [node]
  {:map (vec
          (mapcat
            (fn [[key val]]
              (let [[key val] (check-mode-swap key val)]
                [(resolve-data-node key)
                 (resolve-data-node val)]))
            (partition 2 (or (:% node) (:%% node)))))})

(defn resolve-data-sequence [node]
  {:seq (map resolve-data-node
          (or (:- node) (:-- node)))})

(defn resolve-data-scalar [node type style]
  (set/rename-keys node {style type}))

(defn resolve-data-alias [node]
  (set/rename-keys node {:* :Ali}))

#_(def data-type-tags
  {"map" :map
   "seq" :seq
   "set" :set
   "str" :str
   "chr" :chr
   "sym" :sym
   "key" :key
   "rgx" :rgx
   "int" :int
   "float" :flt
   "bool" :bln
   "null" :nil
   "nil" :nil})

(defn resolve-data-node
  "Resolve nodes recursively in 'yaml' mode"
  [node]
  (let [kind (node-kind node)
        tag (:! node)
        style (some #(when (%1 node) %1) [:= :$ :' :| :>])
        value (get node style)
        node (dissoc node :!)
        check (fn [type value]
                (let [type (check-yaml-core-tag type value)]
                  (if (= type :ERR)
                    (die "Invalid value for data mode scalar with tag "
                      (tagp tag) ": '" value "'")
                    type)))]
    (cond
      (nil? tag)
      (case kind
        :val (resolve-data-scalar node
               (if (= style :=) (resolve-plain-scalar node) :str)
               style)
        :map (resolve-data-mapping node)
        :seq (resolve-data-sequence node)
        :ali (resolve-data-alias node))
      ,
      (= tag "")
      (case kind
        :map (resolve-code-mapping node)
        :seq (resolve-code-sequence node)
        :val (resolve-code-node node)
        :ali (resolve-data-alias node))
      ,
      (re-find #"^tag:yaml.org,2002:" tag)
      (case kind
        :map (if (= kind :map)
               (resolve-data-mapping node)
               (die "Invalid tag for data mode non-mapping: !!map"))
        :seq (if (= kind :seq)
               (resolve-data-sequence node)
               (die "Invalid tag for data mode non-sequence: !!seq"))
        :val (case tag
               "tag:yaml.org,2002:str"
               (resolve-data-scalar node :str style)
                "tag:yaml.org,2002:int"
                (resolve-data-scalar node (check :int value) style)
                "tag:yaml.org,2002:float"
                (resolve-data-scalar node (check :flt value) style)
                "tag:yaml.org,2002:bool"
                (resolve-data-scalar node (check :bln value) style)
                "tag:yaml.org,2002:null"
                (resolve-data-scalar node (check :nil value) style)
                "tag:yaml.org,2002:map"
                (die "Invalid tag for data mode scalar: " tag)
                "tag:yaml.org,2002:seq"
                (die "Invalid tag for data mode scalar: " tag)))
      :else
      (if (and tag (re-find #":$" tag))
        (assoc (resolve-data-node node) :! tag)
        (let [tag (tagp tag)]
          (die
            (condp = kind
              :map (str "Invalid tag for data mode mapping: " tag)
              :seq (str "Invalid tag for data mode sequence: " tag)
              :val (str "Invalid tag for data mode scalar: " tag)
              :ali (die "Can't tag an alias"))))))))

;; XXX Replace this with assignment in data mode
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


;; ----------------------------------------------------------------------------
;; Dispatchers for bare mode:
;; ----------------------------------------------------------------------------
(defn resolve-bare-mapping [node]
  {:map (vec (map resolve-bare-node
               (or (:% node) (:%% node))))})

(defn resolve-bare-sequence [node]
  {:seq (map resolve-bare-node
          (or (:- node) (:-- node)))})

(defn resolve-bare-scalar [node type style]
  (set/rename-keys node {style type}))

(defn resolve-bare-alias [node]
  (set/rename-keys node {:* :ali}))

(defn resolve-bare-node
  "Resolve nodes recursively in 'bare' mode"
  [node]
  (let [kind (node-kind node)
        tag (:! node)
        style (some #(when (%1 node) %1) [:= :$ :' :| :>])
        value (get node style)
        node (dissoc node :!)
        check (fn [type value]
                (let [type (check-yaml-core-tag type value)]
                  (if (= type :ERR)
                    (die "Invalid value for bare mode scalar with tag "
                      (tagp tag) ": '" value "'")
                    type)))]
    (cond
      (nil? tag)
      (case kind
        :val (resolve-bare-scalar node
               (if (= style :=) (resolve-plain-scalar node) :str)
               style)
        :map (resolve-bare-mapping node)
        :seq (resolve-bare-sequence node)
        :ali (resolve-bare-alias node))
      ,
      (re-find #"^tag:yaml.org,2002:" tag)
      (case kind
        :map (if (= tag "tag:yaml.org,2002:map")
               (resolve-bare-mapping node)
               (die "Invalid tag for bare mode mapping: " (tagp tag)))
        :seq (if (= tag "tag:yaml.org,2002:seq")
               (resolve-bare-sequence node)
               (die "Invalid tag for bare mode sequence: " (tagp tag)))
        :val (case tag
               "tag:yaml.org,2002:str"
               (resolve-bare-scalar node :str style)
               "tag:yaml.org,2002:int"
               (resolve-bare-scalar node (check :int value) style)
               "tag:yaml.org,2002:float"
               (resolve-bare-scalar node (check :flt value) style)
               "tag:yaml.org,2002:bool"
               (resolve-bare-scalar node (check :bln value) style)
               "tag:yaml.org,2002:null"
               (resolve-bare-scalar node (check :nil value) style)
               "tag:yaml.org,2002:map"
               (die "Invalid tag for bare mode scalar: " (tagp tag))
               "tag:yaml.org,2002:seq"
               (die "Invalid tag for bare mode scalar: " (tagp tag))))
        ,
        :else
        (let [tag (tagp tag)]
          (die
            (condp = kind
              :map (str "Invalid tag for bare mode mapping: " tag)
              :seq (str "Invalid tag for bare mode sequence: " tag)
              :val (str "Invalid tag for bare mode scalar: " tag)))))))

(comment
  )
