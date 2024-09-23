;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.constructor is responsible for converting the YAMLScript AST
;; into a Clojure AST.

(ns yamlscript.constructor
  (:require
   [clojure.walk :as walk]
   [yamlscript.ast :as ast :refer [Lst Qts Str Sym Vec]]
   [yamlscript.common :as common]
   [yamlscript.re :as re]
   [yamlscript.util :refer [die if-lets]]))

(declare
  construct-node
  construct-pairs
  declare-undefined
  maybe-call-main
  maybe-trace)

(defn construct-ast
  "Construct resolved YAML tree into a YAMLScript AST."
  [node]
  (->> node
    (#(construct-node %1))
    (#(if (vector? %1)
        %1
        [%1]))
    (hash-map :Top)
    declare-undefined
    maybe-call-main))

(defn construct
  "Make the AST and add wrap the last node."
  [node last]
  (-> node
    construct-ast
    ((fn [m]
       (update-in m [:Top (dec (count (:Top m)))]
         (fn [n]
           (let [compile (:compile @common/opts)
                 node (if (and last compile)
                        n
                        (Lst [(Sym '+++) n]))]
             (maybe-trace node))))))))

(defn is-splat? [value]
  (re-matches re/splt (str value)))

(defn expand-splats [nodes]
  (if (some #(is-splat? (:Sym %1)) nodes)
    (let [[fun & nodes] nodes]
      (loop [nodes nodes new [] splats []]
        (if (seq nodes)
          (let [[node & nodes] nodes
                val (str (:Sym node))
                [new splats] (if (or (seq splats)
                                   (is-splat? val))
                               (let [node (if (is-splat? val)
                                            (Sym (apply str (butlast val)))
                                            (Vec [node]))]
                                 [new (conj splats node)])
                               [(conj new node) splats])]
            (recur nodes new splats))
          (let [splats (if (> (count splats) 1)
                         [(Lst (vec (cons (Sym 'concat) splats)))]
                         splats)]
            (concat [(Sym 'apply)] [fun] new splats)))))
    nodes))

(defn apply-yes-lhs [key val]
  (if-lets [_ (vector? key)
            _ (= 4 (count key))
            [a b c d] key
            _ (re-matches re/osym (str (:Sym d)))
            _ (re-matches re/osym (str (:Sym b)))
            b (or (ast/operators b) b)]
    [[(Lst [b a c]) d] val]
    [key val]))

(defn apply-yes [key val]
  (let [[key val] (apply-yes-lhs key val)]
    (if-lets [_ (vector? key)
              _ (= 2 (count key))
              [a b] key
              _ (re-matches re/osym (str (:Sym b)))
              b (or (ast/operators b) b)]
      [[b a] val]
      [key val])))

(defn construct-call [[key val]]
  (let [[key val] (apply-yes key val)]
    (cond
      (= '=> (:Sym key)) val
      (and (:Str key) (nil? val)) key
      :else (Lst (expand-splats (flatten [key val]))))))

(defn apply-let-bindings [lets rest ctx]
  [[(Sym "let")
    (vec
      (concat
        [(Vec (->>
                lets
                flatten
                (filter #(not= {:Sym 'def} %1))
                ;; Handle RHS is mapping
                (partition 2)
                (map #(let [[k v] %1]
                        (if (:pairs v)
                          [k (Lst (get-in
                                    (construct-pairs v ctx)
                                    [0 :Lst]))]
                          %1)))
                (mapcat identity)
                vec))]
        (construct-pairs {:pairs (mapcat identity rest)} ctx)))]])

(defn check-let-bindings [pairs ctx]
  (let [[lets rest]
        (map vec
          (split-with
            #(= 'def (get-in %1 [0 0 :Sym]))
            pairs))]
    (if (seq lets)
      (apply-let-bindings lets rest ctx)
      pairs)))

(defn construct-pairs [{nodes :pairs} ctx]
  (let [pairs (vec (map vec (partition 2 nodes)))
        construct-side (fn [n c] (if (vector? n)
                                   (vec (map #(construct-node %1 c) n))
                                   (construct-node n c)))
        node (loop [pairs pairs, new []]
               (if (seq pairs)
                 (let [pairs (if (> (:lvl ctx) 1)
                               (check-let-bindings pairs ctx)
                               pairs)
                       [[lhs rhs] & pairs] pairs
                       lhs (if (and
                                 (= 2 (count lhs))
                                 (= {:Sym 'def} (first lhs))
                                 (re-find #"^[\[\{]"
                                   (str (:Sym (second lhs)))))
                             [(Sym '+def) (second lhs)]
                             lhs)
                       lhs (if (and (= lhs {:Sym 'do})
                                 (map? rhs)
                                 (not (some #{:pairs :forms} (keys rhs))))
                             {:Sym '=>} lhs)
                       [forms lhs] (if (get-in lhs [:form])
                                     [true (:form lhs)]
                                     [false lhs])
                       lhs (construct-side lhs ctx)
                       rhs (construct-side rhs ctx)
                       rhs (or (:forms rhs) rhs)
                       new (if forms
                             (conj new lhs rhs)
                             (conj new (construct-call [lhs rhs])))]
                   (recur pairs, new))
                 new))]
    node))

(defn construct-forms [{nodes :forms} ctx]
  (let [nodes (reduce
                (fn [nodes node]
                  (let [node (construct-node node ctx)]
                    (if (not= '=> (:Sym node))
                      (conj nodes node)
                      nodes)))
                [] nodes)]
    {:forms nodes}))

(defn construct-coll [node ctx key]
  (let [{nodes key} node
        nodes (expand-splats nodes)
        nodes (map #(construct-node %1 ctx) nodes)]
    {key (-> nodes flatten vec)}))

(defn maybe-trace [node]
  (if-lets [_ (:xtrace @common/opts)
            sym (get-in node [:Lst 0 :Sym])
            _ (type sym)
            _ (not= '_X sym)]
    (Lst [(Sym '_X) node])
    node)
  )

(defn construct-alias [node]
  (Lst [(Sym '_*) (Qts (:ali node))]))

(defn construct-stream-alias [node]
  (Lst [(Sym '_**) (Qts (:Ali node))]))

(defn construct-tag-call [node tag]
  (or (re-find #":$" tag)
    (die "Tag must end with a colon"))
  (let [tag (subs tag 0 (dec (count tag)))
        [tag splat] (if (re-find #"\*$" tag)
                      [(subs tag 0 (dec (count tag))) true]
                      [tag false])
        kind (-> node first key)]
    (if splat
      (case kind
        :Vec (Lst [(Sym 'apply) (Sym tag) node])
        ,    (die "Splat only allowed on Vec"))
      (Lst [(Sym tag) node]))))

(defn construct-node
  ([node ctx]
   (when (vector? ctx) (die "ctx is a vector"))
   (let [[[key]] (seq node)
         ctx (update-in ctx [:lvl] inc)
         anchor (:& node)
         tag (:! node)
         node (dissoc node :& :!)
         node (case key
                :pairs (construct-pairs node ctx)
                :forms (construct-forms node ctx)
                :Map (construct-coll node ctx :Map)
                :Vec (construct-coll node ctx :Vec)
                :Lst (construct-coll node ctx :Lst)
                :ali (construct-alias node)
                :Ali (construct-stream-alias node)
                ,      node)
         node (if anchor
                (let [node (if (vector? node)
                             (first node)
                             node)]
                  (Lst [(Sym '_&) (Qts anchor) node]))
                node)
         node (if tag
                (construct-tag-call node tag)
                node)]
     (maybe-trace node)))
  ([node]
   (construct-node node {:lvl 0 :defn false})))

;;------------------------------------------------------------------------------
;; Fix-up functions
;;------------------------------------------------------------------------------
(defn get-declares [node defns]
  (let [declare (atom {})
        defined (atom {})]
    (walk/prewalk
      #(let [fn-name (get-in %1 [:Lst 0 :Sym])
             defn-name (when (some #{'defn 'defn-} [fn-name])
                         (get-in %1 [:Lst 1 :Sym]))
             sym-name (get-in %1 [:Sym])]
         (when defn-name (swap! defined assoc defn-name true))
         (when (and sym-name
                 (get defns sym-name)
                 (not (get @defined sym-name)))
           (swap! declare assoc sym-name true))
         %1)
      node)
    @declare))

(defn declare-undefined [node]
  (let [defn-names (map #(get-in %1 [:Lst 1 :Sym])
                     (filter #(some #{'defn 'defn-}
                               [(get-in %1 [:Lst 0 :Sym])])
                       (rest (get-in node [:Top]))))
        defn-names (zipmap defn-names (repeat true))
        declares (map Sym
                   (keys (get-declares node defn-names)))
        form (Lst (cons (Sym 'declare) declares))]
    (if (seq declares)
      (if (= 'ns (get-in node [:Top 0 :Lst 0 :Sym]))
        (update-in node [:Top]
          #(vec (concat [(first %1)] [form] (rest %1))))
        (update-in node [:Top]
          #(vec (concat [form] %1))))
      node)))

(defn call-main []
  (maybe-trace
    (Lst [(Sym 'apply)
          (Sym 'main)
          (Sym 'ARGS)])))

(defn maybe-call-main [node]
  (let [need-call-main (atom false)]
    (walk/prewalk
      #(let [main (and (= 'defn (get-in %1 [:Lst 0 :Sym]))
                    (= 'main (get-in %1 [:Lst 1 :Sym])))]
         (when main (reset! need-call-main true))
         %1)
      node)
    (if @need-call-main
      (update-in node [:Top] conj (call-main))
      node)))

(comment
  )
