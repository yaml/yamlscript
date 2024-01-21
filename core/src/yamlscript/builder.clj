;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.builder is responsible parsing all the !ysx nodes into
;; YAMLScript AST nodes.

(ns yamlscript.builder
  (:require
   [clojure.string :as str]
   [yamlscript.debug :refer [www]]
   [yamlscript.ast :refer
    [Bln Flt Int Lst Map Nil Str Sym Vec]]
   [yamlscript.re :as re]
   [yamlscript.ysreader :as ysreader]))

(declare build-node)

(defn build
  "Parse all the !ysx nodes into YAMLScript AST nodes."
  [node] (build-node node))

(defn build-ysm [node]
  (->> node
    first
    val
    (map build-node)
    (hash-map :ysm)))

(defn build-ysg [node]
  (->> node
    first
    val
    (map build-node)
    (hash-map :ysg)))

;; XXX This might belong in the transformer
(defn optimize-ys-expression [node]
  (if (= '=> (get-in node [:Lst 0 :Sym]))
    (get-in node [:Lst 1])
    node))

(defn build-ysx [node]
  (let [string (-> node first val)]
    (if (= string "")
      {:Empty nil}
      (let [expr (ysreader/read-string string)]
        (if expr
          (optimize-ys-expression expr)
          {:Empty nil})))))

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

(defn build-ysi [node]
  (let [string (:ysi node)
        parts (re-seq re-interpolated-string string)
        exprs (map
                #(cond
                   (re-matches (re/re #"\$$symw$bpar") %1)
                   (build-ysx {:ysx (subs %1 1)})
                   ,
                   (re-matches (re/re #"\$$symw") %1)
                   (Sym (subs %1 1))
                   ,
                   (re-matches (re/re #"\$$bpar") %1)
                   (build-ysx {:ysx (subs %1 1)})
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
      :ysm (build-ysm node)
      :ysg (build-ysg node)
      :ysx (build-ysx node)
      :ysi (build-ysi node)
      :str (Str (:str node))
      :map (build-map node)
      :seq (build-vec node)
      :int (Int (:int node))
      :flt (Flt (:flt node))
      :bln (Bln (:bln node))
      :nil (Nil)
      (throw (Exception. (str "Don't know how to build node: " node))))))

(comment
  www

  (build {:ysx ""})

  (re-seq #"(?:bar|.+?(?=bar|$))" "foo bar baz")

  (build {:ysx "; comment (foo bar)"})

  (build {:ysm [{:ysx "println"} {:str "Hello"}]})

  (build {:ysm [{:ysx "inc"} {:ysx "(6 * 7)"}]})

  (build {:ysm [{:ysx "a"} {:ysx "b c"}]})

  (build
    {:ysm [{:ysx "a"}
           {:ysx "b c"}]})
  )
