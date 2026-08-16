;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.transformers namespace contains named rewrites for special
;; forms. yamlscript.transformer finds these by `transform_<symbol>` name.

(ns yamlscript.transformers
  (:require
   [clojure.string :as str]
   [yamlscript.ast :refer [Clj Sym Lst Vec Key]]
   [ys.v0.common]
   [yamlscript.ysreader])
  (:refer-clojure))

(def Q {:Sym 'quote})

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
                  (die "Invalid conditional assignment: "
                    (subs s 1 (dec (count s)))))]
    (let [target (target-node lhs)]
      [target (first rhs) target])))


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
        lhs (if condition (assoc lhs 1 target) lhs)
        [lhs rhs]
        (cond
          (= 2 (count lhs))
          (let [rhs (if (and (vector? rhs) (> (count rhs) 1))
                      (Vec rhs)
                      rhs)]
            [lhs rhs])
          (= 3 (count lhs))
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
            [lhs rhs])
          :else [lhs rhs])
        rhs (if condition
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
;; require
;;-----------------------------------------------------------------------------

(def AS (Key "as"))
(def EXCLUDE (Key "exclude"))
(def REFER (Key "refer"))
(def RENAME (Key "rename"))

(defn require-spc-lhs?
  "Detect require left sides that name a namespace and alias."
  [lhs]
  (when-lets [sym (get-in lhs [0])
              _ (:Sym sym)
              spc (nth lhs 1)
              _ (or (:Spc spc) (:Sym spc))
              _ (= 2 (count lhs))]
    [sym spc]))

(defn require-forms-str
  "Render require argument nodes for migration error messages."
  [nodes]
  (str/join " " (map #(str (or (:Sym %1) (:Key %1))) nodes)))

(defn require-legacy-error
  "Reject old positional require options with migration guidance."
  [rhs]
  (let [form (require-forms-str rhs)]
    (if (= '=> (get-in rhs [0 :Sym]))
      (let [[_ alias & refers] rhs
            alias (:Sym alias)
            _ (or alias (die "Invalid 'require' alias syntax"))
            _ (or (every? :Sym refers)
                (die "Invalid 'require' alias syntax"))
            replacement (str ":as " alias
                          (when (seq refers)
                            (str " :get " (require-forms-str refers))))]
        (die "Legacy 'require' syntax '" form
          "'. Use '" replacement "' instead."))
      (when (every? :Sym rhs)
        (die "Legacy 'require' syntax '" form
          "'. Use ':get " form "' instead.")))))

(defn require-option-error
  "Report an invalid or unsupported require option."
  [option]
  (if (= :refer option)
    (die "Invalid 'require' option ':refer'. Use ':get' instead.")
    (die "Invalid 'require' option ':" (name option) "'")))

(defn require-symbols
  "Take one or more symbol arguments for a require option."
  [option nodes]
  (let [[symbols nodes] (split-with :Sym nodes)]
    (when-not (seq symbols)
      (die "Invalid 'require' option ':" (name option)
        "': expected at least one symbol"))
    [symbols nodes]))

(defn parse-require-options
  "Parse keyword-based YAMLScript require options."
  [rhs]
  (loop [nodes rhs options {}]
    (if-not (seq nodes)
      options
      (let [node (first nodes)
            option (:Key node)]
        (or option
          (require-legacy-error nodes)
          (die "Invalid 'require' arguments"))
        (when-not (some #{option} [:as :get :all :not :none])
          (require-option-error option))
        (when (contains? options option)
          (die "Duplicate 'require' option ':" (name option) "'"))
        (case option
          :as
          (let [alias (second nodes)]
            (when-not (:Sym alias)
              (die "Invalid 'require' option ':as': expected one symbol"))
            (recur (drop 2 nodes) (assoc options option alias)))

          :get
          (let [[symbols nodes] (require-symbols option (rest nodes))]
            (recur nodes (assoc options option symbols)))

          :not
          (let [[symbols nodes] (require-symbols option (rest nodes))]
            (when (some #(namespace (:Sym %1)) symbols)
              (die "Invalid 'require' option ':not': expected plain symbols"))
            (recur nodes (assoc options option symbols)))

          :all
          (recur (rest nodes) (assoc options option true))

          :none
          (recur (rest nodes) (assoc options option true)))))))

(defn validate-require-options
  "Reject conflicting YAMLScript require selection options."
  [options]
  (when (and (:none options)
          (some options [:get :all :not]))
    (die "Invalid 'require' options: ':none' cannot be combined with "
      "':get', ':all', or ':not'"))
  (when (and (:get options) (:all options))
    (die "Invalid 'require' options: ':get' cannot be combined with ':all'"))
  (when (and (:get options) (:not options))
    (die "Invalid 'require' options: ':get' cannot be combined with ':not'"))
  options)

(defn require-get-args
  "Build Clojure refer and rename arguments from ':get' symbols."
  [symbols]
  (let [[refers renames]
        (reduce
          (fn [[refers renames] node]
            (let [sym (:Sym node)]
              (if-let [old (namespace sym)]
                [(conj refers (Sym old))
                 (conj renames (symbol old) (symbol (name sym)))]
                [(conj refers node) renames])))
          [[] []] symbols)]
    (vec
      (concat [REFER (Vec refers)]
        (when (seq renames)
          [RENAME (Clj (apply array-map renames))])))))

(defn require-args
  "Build require arguments from a resolved require right side."
  [rhs]
  (let [rhs (if (vector? rhs) rhs [rhs])
        _ (require-legacy-error rhs)
        options (-> rhs parse-require-options validate-require-options)]
    (vec
      (concat
        (when-let [alias (:as options)] [AS alias])
        (when-let [symbols (:get options)]
          (require-get-args symbols))
        (when (or (:all options) (:not options))
          [REFER (Key "all")])
        (when-let [symbols (:not options)]
          [EXCLUDE (Vec symbols)])))))

(defn require-lib
  "Build one quoted Clojure require libspec."
  [spc rhs]
  (let [args (require-args rhs)]
    (if (seq args)
      (Lst [Q (Vec (concat [spc] args))])
      (Lst [Q spc]))))

(defn require-xmap
  "Build require arguments from a require form map."
  [xmap]
  (reduce
    (fn [acc [spc rhs]]
      (or (:Spc spc) (:Sym spc)
        (die "Invalid 'require' xmap"))
      (let [args (if (nil? rhs)
                   (Lst [Q spc])
                   (require-lib spc rhs))]
        (conj acc args)))
    []
    (partition 2 xmap)))

(defn transform_require
  "Normalize YAMLScript require syntax into Clojure require syntax."
  [lhs rhs]
  (or
    (when-lets [_ (:Sym lhs)
                _ (:Spc rhs)]
      [lhs (Lst [Q rhs])])

    (when-lets [[sym spc] (require-spc-lhs? lhs)
                _ (nil? rhs)]
      [sym (Lst [Q spc])])

    (when-lets [[sym spc] (require-spc-lhs? lhs)
                form (require-lib spc rhs)]
      [sym form])

    (when-lets [_ (:Sym lhs)
                xmap (:xmap rhs)
                args (require-xmap xmap)]
      [lhs args])

    (die "Invalid 'require' form")))

(comment
  )
