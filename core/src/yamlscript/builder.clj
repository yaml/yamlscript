;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.builder is responsible parsing all the !exp nodes into
;; YAMLScript AST nodes.

(ns yamlscript.builder
  (:require
   [clojure.string :as str]
   [yamlscript.debug :refer [www]]
   [yamlscript.ast :refer
    [Bln Flt Int Lst Map Nil Str Sym Vec]]
   [yamlscript.re :as re]
   [yamlscript.util :refer [if-lets when-lets]]
   [yamlscript.ysreader :as ysreader]))

(declare build-node)

(defn build
  "Parse all the !exp nodes into YAMLScript AST nodes."
  [node] (build-node node))

(defn build-defn-defaults [name doc args body]
  (when-lets [args (:Vec args)
              dargs (filter #(vector? (:Sym %1)) args)
              _ (seq dargs)
              dmap (reduce (fn [m {[k v] :Sym}] (assoc m {:Sym k} v)) {} dargs)
              defn (Sym 'defn)
              args (map Sym
                     (map #(or (get-in % [:Sym 0])
                             (get % :Sym)) args))
              n-args (count args)
              n-dargs (count dargs)
              main (Lst [(Vec args) body])
              bodies (reduce
                       (fn [bodies i]
                         (let [nargs (->> args (take i) vec)
                               dargs (->> args (drop i) vec)
                               dvals (->> dargs (map #(get dmap %)) vec)
                               body (Lst [(Vec nargs)
                                          (Lst (->> nargs
                                                 (cons name)
                                                 vec
                                                 (#(concat %1 dvals))))])]
                           (conj bodies body)))
                       [main]
                       (range (dec n-args) (- n-args n-dargs 1) -1))]
    [[defn name doc] bodies]))

(defn build-defn-single [name doc args body]
  (when-let [defn (Sym 'defn)]
    [[defn name doc args] body]))

(defn build-defn [{key :defn} val]
  (let [[_ name args] (re-matches re/dfnk key)
        name (Sym name)
        args (when args
               (let [args (build-node {:exp args})
                     args (if (map? args) [args] args)]
                 (Vec args)))
        body (build-node val)
        [doc body] (if-lets [[key val & nodes] (:pairs body)
                             [doc body]
                             (cond (and (= '=> (:Sym key))
                                     (some val [:Str :str :vstr]))
                                   [val {:pairs nodes}]
                                   ,
                                   (and (nil? val)
                                     (some key [:Str :str :vstr]))
                                   [key {:pairs nodes}])]
                     [doc body]
                     [nil body])]
    (or (build-defn-defaults name doc args body)
        (build-defn-single name doc args body))))

(defn build-pairs [{pairs :pairs}]
  (let [nodes (reduce
                (fn [nodes [key val]]
                  (let [[key val] (if (:defn key)
                                    (build-defn key val)
                                    [(build-node key) (build-node val)])]
                    (conj nodes key val)))
                []
                (partition 2 pairs))]
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
  (let [string (-> node first val)]
    (when-not (empty? string)
      (let [exp (ysreader/read-string string)]
        (when exp
          (optimize-ys-expression exp))))))

(defn build-def [{node :def}]
  [(Sym "def")
   (Sym (str/replace node #"\ +=$" ""))])

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
        (?: \\\$ | [^\$] )+?
        (?= \$ $symw | \$ $bpar | $)
      ) |
      \$ $symw $bpar |
      \$ $symw |
      \$ $bpar
    )"))

(defn build-vstr [node]
  (let [string (:vstr node)
        parts (re-seq re-interpolated-string string)
        exprs (map
                #(cond
                   (re-matches (re/re #"\$$symw$bpar") %1)
                   (build-exp {:exp (subs %1 1)})
                   ,
                   (re-matches (re/re #"\$$symw") %1)
                   (Sym (subs %1 1))
                   ,
                   (re-matches (re/re #"\$$bpar") %1)
                   (build-exp {:exp (subs %1 1)})
                   ,
                   :else
                   (Str (str/replace %1 #"\\(\$)" "$1")))
                parts)]
    (if (= 1 (count exprs))
      (first exprs)
      (Lst (cons (Sym 'str) exprs)))))

(defn build-node [node]
  (let [[tag] (first node)]
    (case tag
      nil nil
      :pairs (build-pairs node)
      :forms (build-forms node)
      :exp (build-exp node)
      :vstr (build-vstr node)
      :str (Str (:str node))
      :def (build-def node)
      :map (build-map node)
      :seq (build-vec node)
      :int (Int (:int node))
      :flt (Flt (:flt node))
      :bln (Bln (:bln node))
      :nil (Nil)
      (throw (Exception. (str "Don't know how to build node: " node))))))

(comment
  www

  (build {:exp ""})

  (re-seq #"(?:bar|.+?(?=bar|$))" "foo bar baz")

  (build {:exp "; comment (foo bar)"})

  (build {:pairs [{:exp "println"} {:str "Hello"}]})

  (build {:pairs [{:exp "inc"} {:exp "(6 * 7)"}]})

  (build {:pairs [{:exp "a"} {:exp "b c"}]})

  (build
    {:pairs [{:exp "a"}
             {:exp "b c"}]})
  )
