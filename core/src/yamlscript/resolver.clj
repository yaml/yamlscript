;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.resolver is responsible for tagging each node in the YAML
;; node tree.
;;
;; The tags used by YS are:
;;
;; YAML 1.2 Core Schema tags ('!!' -> 'tag:yaml.org,2002:'):
;; * !!map - YAML mapping
;; * !!seq - YAML sequence
;; * !!str - YAML string scalar
;; * !!int - YAML integer scalar
;; * !!float - YAML floating point scalar
;; * !!bool - YAML boolean scalar
;; * !!null - YAML null scalar
;;
;; Mode tags:
;; * ! - Toggle between code and data mode
;; * !code - Code mode
;; * !data - Data mode
;; * !bare - Bare mode
;; * !clj - Raw Clojure code
;;
;; Method tags:
;; * !:method - Call method on pair value
;; * !:method* - Apply method on pair value sequence
;; * !:method: - Call method on pair value & toggle mode
;; * !:method*: - Apply method on pair value sequence & toggle mode
;;
;; Data mode node type keys:
;; * :map - YAML mapping
;; * :seq - YAML sequence
;; * :str - YAML string scalar
;; * :int - YAML integer scalar
;; * :flt - YAML floating point scalar
;; * :bln - YAML boolean scalar
;; * :nil - YAML null scalar
;;
;; Code mode (data mode tags, plus):
;; * :xmap - YS expression mapping - each pair creates form
;; * :fmap - YS forms mapping - every node is a form
;; * :cmap - YS code mapping - map where each node is a form
;; * :cseq - YS code sequence - seq where each node is a form
;; * :expr - YS expression scalar
;; * :xstr - YS expression string (w/ interpolation)
;;
;; The resolver transforms the keys of the YS special forms:
;;
;; * :def  - 'foo ='
;; * :defn - 'defn foo(...)'
;; # XXX Maybe :form should be :fexp??
;; * :form - 'foo(...) |' - key is entire form (so is value)
;; * :cmap - 'foo !: <block-map>'
;; * :cseq - 'foo !: <block-seq>'

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
  resolve-data-alias

  resolve-bare-mapping
  resolve-bare-sequence
  resolve-bare-scalar)

(defn resolve-node [node mode]
  (case mode
    :bare (resolve-bare-node node)
    :data (resolve-data-node node)
    :data-top (resolve-data-node-top node)
    :code (resolve-code-node node)))

