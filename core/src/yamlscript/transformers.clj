;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.transformers
  (:require
   [yamlscript.ast :refer [Sym Lst Vec Key]]
   [yamlscript.common]
   [yamlscript.ysreader])
  (:refer-clojure))

(def Q {:Sym 'quote})


;;-----------------------------------------------------------------------------
;; cond and case
;;-----------------------------------------------------------------------------

(defn transform-with-else [lhs rhs subst]
  (when-lets [fmap (:fmap rhs)
              len (count fmap)
              _ (>= len 2)
              last-key-pos (- len 2)
              last-key (nth fmap last-key-pos)
              _ (= 'else (:Sym last-key))
              rhs (update-in rhs
                    [:fmap last-key-pos]
                    (fn [_] subst))]
    [lhs rhs]))

(defn transform_cond [lhs rhs]
  (transform-with-else lhs rhs (Key "else")))

(defn transform_condf [lhs rhs]
  (transform-with-else lhs rhs (Sym "=>")))

(defn transform_condp [lhs rhs]
  (transform-with-else lhs rhs (Sym "=>")))

(defn transform_case [lhs rhs]
  (transform-with-else lhs rhs (Sym "=>")))

;;-----------------------------------------------------------------------------
;; defn and fn
;;-----------------------------------------------------------------------------

(defn transform_defn [lhs rhs]
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

(defn transform_catch [lhs rhs]
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

(defn- lhs-tests [lhs rhs]
  (let [lhs (if (> (count lhs) 3)
              [(first lhs) (Lst (yamlscript.ysreader/yes-expr (rest lhs)))]
              lhs)]
    [lhs rhs]))

(defn transform_if [lhs rhs]
  (let [[lhs rhs] (lhs-tests lhs rhs)
        xmap (:xmap rhs)
        _ (when (and xmap (not= (count xmap) 4))
            (die "Invalid 'if' form"))
        rhs (if-lets
              [_ xmap
               [k1 v1 k2 v2] xmap
               _ (= k1 (Sym 'then))]
              (do
                (when-not (= k2 (Sym 'else))
                  (die "Form after 'then' must be 'else'"))
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

(defn transform-vec-destructure [vec-form]
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

(defn transform-bindings [bindings]
  (let [bindings
        (loop [[lhs rhs & forms] (rest bindings) bindings []]
          (let [lhs (if (:Vec lhs)
                      (transform-vec-destructure lhs)
                      lhs)]
            (if (seq forms)
              (recur forms (conj bindings lhs rhs))
              (conj bindings lhs rhs))))]
    (Vec bindings)))

(defn- lhs-bindings [lhs rhs]
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
;; require
;;-----------------------------------------------------------------------------

(def AS (Key "as"))
(def REFER (Key "refer"))

(defn require-spc-lhs? [lhs]
  (when-lets [sym (get-in lhs [0])
              _ (:Sym sym)
              spc (nth lhs 1)
              _ (or (:Spc spc) (:Sym spc))
              _ (= 2 (count lhs))]
    [sym spc]))

(defn require-args [rhs]
  (let [args []
        rhs (if (vector? rhs) rhs [rhs])
        [args rhs] (if-lets [_ (= '=> (get-in rhs [0 :Sym]))
                             alias (nth rhs 1)
                             _ (:Sym alias)]
                     [(conj args AS alias) (drop 2 rhs)]
                     [args rhs])
        args (if (seq rhs)
               (cond
                 (every? :Sym rhs) (conj args REFER (Vec rhs))
                 (= rhs [{:Key :all}]) (conj args REFER (first rhs))
                 :else (die "Invalid 'require' arguments"))
               args)]
    args))

(defn require-xmap [xmap]
  (reduce
    (fn [acc [spc rhs]]
      (or (:Spc spc) (:Sym spc)
        (die "Invalid 'require' xmap"))
      (let [args (if (nil? rhs)
                   (Lst [Q spc])
                   (if (= :all (:Key rhs))
                     (Lst [Q (Vec [spc REFER rhs])])
                     (Lst [Q (Vec (concat [spc] (require-args rhs)))])))]
        (conj acc args)))
    []
    (partition 2 xmap)))

(defn transform_require [lhs rhs]
  (or
    (when-lets [_ (:Sym lhs)
                _ (:Spc rhs)]
      [lhs (Lst [Q rhs])])

    (when-lets [[sym spc] (require-spc-lhs? lhs)
                _ (nil? rhs)]
      [sym (Lst [Q spc])])

    (when-lets [[sym spc] (require-spc-lhs? lhs)
                args (require-args rhs)]
      [sym (Lst [Q (Vec (concat [spc] args))])])

    (when-lets [_ (:Sym lhs)
                xmap (:xmap rhs)
                args (require-xmap xmap)]
      [lhs args])

    (die "Invalid 'require' form")))

(comment
  )
