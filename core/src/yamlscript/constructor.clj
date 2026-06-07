;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.constructor is responsible for converting the YS AST into a
;; Clojure AST.

(ns yamlscript.constructor
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [yamlscript.ast :as ast :refer [Lst Map Qts Str Sym Vec]]
   [yamlscript.common]
   [yamlscript.global :as global]
   [yamlscript.re :as re])
  (:refer-clojure))

(declare
  construct-node
  construct-xmap
  declare-undefined
  maybe-call-main
  maybe-trace)

(defn construct-ast
  "Construct YAMLScript AST nodes into a top-level Clojure AST."
  [node]
  (->> node
    (#(construct-node %1))
    (#(if (vector? %1)
        %1
        [%1]))
    (hash-map :Top)
    declare-undefined
    maybe-call-main))

(def ^:dynamic no-wrap false)

(defn construct
  "Construct Clojure AST and wrap the final value when needed."
  [node ctx]
  (let [last (:last ctx)]
    (-> node
      construct-ast
      ((fn [m]
         (update-in m [:Top (dec (count (:Top m)))]
           (fn [n]
             (let [compile (:compile @global/opts)
                   node (if (or (not n) no-wrap (and last compile))
                          n
                          (Lst [(Sym '+++) n]))]
               (maybe-trace node)))))))))

(defn is-splat?
  "Return true when a symbol value uses YAMLScript splat syntax."
  [value]
  (re-matches re/splt (str value)))

(defn expand-splats
  "Rewrite splat arguments into an apply call when needed."
  [nodes]
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

(defn apply-yes-lhs
  "Normalize yes-expression operators on the left side of a call."
  [key val]
  (if-lets [_ (vector? key)
            _ (= 4 (count key))
            [a b c d] key
            _ (re-matches re/osym (str (:Sym d)))
            _ (re-matches re/osym (str (:Sym b)))
            b (or (ast/operators b) b)]
    [[(Lst [b a c]) d] val]
    [key val]))

(defn apply-yes
  "Normalize yes-expression operators before constructing a call."
  [key val]
  (let [[key val] (apply-yes-lhs key val)]
    (if-lets [_ (vector? key)
              _ (= 2 (count key))
              [a b] key
              _ (re-matches re/osym (str (:Sym b)))
              b (or (ast/operators b) b)]
      [[b a] val]
      [key val])))

(defn construct-call
  "Construct one expression-map pair as a Clojure call form."
  [[key val]]
  (let [[key val] (apply-yes key val)]
    (cond
      (= '=> (:Sym key)) val
      (and (:Str key) (nil? val)) key
      :else (Lst (expand-splats (flatten [key val]))))))

(defn construct-tag-call
  "Apply one or more YAML tag calls around a constructed node."
  [node tag]
  ;; XXX - We allow leading colons because they used to be mandatory in v0.
  ;; Now they are optional in v0 and should be removed in v1.
  (let [tag (if (str/starts-with? tag ":") (subs tag 1) tag)
        tags (str/split tag #":")]
    (reduce (fn [node tag]
              (let [[tag splat] (if (re-find #"\*$" tag)
                                  [(subs tag 0 (dec (count tag))) true]
                                  [tag false])]
                (if splat
                  (Lst [(Sym 'apply) (Sym tag) node])
                  (if (vector? node)
                    (Lst [(Sym tag) (first node)])
                    (Lst [(Sym tag) node])))))
      node tags)))

(defn apply-let-bindings
  "Turn leading def pairs into a Clojure let form."
  [lets rest ctx]
  [[(Sym "let")
    (vec
      (concat
        [(Vec (->>
                lets
                flatten
                (remove #(= {:Sym 'def} %1))
                ;; Handle RHS is mapping
                (partition 2)
                (map #(let [[k v] %1]
                        (if (:xmap v)
                          (let [t (:! v)
                                v (construct-xmap v ctx)
                                v (if t [(construct-tag-call v t)] v)]
                            [k (Lst (get-in v [0 :Lst]))])
                          %1)))
                (mapcat identity)
                vec))]
        (construct-xmap {:xmap (mapcat identity rest)} ctx)))]])

(defn check-let-bindings
  "Detect leading def pairs that should become let bindings."
  [xmap ctx]
  (let [[lets rest]
        (map vec
          (split-with
            #(= 'def (get-in %1 [0 0 :Sym]))
            xmap))]
    (if (seq lets)
      (apply-let-bindings lets rest ctx)
      xmap)))

(defn construct-side
  "Construct either side of an expression pair."
  [side ctx]
  (if (vector? side)
    (vec (map #(construct-node %1 ctx) side))
    (construct-node side ctx)))

(defn construct-xmap
  "Construct an expression mapping into Clojure AST forms."
  [{nodes :xmap} ctx]
  (let [xmap (vec (map vec (partition 2 nodes)))
        node (loop [xmap xmap, new []]
               (if (seq xmap)
                 (let [xmap (if (> (:lvl ctx) 1)
                              (check-let-bindings xmap ctx)
                              xmap)
                       [[lhs rhs] & xmap] xmap
                       lhs (if (and
                                 (= 2 (count lhs))
                                 (= {:Sym 'def} (first lhs))
                                 (re-find #"^[\[\{]"
                                   (str (:Sym (second lhs)))))
                             [(Sym '+def) (second lhs)]
                             lhs)
                       lhs (if (and (= lhs {:Sym 'do})
                                 (map? rhs)
                                 (not (some #{:xmap :fmap} (keys rhs))))
                             {:Sym '=>} lhs)
                       [fmap lhs] (if (get-in lhs [:form])
                                    [true (:form lhs)]
                                    [false lhs])
                       lhs (construct-side lhs ctx)
                       rhs (construct-side rhs ctx)
                       rhs (or (:fmap rhs) rhs)
                       new (if fmap
                             (conj new lhs rhs)
                             (conj new (construct-call [lhs rhs])))]
                   (recur xmap, new))
                 new))]
    node))

(defn dmap-code
  "Merge embedded code forms into a constructed data map."
  [code dmap ctx]
  (let [parts (map vec (partition-by #(= (first %1) {:Sym 'def}) code))
        result
        (reduce
          (fn [dmap part]
            (if (= (get-in part [0 0]) {:Sym 'def})
              (let [bind
                    (Vec
                      (vec
                        (map
                          (fn [node]
                            (let [node (construct-node node ctx)]
                              (if (vector? node) (first node) node)))
                          (mapcat rest part))))]
                (Lst [(Sym 'let) bind dmap]))
              (reduce (fn [dmap [form]]
                        (let [form (construct-node form ctx)
                              form (if (map? form)
                                     form
                                     (if (= 1 (count form))
                                       (first form)
                                       (Lst (apply vector (Sym 'do) form))))]
                          (if (= dmap {:Map []})
                            form
                            (Lst [(Sym 'merge) form dmap]))))
                dmap (reverse part))))
          dmap (reverse parts))]
    result))

(defn construct-conditional-pair
  "Construct a data-map pair that appears only when its value is non-nil."
  [key-node val-node ctx]
  (let [key-str (or (:Str key-node) (str key-node))
        val (construct-node val-node ctx)]
    (Lst [(Sym 'if-let)
          (Vec [(Sym '_) (Lst [(Sym 'some?) val])])
          (Map [(Str key-str) (Sym '_)])
          (Map [])])))

(defn- pairs->map
  "Convert a flat list of key/value pairs to a map AST node."
  [pairs ctx]
  (Map
    (vec
      (mapcat
        #(vector
           (construct-node (first %) ctx)
           (construct-node (second %) ctx))
        (partition 2 pairs)))))

(defn- merge-maps
  "Create a merge expression, or return map if base is nil."
  [base new-map]
  (if base
    (Lst [(Sym 'merge) base new-map])
    new-map))

(defn construct-dmap
  "Construct a data map, including conditional and embedded code pairs."
  [node ctx]
  (let [parts (vec (map vec (partition-by vector? (:dmap node))))
        parts (reverse parts)
        parts (if (get-in (vec parts) [0 0 0])
                (cons [] parts)
                parts)
        [amap & parts] parts
        ;; Process amap to handle conditional pairs
        amap (if (some #(:|? %) (take-nth 2 amap))
               ;; Has conditional pairs - process with merges
               (loop [[k v & rest] amap
                      regular-pairs []
                      result nil]
                 (cond
                   (nil? k)
                   (if (empty? regular-pairs)
                     result
                     (merge-maps result (pairs->map regular-pairs ctx)))

                   (:|? k)
                   ;; Flush regular pairs and add conditional
                   (let [base (if (empty? regular-pairs)
                                result
                                (merge-maps result
                                  (pairs->map regular-pairs ctx)))
                         cond-form
                           (construct-conditional-pair
                             (dissoc k :|?) v ctx)]
                     (recur rest [] (merge-maps base cond-form)))

                   :else
                   ;; Accumulate regular pair
                   (recur rest (conj regular-pairs k v) result)))
               ;; No conditional pairs - use original logic
               (construct-node (Map amap) ctx))
        dmap (reduce (fn [dmap part]
                       (if (get-in part [0 0])
                         (dmap-code part dmap ctx)
                         ;; Check if this is a conditional pair
                         (if (and (vector? part)
                               (= 2 (count part))
                               (:|? (first part)))
                           (let [[key-node val-node] part
                                 cond-form (construct-conditional-pair
                                             (dissoc key-node :|?)
                                             val-node
                                             ctx)]
                             (Lst [(Sym 'merge) dmap cond-form]))
                           (let [part (if (vector? part)
                                        (vec
                                          (map #(construct-node %1 ctx) part))
                                        (construct-node part ctx))
                                 amap (Map part)]
                             (Lst [(Sym 'merge) dmap amap])))))
               amap parts)]
    dmap))

(defn construct-fmap
  "Construct every form in a forms mapping."
  [{nodes :fmap} ctx]
  (let [nodes (reduce
                (fn [nodes node]
                  (let [node (construct-node node ctx)]
                    (if (not= '=> (:Sym node))
                      (conj nodes node)
                      nodes)))
                [] nodes)]
    {:fmap nodes}))

;; Handle sequences with code elements
(defn construct-vec-dmap
  "Construct sequence entries that include embedded data-map code."
  [node ctx]
  ;; Must have a dmap element
  (when-lets [nodes (:Vec node)
              _ (some (fn [node]
                        (when-let [dmap (:dmap node)]
                          (and
                            (= 1 (count dmap))
                            (= 1 (count (first dmap))))))
                  nodes)
              nodes (partition-by (comp boolean :dmap) nodes)]
    (let [vect (reduce
                 (fn [new group]
                   (let [nodes (map #(construct-node %1 ctx) group)
                         node (if (:dmap (first group))
                                (if (> (count group) 1)
                                  (Lst (vec (cons (Sym '+concat) (vec nodes))))
                                  (first nodes))
                                (Vec (vec nodes)))
                         new (if new (conj new node) [node])]
                     new))
                 nil nodes)]
      (Lst (vec (cons (Sym '+concat) vect))))))

(defn construct-vec
  "Construct a YAMLScript vector or sequence node."
  [node ctx]
  (or
    (construct-vec-dmap node ctx)
    (let [{nodes :Vec} node
          nodes (expand-splats nodes)
          nodes (map #(construct-node %1 ctx) nodes)]
      {:Vec (-> nodes flatten vec)})))

(defn construct-coll
  "Construct a collection node using the requested AST key."
  [node ctx key]
  (let [{nodes key} node
        nodes (expand-splats nodes)
        nodes (map #(construct-node %1 ctx) nodes)]
    {key (-> nodes flatten vec)}))

(defn construct-trace
  "Wrap a form in tracing when trace mode is active."
  [node]
  (Lst [(Sym 'TTT) node]))

(def do-not-trace '[+++ TTT catch defn defn- finally ns])
(def cannot-trace '[-> ->>])

(defn maybe-trace
  "Apply trace wrapping unless the form cannot or should not be traced."
  [node]
  (if (vector? node)
    (vec (map maybe-trace node))
    (if-lets [_ (:xtrace @global/opts)
              sym (get-in node [:Lst 0 :Sym])
              _ (not (some #{sym} do-not-trace))]
      (if (some #{sym} cannot-trace)
        (die "Cannot yet trace YS code containing: '" sym "'")
        (construct-trace node))
      node)))

(defn construct-alias
  "Construct a document-local YAML alias lookup."
  [node]
  (Lst [(Sym '_*) (Qts (:ali node))]))

(defn construct-stream-alias
  "Construct a stream-level YAML alias lookup."
  [node]
  (Lst [(Sym '_**) (Qts (:Ali node))]))

(defn construct-interop-call
  "Construct Java interop shorthand into a Clojure call."
  [node]
  (if-lets [sym (get-in node [:Lst 0])
            name (str (:Sym sym))
            _ (str/starts-with? name "~")
            method (Sym (str "." (subs name 1)))]
    (->> node vals first rest (cons method) vec Lst)
    node))

(defn construct-node
  "Dispatch a YAMLScript AST node to Clojure AST construction."
  ([node ctx]
   (when (vector? ctx) (die "ctx is a vector"))
   (let [[[key]] (seq node)
         ctx (update-in ctx [:lvl] inc)
         anchor (:& node)
         tag (:! node)
         node (dissoc node :& :! :|?)
         node (case key
                :xmap (construct-xmap node ctx)
                :fmap (construct-fmap node ctx)
                :dmap (construct-dmap node ctx)
                :Map (construct-coll node ctx :Map)
                :Vec (construct-vec node ctx)
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
                node)
         node (construct-interop-call node)]
     (maybe-trace node)))
  ([node]
   (maybe-trace (construct-node node {:lvl 0 :defn false}))))

;;------------------------------------------------------------------------------
;; Fix-up functions
;;------------------------------------------------------------------------------
(defn get-declares
  "Collect unresolved function names that need declare forms."
  [node defns]
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

(defn declare-undefined
  "Prepend declare forms for forward references in definitions."
  [node]
  (let [defn-names (map #(get-in %1 [:Lst 1 :Sym])
                     (filter #(some #{'defn 'defn-}
                               [(get-in %1 [:Lst 0 :Sym])])
                       (rest (get-in node [:Top]))))
        defn-names (zipmap defn-names (repeat true))
        declares (map Sym
                   (keys (get-declares node defn-names)))
        form (Lst (cons (Sym 'declare) declares))
        form (maybe-trace form)]
    (if (seq declares)
      (if (= 'ns (get-in node [:Top 0 :Lst 0 :Sym]))
        (update-in node [:Top]
          #(vec (concat [(first %1)] [form] (rest %1))))
        (update-in node [:Top]
          #(vec (concat [form] %1))))
      node)))

(defn call-main
  "Build the form that invokes main when one is defined."
  []
  (Lst [(Sym 'apply)
        (Sym 'main)
        (Sym 'ARGS)]))

(defn maybe-call-main
  "Append a main call when running a script that defines main."
  [node]
  (let [need-call-main (atom false)]
    (walk/prewalk
      #(let [main (and (= 'defn (get-in %1 [:Lst 0 :Sym]))
                    (= 'main (get-in %1 [:Lst 1 :Sym])))]
         (when main (reset! need-call-main true))
         %1)
      node)
    (if @need-call-main
      (update-in node [:Top] conj (maybe-trace (call-main)))
      node)))

(comment
  )
