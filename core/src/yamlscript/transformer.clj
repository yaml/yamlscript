;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.transformer is responsible for transforming the YAMLScript AST
;; according to certain special rules.

(ns yamlscript.transformer
  (:require
   [yamlscript.ast :refer [Lst Sym QSym]]
   [yamlscript.common]
   [yamlscript.transformers]
   [ys.dwim])
  (:refer-clojure))

(declare
  transform-node
  transform-node-top)

(defn transform
  "Transform special rules for YAMLScript AST."
  [node]
  (transform-node-top node))

(def transformers-ns (the-ns 'yamlscript.transformers))

(def plus-fn?
  (-> 'ys.dwim
    ns-publics
    keys
    (->> (map str)
      (map #(subs %1 1))
      (map symbol))
    set))

(def topic (Sym '_))

(defn dot-list [ctx node]
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

(defn transform-dot [node]
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

(defn dot-rhs [rhs form]
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

(defn adjust-dot-def [[lhs rhs]]
  (if-lets [_ (vector? lhs)
            _ (= 3 (count lhs))
            [def sym dot] lhs
            _ (= 'def (:Sym def))
            _ (= '. (:Sym dot))
            _ (if-not (or (map? rhs)
                        (> (count rhs) 1))
                (die "Invalid dot assignment")
                true)
            lhs [def sym]
            rhs (dot-rhs rhs sym)]
    [lhs rhs]
    [lhs rhs]))

(defn adjust-dot-on-right [lhs rhs]
  (if-lets [_ (map? lhs)
            _ (vector? rhs)
            [dot & rest] rhs
            _ (= '. (:Sym dot))
            lhs [lhs dot]
            rhs (if (= 1 (count rest)) (first rest) rest)]
    [lhs rhs]
    [lhs rhs]))

(defn adjust-dot-pair [[lhs rhs]]
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

(defn transform-def-ops [lhs rhs]
  (when (and
          (vector? lhs)
          (= 3 (count lhs))
          (= 'def (:Sym (first lhs))))
    (let [[a b c] lhs
          lhs [a b]
          op (:Sym c)
          op (Sym (or ({'|| 'or
                        '||| 'or?
                        '+ 'add+
                        '* 'mul+
                        '/ 'div+
                        '** 'pow} op) op))
          rhs (Lst [op b rhs])]
      [lhs rhs])))

(defn swap-underscores [lhs rhs]
  (if-lets [_ (get-in lhs [0 :Sym])
            _ (some (partial = {:Sym '_}) lhs)
            _ (map? rhs)
            lhs (vec (map #(if (= {:Sym '_} %1) rhs %1) lhs))]
    [lhs []]
    [lhs rhs]))

(defn apply-transformer [key val]
  (let [[key val] (swap-underscores key val)]
    (or (if-lets [name (or
                         (get-in key [:Sym])
                         (get-in key [0 :Sym]))
                  sym (symbol (str "transform_" name))
                  transformer (ns-resolve transformers-ns sym)]
          (transformer key val)
          (transform-def-ops key val))
      [key val])))

(defn transform-xmap [node]
  (let [key (key (first node))]
    (->> node
      first
      val
      (partition 2)
      (mapv adjust-dot-def)
      (mapv adjust-dot-pair)
      (apply concat)
      (mapv #(if (vector? %1)
               (mapv transform-node %1)
               (transform-node %1)))
      (partition 2)
      (reduce
        (fn [acc [k v]]
          (let [[k v] (if (= :xmap key)
                        (apply-transformer k v)
                        [k v])]
            (conj acc k v)))
        [])
      (hash-map key))))

(defn transform-dmap [node]
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
    (mapv #(if (vector? %1)
             (mapv transform-node %1)
             (transform-node %1)))
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

(defn transform-list [node]
  (assoc node :Lst
    (mapv
      transform-node
      (:Lst node))))

(defn transform-map [node]
  (assoc node :Map
    (mapv
      transform-node
      (:Map node))))

(defn transform-vec [node]
  (assoc node :Vec
    (mapv
      transform-node
      (:Vec node))))

(defn transform-sym [node]
  (let [sym (str (:Sym node))]
    (when (= sym "%")
      (die "Invalid symbol '%'. Did you mean '%1'?"))
    node))

; TODO:
; Turn :xmap mappings into :fmap groups when appropriate.

(defn transform-node [node]
  (let [anchor (:& node)
        tag (:! node)
        node (condf node
               :xmap (transform-xmap node)
               :fmap (transform-xmap node)  ;; :fmap also uses transform-xmap
               :dmap (transform-dmap node)
               :dot (transform-dot node)
               :Lst (transform-list node)
               :Map (transform-map node)
               :Vec (transform-vec node)
               :Sym (transform-sym node)
               node)
        node (if anchor (assoc node :& anchor) node)
        node (if tag (assoc node :! tag) node)]
    node))

(defn transform-node-top [node]
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
