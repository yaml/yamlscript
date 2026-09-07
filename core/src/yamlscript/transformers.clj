;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.transformers namespace contains named rewrites for special
;; forms. yamlscript.transformer finds these by `transform_<symbol>` name.

(ns yamlscript.transformers
  (:require
   [clojure.string :as str]
   [yamlscript.ast :refer [Sym Lst Vec Key]]
   [ys.v0.common]
   [ys.v0.util :as util]
   [yamlscript.ysreader])
  (:refer-clojure))

(defn- if-marker?
  "Return true for the ':if' marker in conditional assignment targets."
  [node]
  (= :if (:Key node)))

(defn- target-node
  "Return a binding target from one or more parsed target forms."
  [forms]
  (if (= 1 (count forms))
    (first forms)
    (Vec (vec forms))))

(defn split-if-target
  "Split a conditional assignment target into target, condition and fallback."
  [target]
  (when-lets [sym (:Sym target)
              s (str sym)
              _ (and
                  (> (count s) 2)
                  (= \[ (first s))
                  (= \] (last s))
                  (re-find #" +:if +" s))
              forms (yamlscript.ysreader/read-string
                      (subs s 1 (dec (count s))))
              forms (if (vector? forms) forms [forms])
              [lhs [_ & rhs]] [(take-while (complement if-marker?) forms)
                                (drop-while (complement if-marker?) forms)]
              _ (if (and (seq lhs) (= 1 (count rhs)))
                  true
                  (util/die "Invalid conditional assignment: "
                    (subs s 1 (dec (count s)))))]
    (let [target (target-node lhs)]
      [target (first rhs) target])))

(defn- assignment-target-forms
  "Parse one or more assignment targets when any target is dotted."
  [target]
  (let [target
        (if-lets [sym (:Sym target)
                  text (str sym)
                  _ (str/includes? text ".")]
          (yamlscript.ysreader/read-string text)
          target)]
    (cond
      (:Vec target) (:Vec target)
      (vector? target) target
      :else [target])))

(defn- dotted-assignment-targets
  "Return parsed targets when an assignment contains a dotted target."
  [target]
  (let [targets (assignment-target-forms target)]
    (when (some :dot targets)
      targets)))

(defn- assignment-operator
  "Normalize an assignment operator to its runtime function."
  [op]
  (Sym (or ({'|| 'or
             '||| 'or?
             '+ 'add+
             '* 'mul+
             '/ 'div+
             '** 'pow} op) op)))


;;-----------------------------------------------------------------------------
;; cond and case
;;-----------------------------------------------------------------------------

(defn transform-with-else
  "Normalize trailing else markers in cond-like forms."
  [lhs rhs subst]
  (when-let [fmap (:fmap rhs)]
    (let [last-key-pos (- (count fmap) 2)
          last-key (when (>= last-key-pos 0)
                     (nth fmap last-key-pos))
          last-sym (:Sym last-key)
          fmap (if (or (= '=> last-sym) (= 'else last-sym))
                 (assoc fmap last-key-pos subst)
                 fmap)]
      [lhs (assoc rhs :fmap fmap)])))

(defn transform_cond
  "Transform YAMLScript cond syntax into Clojure cond syntax."
  [lhs rhs]
  (transform-with-else lhs rhs (Key "else")))

(defn transform_condf
  "Transform YAMLScript condf syntax into Clojure condf syntax."
  [lhs rhs]
  (transform-with-else lhs rhs (Sym "=>")))

(defn transform_condp
  "Transform YAMLScript condp syntax into Clojure condp syntax."
  [lhs rhs]
  (transform-with-else lhs rhs (Sym "=>")))

(defn transform_case
  "Transform YAMLScript case syntax into Clojure case syntax."
  [lhs rhs]
  (transform-with-else lhs rhs (Sym "=>")))

;;-----------------------------------------------------------------------------
;; def, defn and fn
;;-----------------------------------------------------------------------------

(comment
  (yamlscript.compiler/compile "
!ys-0
defn x():
  a b =: c d")
  )

(defn transform_def
  "Normalize definition forms, including operator update syntax."
  [lhs rhs]
  (let [[target condition fallback] (split-if-target (second lhs))
        target (or target (second lhs))
        lhs (if condition (assoc lhs 1 target) lhs)
        targets (dotted-assignment-targets target)
        rhs (if (and (vector? rhs) (> (count rhs) 1))
              (Vec rhs)
              rhs)
        [lhs rhs]
        (cond
          targets
          (let [op (when (= 3 (count lhs)) (:Sym (nth lhs 2)))
                assign {:targets targets
                        :operator (when op (assignment-operator op))
                        :condition condition}]
            [[(first lhs) {:Assign assign}] rhs])

          (= 2 (count lhs))
          [lhs rhs]

          (= 3 (count lhs))
          (let [[a b c] lhs
                lhs [a b]
                op (:Sym c)
                op (assignment-operator op)
                rhs (Lst [op b rhs])]
            [lhs rhs])
          :else [lhs rhs])
        rhs (if (and condition (not targets))
              (Lst [(Sym 'if) condition rhs fallback])
              rhs)]
    [lhs rhs]))

(defn transform_defn
  "Convert multi-body defn shorthand into explicit arities."
  [lhs rhs]
  (when-lets [lhs (remove nil? lhs)
              lhs (vec lhs)
              _ (= 2 (count lhs))
              kind (get-in lhs [0 :Sym])
              _ (#{'defn 'fn} kind)
              xmap (:xmap rhs)
              _ (every? :Lst (->> xmap (partition 2) (map first)))
              xmap (reduce
                     (fn [acc [lhs rhs]]
                       (let [lhs (Vec (:Lst lhs))]
                         (conj acc lhs rhs)))
                     []
                     (partition 2 xmap))
              rhs {:xmap xmap}]
    [lhs rhs]))

(defn transform_catch
  "Fill in default exception class and binding for catch forms."
  [lhs rhs]
  (let [lhs (cond
              (= lhs (Sym 'catch))
              [lhs (Sym 'Exception) (Sym '_e)]
              ,
              (= (count lhs) 2)
              [(first lhs) (Sym 'Exception) (second lhs)]
              ,
              :else lhs)]
    [lhs rhs]))


;;-----------------------------------------------------------------------------
;; Group LHS arguments as a single conditional test form
;;-----------------------------------------------------------------------------

(defn- lhs-tests
  "Group a multi-token conditional left side into one test form."
  [lhs rhs]
  (let [lhs (if (> (count lhs) 3)
              [(first lhs) (Lst (yamlscript.ysreader/yes-expr (rest lhs)))]
              lhs)]
    [lhs rhs]))

(defn transform_if
  "Normalize if forms, including then/else block maps."
  [lhs rhs]
  (let [[lhs rhs] (lhs-tests lhs rhs)
        xmap (:xmap rhs)
        _ (when (and xmap (not= (count xmap) 4))
            (util/die "Invalid 'if' form"))
        rhs (if-lets
              [_ xmap
               [k1 v1 k2 v2] xmap
               _ (= k1 (Sym 'then))]
              (do
                (when-not (= k2 (Sym 'else))
                  (util/die "Form after 'then' must be 'else'"))
                (let [rhs
                      (if (> (count (:xmap v1)) 2)
                        (update-in rhs [:xmap 0] (fn [_] (Sym 'do)))
                        (update-in rhs [:xmap 0] (fn [_] (Sym '=>))))
                      rhs
                      (if (> (count (:xmap v2)) 2)
                        (update-in rhs [:xmap 2] (fn [_] (Sym 'do)))
                        (update-in rhs [:xmap 2] (fn [_] (Sym '=>))))]
                  rhs))
              (if-lets
                [_ xmap
                 [_ _ k2 v2] xmap
                 _ (= k2 (Sym 'else))]
                (if (> (count (:xmap v2)) 2)
                  (update-in rhs [:xmap 2] (fn [_] (Sym 'do)))
                  (update-in rhs [:xmap 2] (fn [_] (Sym '=>))))
                rhs))]
    [lhs rhs]))

(intern 'yamlscript.transformers 'transform_if-not   transform_if)
(intern 'yamlscript.transformers 'transform_when     lhs-tests)
(intern 'yamlscript.transformers 'transform_when-not lhs-tests)
(intern 'yamlscript.transformers 'transform_while    lhs-tests)


;;-----------------------------------------------------------------------------
;; let destructuring
;;-----------------------------------------------------------------------------

(defn transform-vec-destructure
  "Rewrite YAMLScript vector rest destructuring to Clojure form."
  [vec-form]
  (if-lets [vect (:Vec vec-form)
            form (last vect)
            list (:Lst form)
            _ (= 2 (count list))
            _ (= {:Sym '_**} (first list))
            sym (:Qts (second list))]
    (Vec (conj (vec (drop-last vect)) (Sym '&) (Sym sym)))
    vec-form))


;;-----------------------------------------------------------------------------
;; Group LHS arguments as a single bindings form
;;-----------------------------------------------------------------------------

(defn transform-bindings
  "Group alternating binding names and values into a vector form."
  [bindings]
  (let [bindings
        (loop [[lhs rhs & forms] (rest bindings) bindings []]
          (let [lhs (if (:Vec lhs)
                      (transform-vec-destructure lhs)
                      lhs)]
            (if (seq forms)
              (recur forms (conj bindings lhs rhs))
              (conj bindings lhs rhs))))]
    (Vec bindings)))

(defn- lhs-bindings
  "Normalize binding-style special forms to one bindings vector."
  [lhs rhs]
  (let [lhs (cond
              (> (count lhs) 2) [(first lhs) (transform-bindings lhs)]
              (:Sym lhs) [lhs (Vec [])]
              :else lhs)]
    [lhs rhs]))

(intern 'yamlscript.transformers 'transform_binding    lhs-bindings)
(intern 'yamlscript.transformers 'transform_doseq      lhs-bindings)
(intern 'yamlscript.transformers 'transform_dotimes    lhs-bindings)
(intern 'yamlscript.transformers 'transform_each       lhs-bindings)
(intern 'yamlscript.transformers 'transform_for        lhs-bindings)
(intern 'yamlscript.transformers 'transform_if-let     lhs-bindings)
(intern 'yamlscript.transformers 'transform_if-lets    lhs-bindings)
(intern 'yamlscript.transformers 'transform_if-some    lhs-bindings)
(intern 'yamlscript.transformers 'transform_let        lhs-bindings)
(intern 'yamlscript.transformers 'transform_loop       lhs-bindings)
(intern 'yamlscript.transformers 'transform_when-first lhs-bindings)
(intern 'yamlscript.transformers 'transform_when-let   lhs-bindings)
(intern 'yamlscript.transformers 'transform_when-lets  lhs-bindings)
(intern 'yamlscript.transformers 'transform_when-some  lhs-bindings)
(intern 'yamlscript.transformers 'transform_with-open  lhs-bindings)


;;-----------------------------------------------------------------------------
;; retired require
;;-----------------------------------------------------------------------------

(defn transform_require
  "Compile every former require form as a call to its retirement stub."
  [lhs _]
  [(if (vector? lhs) (first lhs) lhs) []])

(comment
  )