(defn resolve
  "Walk YAML tree and tag all nodes according to YS rules."
  [node]
  (let [mode (:+ node)
        node (dissoc node :+)]
    (case mode
      "code" (resolve-node node :code)
      "data" (resolve-node node :data-top)
      "bare" (resolve-node node :bare))))


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
(def re-keyword re/keyw)
(def re-call-tag (re/re #"(?::(?:$fsym|$ysym)\*?)+:?"))
#_(re-matches re-call-tag ":bar:zoo")

(comment
  (YSC "
foo: !:inc: 1 + 2
bar:: !:inc 3 + 4
")
  )

(defn check-double-colon-with-tag [key val]
  (let [key-text (:= key)
        val-tag (:! val)
        [key val] (if (and
                        key-text val-tag
                        (re-find #":$" key-text)
                        (re-find #"^:\w" val-tag))
                    (if (re-find #":$" val-tag)
                      (die "Tag '!" val-tag
                        "' can't end with ':' after '::' key")
                      [(assoc key := (str/replace key-text #"\s*:$" ""))
                       (assoc val :! (str val-tag ":"))])
                    [key val])]
    [key val]))

(defn check-mode-swap [key val]
  (let [[key val] (check-double-colon-with-tag key val)
        key-text (:= key)]
    (if (and key-text (re-find #":$" key-text))
      (if (re-find #"::$" key-text)
        (let [key (assoc key := (str/replace key-text #"\s*::$" ""))]
          [key val])
        (if (:! val)
          (die "Can't specify tag on value of '::' pair")
          (let [key (assoc key := (str/replace key-text #"\s*:$" ""))
                val (assoc val :! "")]
            [key val])))
      [key val])))

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
      (die "Inf and NaN not supported in YS"))
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
;; Taggers for code mode pairs:
;; ----------------------------------------------------------------------------
(defn tag-str [[key val]]
  (when-lets [str (or (:xstr key) (:str key))
              _ (= "" (:expr val))]
    [{:str str} nil]))

(defn tag-fn [[{key :expr} val]]
  (when (and key (re-matches re/afnk key))
    [{:fn key} val]))

(defn tag-def [[{key :expr} val]]
  (when (and key (re-matches re/defk key))
    [{:def key} val]))

(defn tag-defn [[{key :expr} val]]
  (when (and key (re-matches re/dfnk key))
    [{:defn key} val]))

(defn tag-fmap [[key val]]
  (when-lets [key-str (:expr key)
              _ (or
                  (re-find #" +%$" key-str)
                  (re-matches #"(cond|cond[fp] .+|case .+)" key-str))
              _ (contains? val :xmap)
              key (assoc key :expr (str/replace key-str #" +%$" ""))
              val (set/rename-keys val {:xmap :fmap})]
    [key val]))

(defn tag-form [[key val]]
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
  (let [; assert key is scalar
        [key val] (check-mode-swap key val)
        pair [(resolve-code-node key)
              (resolve-code-node val)]]
    ((some-fn
       tag-str
       tag-fn
       tag-def
       tag-defn
       tag-fmap
       tag-form
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

(def esc1 #"^\+\ *[\`\!\@\#\%\&\*\-\{\[\|\:\'\"\,\?\>]")
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
      (some #{"" "data"} [tag])
      (case kind
        :map (resolve-data-mapping node)
        :seq (resolve-data-sequence node)
        :val (resolve-data-node node)
        :ali (resolve-data-alias node))
      ,
      (= "bare" tag)
      (case kind
        :map (resolve-bare-mapping node)
        :seq (resolve-bare-sequence node)
        :val (resolve-bare-node node))
      ,
      (and tag (re-matches re-call-tag tag))
      (if (str/ends-with? tag ":")
        (assoc (resolve-data-node node) :! (subs tag 0 (dec (count tag))))
        (assoc (resolve-code-node node) :! tag))

      (and (= tag "clj") (= :val kind))
      (resolve-code-scalar node :clj style)
      ,
      (re-find #"^tag:yaml.org,2002:" tag)
      (case kind
        :map (if (= tag "tag:yaml.org,2002:map")
               (resolve-bare-mapping node)
               (die "Invalid tag for code mode mapping: " (tagp tag)))
        :seq (if (= tag "tag:yaml.org,2002:seq")
               (resolve-bare-sequence node)
               (die "Invalid tag for code mode sequence: " (tagp tag)))
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
               (die "Invalid tag for code mode scalar: " (tagp tag))
               "tag:yaml.org,2002:seq"
               (die "Invalid tag for code mode scalar: " (tagp tag))))
      ,
      :else (die "Invalid tag for code mode node: " (tagp tag)))))

;; ----------------------------------------------------------------------------
;; Dispatchers for data mode:
;; ----------------------------------------------------------------------------
(defn resolve-data-mapping [node]
  (let [nodes (or (:% node) (:%% node))
        merge (some #(re-matches #"<<\s*:?" %1)
                (remove nil?  (map := (keys (apply hash-map nodes)))))
        mapping
        {:map
         (vec (mapcat
                (fn [[key val]]
                  (let [okey key
                        [key val] (check-mode-swap key val)
                        key-str (:= key)
                        [key val]
                        (cond
                          (and key-str (re-matches re/defk key-str))
                          [{:def key-str} (resolve-code-node val)]
                          (and key-str (re-matches #":.*[^-\w].*" key-str))
                          [(resolve-code-node key)
                           (if (str/ends-with? (:= okey) ":")
                             (resolve-data-node (dissoc val :!))
                             (resolve-code-node val))]
                          :else
                          [(resolve-data-node key) (resolve-data-node val)])
                        key (if (and key-str (= key-str "<<"))
                              {:key ":-<<"}
                              key)]
                    [key val]))
                (partition 2 nodes)))}
        mapping (if-let [anchor (:& node)] (assoc mapping :& anchor) mapping)
        mapping (if merge (assoc mapping :! ":+merge") mapping)]
    mapping))

(defn resolve-data-sequence [node]
  (let [sequence
        {:seq (map resolve-data-node
                (or (:- node) (:-- node)))}]
    (if-let [anchor (:& node)]
      (assoc sequence :& anchor)
      sequence)))

(defn resolve-data-scalar [node type style]
  (set/rename-keys node {style type}))

(defn resolve-data-alias [node]
  (set/rename-keys node {:* :Ali}))

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
      (some #{"" "code"} [tag])
      (case kind
        :map (resolve-code-mapping node)
        :seq (resolve-code-sequence node)
        :val (resolve-code-node node)
        :ali (resolve-code-alias node))
      ,
      (and tag (re-matches re-call-tag tag))
      (if (str/ends-with? tag ":")
        (assoc (resolve-code-node node) :! (subs tag 0 (dec (count tag))))
        (assoc (resolve-data-node node) :! tag))
      ,
      (= "bare" tag)
      (case kind
        :map (resolve-bare-mapping node)
        :seq (resolve-bare-sequence node)
        :val (resolve-bare-node node))
      ,
      (re-find #"^tag:yaml.org,2002:" tag)
      (case kind
        :map (if (= tag "tag:yaml.org,2002:map")
               (resolve-bare-mapping node)
               (die "Invalid tag for data mode mapping: " (tagp tag)))
        :seq (if (= tag "tag:yaml.org,2002:seq")
               (resolve-bare-sequence node)
               (die "Invalid tag for data mode sequence: " (tagp tag)))
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
               (die "Invalid tag for data mode scalar: " (tagp tag))))
      ,
      :else (die "Invalid tag for data mode node: " (tagp tag)))))

;; XXX Replace this with assignment in data mode
(defn resolve-data-node-top [node]
  (if-lets [xmap (or (:% node) (:%% node))
            key-str (get-in xmap [0 :=])
            _ (some #{":" "=>"} [key-str])
            [_ val & rest] xmap
            key {:= "=>"}]
    {:map (vec (concat
                 [(resolve-code-node key)]
                 [(resolve-code-node val)]
                 (:map (resolve-data-node {:% rest}))))}
    (if-lets [list (or (:- node) (:-- node))
              key-str (get-in list [0 :% 0 :=])
              _ (= "=>" key-str)
              [first & rest] list]
      {:seq (cons (resolve-code-mapping first)
              (map resolve-data-node rest))}
      (resolve-data-node node))))


;; ----------------------------------------------------------------------------
;; Dispatchers for bare mode:
;; ----------------------------------------------------------------------------
(defn resolve-bare-mapping [node]
  (let [nodes (or (:% node) (:%% node))
        nodes2 (map-indexed
                 (fn [index item]
                   (if (and (even? index)
                         (= item {:= "<<"}))
                     {:key ":-<<"}
                     item))
                 nodes)
        merge (not= nodes nodes2)
        mapping {:map (vec (map resolve-bare-node nodes2))}
        mapping (if-let [anchor (:& node)]
                  (assoc mapping :& anchor)
                  mapping)
        mapping (if merge (assoc mapping :! ":+merge") mapping)]
    mapping))

(defn resolve-bare-sequence [node]
  (let [sequence
        {:seq (map resolve-bare-node
                (or (:- node) (:-- node)))}]
    (if-let [anchor (:& node)]
      (assoc sequence :& anchor)
      sequence)))

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
               (die "Invalid tag for bare mode scalar: " (tagp tag))))
      ,
      :else (die "Invalid tag for bare mode node: " (tagp tag)))))

(comment
  )
