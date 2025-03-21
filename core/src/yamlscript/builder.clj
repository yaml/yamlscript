;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.builder is responsible parsing all the !expr nodes into YS
;; AST nodes.

(ns yamlscript.builder
  (:require
   [clojure.string :as str]
   [yamlscript.ast :refer
    [Bln Clj Flt Int Key Lst Map Nil Str Sym Vec]]
   [yamlscript.common]
   [yamlscript.composer]
   [yamlscript.global]
   [yamlscript.parser]
   [yamlscript.re :as re]
   [yamlscript.resolver]
   [yamlscript.ysreader :as ysreader])
  (:refer-clojure))

(declare build-node)

(defn adjust-special-key-top [node]
  (if-lets [[key val & rest] (:map node)
            _ (re-matches #":.*[^-\w].*" (str (:expr key)))]
    {:map
     (concat
       [{:expr "=>"}
        {:xmap [{:expr (subs (:expr key) 1)}
                (if (= {:nil ""} val) {:expr ""} val)]}]
       rest)}
    node))

(defn build
  "Parse all the !expr nodes into YS AST nodes."
  [node] (build-node (adjust-special-key-top node)))

(defn build-from-string [string]
  (require
    '[yamlscript.parser]
    '[yamlscript.composer]
    '[yamlscript.resolver])
  (-> string
    yamlscript.parser/parse
    yamlscript.composer/compose
    first
    yamlscript.resolver/resolve
    yamlscript.builder/build))

(defn build-defn-defaults [name doc args body kind]
  (when-lets [args (:Vec args)
              dargs (filter #(vector? (:Sym %1)) args)
              _ (seq dargs)
              dmap (reduce (fn [m {[k v] :Sym}] (assoc m {:Sym k} v)) {} dargs)
              defn (Sym kind)
              args (map Sym
                     (map #(or (get-in % [:Sym 0])
                             (get % :Sym)) args))
              last2 (or (take-last 2 args) '())
              orig-args args
              args (if (= (first last2) {:Sym '&})
                     (drop-last 2 args)
                     args)
              n-args (count args)
              n-dargs (count dargs)
              main (Lst [(Vec orig-args) body])
              bodies (reduce
                       (fn [bodies i]
                         (let [nargs (->> args (take i) vec)
                               dargs (->> args (drop i) vec)
                               dvals (->> dargs (map #(get dmap %)) vec)
                               body (Lst [(Vec nargs)
                                          (Lst (-> nargs
                                                 (#(if name (cons name %) %))
                                                 vec
                                                 (concat dvals)))])]
                           (conj bodies body)))
                       [main]
                       (range (dec n-args) (- n-args n-dargs 1) -1))]
    [[defn name doc] bodies]))

(defn build-defn-single [name doc args body kind]
  (when-lets [defn (Sym kind)
              _ true]
    [[defn name doc args] body]))

(defn fix-args [args]
  (let [args (if (= args "*") "& _" args)
        args (str/replace args #"(?:^| )\*$" "& _")
        args (str/replace args (re/re #"\*($symw)") "& $1")]
    args))

(defn build-defn [key val]
  (let [[key kind] (if-let [key (:defn key)]
                     [key 'defn]
                     [(:fn key) 'fn])
        [_ kind name args] (if (= kind 'defn)
                             (re-matches re/dfnk key)
                             (re-matches re/afnk key))
        kind (symbol kind)
        name (when name (Sym name))
        args (when args
               (let [args (fix-args args)
                     args (build-node {:expr args})
                     args (if (map? args) [args] args)]
                 (Vec args)))
        body (build-node
               (if-lets [_ (nil? args)
                         xmap (:xmap val)]
                 (let [xmap (loop [[k v & xmap] xmap new []]
                               (if (nil? v)
                                 new
                                 (let [args (:expr k)
                                       args (fix-args args)
                                       args {:expr args}]
                                   (recur xmap (conj new args v)))))]
                   {:xmap xmap})
                 val))
        [doc body] (if-lets [[key val & nodes] (:xmap body)
                             [doc body] (when (and (nil? key) (:Str val))
                                          [val {:xmap nodes}])]
                     [doc body]
                     [nil body])]
    (or
      (build-defn-defaults name doc args body kind)
      (build-defn-single name doc args body kind))))

(defn build-form-pair [key val]
  (let [key (:form key)]
    [{:form (build-node key)} (build-node val)]))

(defn build-pair [nodes [key val]]
  (let [[key val] (condf key
                    :defn (build-defn key val)
                    :fn (build-defn key val)
                    :form (build-form-pair key val)
                    [(build-node key) (build-node val)])]
    (conj nodes key val)))

(defn build-xmap [{xmap :xmap}]
  (let [nodes (reduce build-pair [] (partition 2 xmap))]
    {:xmap nodes}))

(defn build-fmap [node]
  (->> node
    first
    val
    (map build-node)
    (hash-map :fmap)))

;; XXX This might belong in the transformer
(defn optimize-ys-expression [node]
  (if (= '=> (get-in node [:Lst 0 :Sym]))
    (get-in node [:Lst 1])
    node))

(defn build-expr [node]
  (let [string (:expr node)]
    (when-not (empty? string)
      (let [expr (ysreader/read-string string)]
        (when expr
          (optimize-ys-expression expr))))))

;; XXX The destructure vector is just a string here.
;; Needs to be parsed into a proper AST node.
(defn destruct-vec [s]
  (let [s (if (re-find (re/re #"(?:^$ysym |\] )") s)
            (str "[" s "]")
            s)]
    (str/replace s (re/re #"\[(.*)\*($ysym)\s*\]") "[$1 & $2]")))

(defn build-def [{node :def}]
  (if-lets [m (re-matches re/defk node)
            v [(Sym "def")
               (Sym (destruct-vec (m 1)))]]
    (if (empty? (m 2))
      v
      (conj v (Sym (m 2))))))

(defn build-dmap [body]
  (let [dmap (loop [[k v & body] body new []]
               (if (nil? k)
                 new
                 (let [[k v] (if (= k {:Nil nil})
                               [[v] nil]
                               [k v])
                       new (if (vector? k)
                             (if (nil? v)
                               (conj new k)
                               (conj new (conj k v)))
                             (conj new k v))]
                   (recur body new))))]
    {:dmap dmap}))

(defn adjust-special-keys [nodes]
  (reduce
    (fn [body [key val]]
      (let [[key val] (if (= \: (first (:expr key)))
                        [{:nil ""}
                         {:xmap [{:expr (subs (:expr key) 1)}
                                 val]}]
                        [key val])]
        (conj body key val)))
    [] (partition 2 nodes)))

(defn build-map [node]
  (let [body (vec (map build-node (adjust-special-keys (:map node))))]
    (if (or (some vector? body)
          (= {:Sym '=>} (first body))
          (some #(= (first %1) {:Nil nil}) (partition 2 body)))
      (build-dmap body)
      (Map (map build-node (:map node))))))

(defn build-vec [node]
  (Vec (map build-node (:seq node))))

;; XXX This is a hack. It should be done with a proper parser.
(def re-interpolated-string
  (re/re
    #"(?sx)
    (?:
      (?:
        (?: \\\$ | \$ [^\w\{\(] | [^\$] )+?
        (?= \$ $symw | \$ $bpar | \$ \{ | $)
      ) |
      \$ \{ $symw \} |
      \$ $symw $bpar |
      \$ $symw |
      \$ $bpar |
      (?: [^\$]*\$+[^\$]*)
    )"))

(defn build-expr-interpolated [node]
  (let [expr (build-expr node)
        lst1 (:Lst expr)
        lst2 (get-in lst1 [0 :Lst])
        expr (if (and (= 1 (count lst1)) (get-in lst1 [0 :dot]))
              (first lst1)
              (if (and (= 1 (count lst1)) lst2) (Lst lst2) expr))]
    expr))

(defn build-interpolated [string]
  (let [parts (re-seq re-interpolated-string string)
        exprs (map
                #(condp (fn [re s] (re-matches re s)) %1
                   (re/re #"\$\{$symw\}")
                   (Sym (subs %1 2 (dec (count %1))))

                   (re/re #"\$$symw$bpar")
                   (build-expr {:expr (subs %1 1)})

                   (re/re #"\$$symw")
                   (Sym (subs %1 1))

                   (re/re #"\$$bpar")
                   (build-expr-interpolated {:expr (subs %1 1)})

                   (Str (str/replace %1 #"\\(\$)" "$1")))
                parts)]
    (if (= 1 (count exprs))
      (first exprs)
      (Lst (cons (Sym 'str) exprs)))))

(defn build-dq-string [string]
  (-> string
    (str/replace #"\\\$" "$")
    (str/replace #"\\ " " ")
    (str/replace #"\\b" "\b")
    (str/replace #"\\f" "\f")
    (str/replace #"\\n" "\n")
    (str/replace #"\\r" "\r")
    (str/replace #"\\t" "\t")
    Str))

(defn build-xstr [node]
  (let [string (:xstr node)]
    (if (re-find #"\$[\{\(a-zA-Z]" string)
      (build-interpolated string)
      (build-dq-string string))))

(reset! yamlscript.global/build-xstr build-xstr)

(defn build-node [node]
  (let [anchor (:& node)
        ytag (:! node)
        node (dissoc node :! :&)
        [tag] (first node)
        node (case tag
               nil nil
               :xmap (build-xmap node)
               :fmap (build-fmap node)
               :expr (build-expr node)
               :xstr (build-xstr node)
               :str (Str (:str node))
               :def (build-def node)
               :ali node
               :Ali node
               :map (build-map node)
               :seq (build-vec node)
               :int (Int (:int node))
               :flt (Flt (:flt node))
               :bln (Bln (:bln node))
               :key (Key (subs (:key node) 1))
               :clj (Clj (:clj node))
               :nil (Nil)
               (die "Don't know how to build node: " node))
        node (if anchor
               (assoc node :& anchor)
               node)]
    (if ytag
      (assoc node :! ytag)
      node)))

(comment
  )
