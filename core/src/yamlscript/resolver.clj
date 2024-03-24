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
   [yamlscript.re :as re]
   [yamlscript.util :refer [if-lets when-lets]]
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
          (throw (Exception. (str "Unknown yamlscript tag: !" full-tag)))))
      (resolve-bare-node node))))

;; ----------------------------------------------------------------------------
;; Resolve taggers for code mode:
;; ----------------------------------------------------------------------------
(defn tag-str [[key val]]
  (when-lets [str (or (:vstr key) (:str key))
              _ (= "" (:exp val))]
    [{:str str} nil]))

(defn tag-def [[{key :exp} val]]
  (when (re-matches re/defk key)
    [{:def key} val]))

(defn tag-defn [[{key :exp} val]]
  (when (re-matches re/dfnk key)
    [{:defn key} val]))

(defn tag-afn [[{key :exp} val]]
  (when (re-matches re/afnk key)
    [{:fn key} val]))

;; XXX - This needs refactoring to not mutate the key
(defn tag-fn [[key val]]
  (when-lets [key (:exp key)
              old key
              rgx (re/re #"^fn\((.*)\)$")
              key (str/replace key rgx "fn [$1]")
              _ (not= old key)]
    [{:exp key} val]))

(defn tag-forms [[key val]]
  (when-lets [_ (or
                  (re-find #" +%$" (:exp key))
                  (re-matches #"(cond|condp|case)" (:exp key)))
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
       tag-fn
       tag-def
       tag-defn
       tag-afn
       tag-forms
       tag-exp
       identity) pair)))

(defn resolve-code-mapping [node]
  (when (:%% node)
    (throw (Exception. "Flow mappings not allowed in code mode")))
  (let [node
        {:pairs (vec
                  (mapcat
                    (fn [[key val]] (resolve-code-pair key val))
                    (partition 2 (:% node))))}]
    (if-lets [[key val] (:pairs node)
              key-str (:exp key)
              _ (re-matches re/dfnk key-str)]
      {:defn [key val]}
      node)))

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
           (set/rename-keys node {:= :exp}))
      :$ (set/rename-keys node {:$ :vstr})
      :' (set/rename-keys node {:' :str})
      :| (set/rename-keys node {:| :vstr})
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
