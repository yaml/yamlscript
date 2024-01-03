;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.builder is responsible parsing all the !ysx nodes into
;; YAMLScript AST nodes.

(ns yamlscript.builder
  (:use yamlscript.debug)
  (:require
   [clojure.string :as str]
   [yamlscript.ast :refer :all]
   [yamlscript.re :as re]
   [yamlscript.ysreader :as ysreader]))

(declare build-node)

(defn build
  "Parse all the !ysx nodes into YAMLScript AST nodes."
  [node] (build-node node))

(defn build-ys-mapping [node]
  (->> node
    first
    val
    (map build-node)
    (hash-map :ysm)))

;; XXX This might belong in the transformer
(defn optimize-ys-expression [node]
  (if (= '=> (get-in node [:Lst 0 :Sym]))
    (get-in node [:Lst 1])
    node))

(defn build-ys-expression [node]
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

(defn build-interpolated-string [node]
  (let [string (:ysi node)
        parts (re-seq re-interpolated-string string)
        exprs (map
                #(cond
                   (re-matches (re/re #"\$$symw$bpar") %)
                   (build-ys-expression {:ysx (subs % 1)})
                   ,
                   (re-matches (re/re #"\$$symw") %)
                   (Sym (subs % 1))
                   ,
                   (re-matches (re/re #"\$$bpar") %)
                   (build-ys-expression {:ysx (subs % 1)})
                   ,
                   :else
                   (Str (str/replace % #"\\(\$)" "$1")))
                parts)]
    (if (= 1 (count exprs))
      (first exprs)
      (Lst (cons (Sym 'str) exprs)))))

(defn build-node [node]
  (let [[tag] (first node)]
    (case tag
      :ysm (build-ys-mapping node)
      :ysx (build-ys-expression node)
      :ysi (build-interpolated-string node)
      :str (Str (:str node))
      :map (build-map node)
      :seq (build-vec node)
      :int (Int (:int node))
      :flt (Flt (:flt node))
      :bln (Bln (:bln node))
      :nil (Nil)
      (throw (Exception. (str "Don't know how to build node: " node))))))

(comment
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
