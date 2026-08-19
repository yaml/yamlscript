;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.transformer is responsible for transforming the YS AST
;; according to certain special rules.

(ns yamlscript.transformer
  (:require
   [yamlscript.ast :refer [Key Lst Sym QSym Vec]]
   [ys.v0.common]
   [yamlscript.transformers]
   [ys.v0.dwim])
  (:refer-clojure))

(declare
  transform-node
  transform-node-top)

(defn transform
  "Transform special rules for YS AST."
  [node]
  (transform-node-top node))

(def transformers-ns (the-ns 'yamlscript.transformers))

(def plus-fn?
  (-> 'ys.v0.dwim
    ns-publics
    keys
    (->> (map str)
      (map #(subs %1 1))
      (map symbol))
    set))

(def topic (Sym '_))

(defn dot-list
  "Apply a dotted list call to its current dot-chain context."
  [ctx node]
  (let [[func & args] (:Lst node)
        topics (filter #{topic} args)
        [func ctx args]
        (case (count topics)
          0 (let [+func (if-lets
                          [sym (get-in func [:Sym])
                           _ (> (count args) 0)
                           _ (plus-fn? sym)
                           sym (symbol (str "+" sym))]
                          (update-in func [:Sym] (constantly sym))
                          func)]
              [+func ctx args])
          1 [func nil (map (fn [arg] (if (= topic arg) ctx arg)) args)]
          (die "XXX support for multiple topics not yet implemented"))
        [func ctx args] (if (= args '({:Sym *}))
                          [{:Sym 'apply} func [ctx]]
                          [func ctx args])]
    (Lst (concat [func ctx] (vec args)))))

(defn transform-dot
  "Rewrite a parsed dot chain into nested function and lookup calls."
  [node]
  (let
   [lst (:dot node)
    form (reduce
           (fn [ctx node]
             (let
              [node (transform-node node)
               ctx
               (if ctx
                 (condf node
                   :Int (Lst [(Sym 'get+) ctx node])
                   :Str (Lst [(Sym 'get) ctx node])
                   :QSym (Lst [(Sym 'get+) ctx node])
                   :Sym (Lst [(Sym 'get+) ctx node])
                   :Key (Lst [node ctx])
                   :Lst (dot-list ctx node)
                   ctx)
                 node)]
               ctx))
           nil lst)]
    form))

(defn dot-rhs
  "Build the right-hand side of a dot assignment or dot pair."
  [rhs form]
  (let [rhs (if-lets [dots (:dot rhs)
                      [dot1 & dots] dots
                      dot1 (if-let [sym (:Sym dot1)]
                             (QSym sym)
                             dot1)]
              (apply vector form dot1 dots)
              (if (:Sym rhs)
                [form (QSym (:Sym rhs))]
                [form rhs]))]
    (transform-dot {:dot rhs})))

(defn- assignment-path-step
  "Convert one dotted target segment into a runtime path descriptor."
  [node]
  (condf node
    :QSym (Vec [(Key "bare") node])
    :Lst (let [ctx (Sym (gensym "container__"))
               call (dot-list ctx (transform-node node))]
           (Vec [(Key "call")
                 (Lst [(Sym 'fn) (Vec [ctx]) call])]))
    (Vec [(Key "value") (transform-node node)])))

(defn- transform-assignment-target
  "Normalize one plain or dotted assignment target."
  [target]
  (if-let [dots (:dot target)]
    (let [[root & path] dots
          _ (when-not (:Sym root)
              (die "Dotted assignment root must be a symbol"))
          _ (when-not (seq path)
              (die "Dotted assignment requires a path"))]
      {:root root
       :steps (Vec (mapv assignment-path-step path))})
    (if (:Sym target)
      {:root target}
      (die "Mixed assignment targets must be symbols or dotted paths"))))

(defn transform-assign
  "Normalize parsed dotted-assignment targets and their condition."
  [node]
  (let [assign (:Assign node)]
    (if (every? :root (:targets assign))
      node
      {:Assign
       (-> assign
         (update :targets #(mapv transform-assignment-target %1))
         (update :condition #(when %1 (transform-node %1))))})))

(defn- dot-assignment-updater
  "Build a unary updater for one `.=` right-hand-side form."
  [rhs]
  (let [old (Sym (gensym "value__"))
        body (if rhs (dot-rhs rhs old) {:Nil nil})]
    (Lst [(Sym 'fn) (Vec [old]) body])))

(defn- finalize-assignment
  "Attach `.=` updater functions after both pair sides are transformed."
  [lhs rhs]
  (if-lets [assign (:Assign (second lhs))
            _ (= '. (get-in assign [:operator :Sym]))
            targets (:targets assign)
            forms (if (= 1 (count targets))
                    [rhs]
                    (when (:Vec rhs) (:Vec rhs)))
            _ (if forms
                true
                (die "Multi-target '.=' requires positional RHS forms"))
            updaters (mapv dot-assignment-updater
                       (take (count targets) (concat forms (repeat nil))))]
    [(assoc-in lhs [1 :Assign :updaters] (Vec updaters)) rhs]
    [lhs rhs]))

(defn adjust-dot-def
  "Rewrite dot assignment syntax in definition pairs."
  [[lhs rhs]]
  (let [orig-lhs lhs
        [target condition fallback]
        (yamlscript.transformers/split-if-target (second lhs))
        lhs (if condition (assoc lhs 1 target) lhs)]
    (if-lets [_ (vector? lhs)
              _ (= 3 (count lhs))
              [def sym dot] lhs
              _ (= 'def (:Sym def))
              _ (= '. (:Sym dot))
              _ (not (re-find #"\." (str (:Sym sym))))
              _ (if-not (or (map? rhs)
                          (> (count rhs) 1))
                  (die "Invalid dot assignment")
                  true)
              lhs [def sym]
              rhs (dot-rhs rhs sym)]
      [lhs (if condition
             (Lst [(Sym 'if) condition rhs fallback])
             rhs)]
      [orig-lhs rhs])))

(defn adjust-dot-on-right
  "Move right-side dot syntax into a normal dot pair shape."
  [lhs rhs]
  (if-lets [_ (map? lhs)
            _ (vector? rhs)
            [dot & rest] rhs
            _ (= '. (:Sym dot))
            lhs [lhs dot]
            rhs (if (= 1 (count rest)) (first rest) rest)]
    [lhs rhs]
    [lhs rhs]))

(defn adjust-dot-pair
  "Rewrite dot pair syntax into an expression pair."
  [[lhs rhs]]
  (if-lets [[lhs rhs] (adjust-dot-on-right lhs rhs)
            _ (vector? lhs)
            _ (= 2 (count lhs))
            [form dot] lhs
            _ (= '. (:Sym dot))
            _ (if-not (or (map? rhs)
                        (> (count rhs) 1))
                (die "Invalid dot pair")
                true)
            lhs (Sym '=>)
            rhs (dot-rhs rhs form)]
    [lhs rhs]
    [lhs rhs]))

(defn swap-underscores
  "Replace underscore placeholders in a left side with the right side."
  [lhs rhs]
  (let [forms (if-let [forms (:Lst lhs)]
                (when (>= (count forms) 3) forms)
                (when (vector? lhs) lhs))]
    (if-lets [_ (get-in forms [0 :Sym])
              _ (some (partial = {:Sym '_}) forms)
              _ (map? rhs)
              forms (vec (map #(if (= {:Sym '_} %1) rhs %1) forms))]
      [forms []]
      [lhs rhs])))

(defn apply-transformer
  "Run a named special-form transformer when one exists."
  [key val]
  (let [[key val] (swap-underscores key val)]
    (or
      (when-lets [name (or
                         (get-in key [:Sym])
                         (get-in key [0 :Sym]))
                  sym (symbol (str "transform_" name))
                  transformer (ns-resolve transformers-ns sym)]
        (transformer key val))
      [key val])))

(defn transform-child
  "Transform a node or a vector of nodes."
  [node]
  (if (vector? node)
    (mapv transform-node node)
    (transform-node node)))

(defn- normalize-fmap-form
  "Wrap a multi-form form-map expression in a do expression."
  [form]
  (if (or
        (and (vector? form) (> (count form) 1))
        (> (count (:xmap form)) 2))
    {:xmap [(Sym 'do) form]}
    form))

(defn transform-xmap
  "Transform every pair in an expression or forms mapping."
  [node]
  (let [key (key (first node))]
    (->> node
      first
      val
      (partition 2)
      (mapv adjust-dot-def)
      (mapv adjust-dot-pair)
      (apply concat)
      (mapv transform-child)
      (partition 2)
      (reduce
        (fn [acc [k v]]
          (let [[k v] (if (= :xmap key)
                        (apply-transformer k v)
                        [k v])
                [k v] (if (= :fmap key)
                        [(normalize-fmap-form k)
                         (normalize-fmap-form v)]
                        [k v])
                [k v] (if (= :xmap key)
                        [(transform-child k) (transform-child v)]
                        [k v])
                [k v] (if (= :xmap key)
                        (finalize-assignment k v)
                        [k v])]
            (conj acc k v)))
        [])
      (hash-map key))))

(defn transform-dmap
  "Transform code-bearing entries inside a data map."
  [node]
  (->> node
    :dmap
    (reduce (fn [acc node]
              (if (vector? node)
                (conj acc node nil)
                (conj acc node)))
            [])
    (partition 2)
    (mapv adjust-dot-def)
    (mapv adjust-dot-pair)
    (apply concat)
    (mapv transform-child)
    (partition 2)
    (reduce
      (fn [acc [k v]]
        (let [[k v] (if (= :xmap key)
                      (apply-transformer k v)
                      [k v])]
          (conj acc k v)))
      [])
    (remove nil?)
    (hash-map :dmap)))

(defn transform-list
  "Transform every child node in a list AST node."
  [node]
  (assoc node :Lst
    (mapv
      transform-node
      (:Lst node))))

(defn transform-map
  "Transform every child node in a map AST node."
  [node]
  (assoc node :Map
    (mapv
      transform-node
      (:Map node))))

(defn transform-vec
  "Transform every child node in a vector AST node."
  [node]
  (assoc node :Vec
    (mapv
      transform-node
      (:Vec node))))

(defn transform-splat
  "Transform the expression contained by a postfix splat."
  [node]
  (update node :Splat transform-node))

; TODO:
; Turn :xmap mappings into :fmap groups when appropriate.

(defn transform-node
  "Dispatch one AST node through the transformer stage."
  [node]
  (let [anchor (:& node)
        tag (:! node)
        node (condf node
               :xmap (transform-xmap node)
               :fmap (transform-xmap node)  ;; :fmap also uses transform-xmap
               :dmap (transform-dmap node)
               :Assign (transform-assign node)
               :dot (transform-dot node)
               :Lst (transform-list node)
               :Map (transform-map node)
               :Vec (transform-vec node)
               :Splat (transform-splat node)
               node)
        node (if anchor (assoc node :& anchor) node)
        node (if tag (assoc node :! tag) node)]
    node))

(defn transform-node-top
  "Transform the top-level AST form and initialize context."
  [node]
  (transform-node
    (or
      (when-lets [[key val & rest] (:Map node)
                  _ (= key {:Sym '=>})
                  val (or (:xmap val) [{:Sym '=>} val])]
        {:xmap (vec (concat val [{:Sym '=>} {:Map rest}]))})

      (when-lets [[key val & rest] (:dmap node)
                  _ (= key {:Sym '=>})
                  val (or (:xmap val) [{:Sym '=>} val])]
        {:xmap (vec (concat val [{:Sym '=>} {:dmap rest}]))})

      (when-lets [[first & rest] (:Vec node)
                  val (get-in first [:xmap 1 :xmap])]
        {:xmap (concat val [{:Sym '=>} {:Vec rest}])})

      node)))

(comment
  )
