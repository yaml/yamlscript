;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.builder is responsible parsing all the !exp nodes into
;; YAMLScript AST nodes.

(ns yamlscript.builder
  (:require
   [clojure.string :as str]
   [yamlscript.ast :refer
    [Bln Clj Flt Form Int Key Lst Map Nil Str Sym Vec]]
   [yamlscript.re :as re]
   [yamlscript.util :refer [die if-lets when-lets]]
   [yamlscript.ysreader :as ysreader]))

(declare build-node)

(defn build
  "Parse all the !exp nodes into YAMLScript AST nodes."
  [node] (build-node node))

(defn build-from-string [string]
  (require
    '[yamlscript.parser]
    '[yamlscript.composer]
    '[yamlscript.resolver]
    '[yamlscript.builder])
  (-> string
    yamlscript.parser/parse
    yamlscript.composer/compose
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
                                                 (->> (cons name))
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
        name (Sym name)
        args (when args
               (let [args (fix-args args)
                     args (build-node {:exp args})
                     args (if (map? args) [args] args)]
                 (Vec args)))
        body (build-node
               (if-lets [_ (nil? args)
                         pairs (:pairs val)]
                 (let [pairs (loop [[k v & pairs] pairs new []]
                               (if (nil? v)
                                 new
                                 (let [args (:exp k)
                                       args (fix-args args)
                                       args {:exp args}]
                                   (recur pairs (conj new args v)))))]
                   {:pairs pairs})
                 val))
        [doc body] (if-lets [[key val & nodes] (:pairs body)
                             [doc body] (when (and (nil? key) (:Str val))
                                          [val {:pairs nodes}])]
                     [doc body]
                     [nil body])]
    (or
      (build-defn-defaults name doc args body kind)
      (build-defn-single name doc args body kind))))

(defn build-form-pair [key val]
  (let [key (:form key)]
    [{:form (build-node key)} (build-node val)]))

(defn build-pair [nodes [key val]]
  (let [[key val] (cond
                    (:defn key) (build-defn key val)
                    (:fn key) (build-defn key val)
                    (:form key) (build-form-pair key val)
                    :else [(build-node key) (build-node val)])]
    (conj nodes key val)))

(defn build-pairs [{pairs :pairs}]
  (let [nodes (reduce build-pair [] (partition 2 pairs))]
    {:pairs nodes}))

(defn build-forms [node]
  (->> node
    first
    val
    (map build-node)
    (hash-map :forms)))

;; XXX This might belong in the transformer
(defn optimize-ys-expression [node]
  (if (= '=> (get-in node [:Lst 0 :Sym]))
    (get-in node [:Lst 1])
    node))

(defn build-exp [node]
  (let [string (:exp node)]
    (when-not (empty? string)
      (let [exp (ysreader/read-string string)]
        (when exp
          (optimize-ys-expression exp))))))

(defn build-def [{node :def}]
  (if-lets [m (re-matches re/defk node)
            v [(Sym "def")
               (Sym (m 1))]]
    (if (empty? (m 2))
      v
      (conj v (Sym (m 2))))))

(defn build-map [node]
  (Map (map build-node (:map node))))

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

(defn build-exp-interpolated [node]
  (let [exp (build-exp node)
        lst1 (:Lst exp)
        lst2 (get-in lst1 [0 :Lst])
        exp (if (and (= 1 (count lst1)) (get-in lst1 [0 :dot]))
              (first lst1)
              (if (and (= 1 (count lst1)) lst2) (Lst lst2) exp))]
    exp))

(defn build-interpolated [string]
  (let [parts (re-seq re-interpolated-string string)
        exprs (map
                #(cond
                   (re-matches (re/re #"\$\{$symw\}") %1)
                   (Sym (subs %1 2 (dec (count %1))))

                   (re-matches (re/re #"\$$symw$bpar") %1)
                   (build-exp {:exp (subs %1 1)})

                   (re-matches (re/re #"\$$symw") %1)
                   (Sym (subs %1 1))

                   (re-matches (re/re #"\$$bpar") %1)
                   (build-exp-interpolated {:exp (subs %1 1)})

                   :else
                   (Str (str/replace %1 #"\\(\$)" "$1")))
                parts)]
    (if (= 1 (count exprs))
      (first exprs)
      (Lst (cons (Sym 'str) exprs)))))

(defn build-dq-string [string]
  (-> string
    (str/replace #"\\\$" "$")
    (str/replace #"\\ " " ")
    (str/replace #"\\n" "\n")
    Str))

(defn build-vstr [node]
  (let [string (:vstr node)]
    (if (re-find #"\$[\{\(a-zA-Z]" string)
      (build-interpolated string)
      (build-dq-string string))))

(reset! yamlscript.util/build-vstr build-vstr)

(defn build-node [node]
  (let [anchor (:& node)
        ytag (:! node)
        node (dissoc node :! :&)
        [tag] (first node)
        node (case tag
               nil nil
               :pairs (build-pairs node)
               :forms (build-forms node)
               :exp (build-exp node)
               :vstr (build-vstr node)
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
