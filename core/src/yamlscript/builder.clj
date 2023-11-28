;; Copyright 2023 Ingy dot Net
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

(defn build-ysm [node]
  (let [ysm (-> node first val)]
    (->> ysm
      (map build-node)
      (hash-map :ysm))))

(defn build-ysx [node]
  (let [string (-> node first val)]
    (if (= string "")
      {:Empty nil}
      (let [expr (ysreader/read-string string)]
        (if expr
          expr
          {:Empty nil})))))

(defn build-map [node]
  (Map (map build-node (:map node))))

(defn build-vec [node]
  (Vec (map build-node (:seq node))))

(def re-ysi
  (re/re
    #"(?x)
    (?:
      \$ $symw $bpar |
      \$ $symw |
      \$ $bpar |
      .+?(?= \$ $symw | \$ $bpar | $)
    )"))

(defn build-ysi [node]
  (let [string (:ysi node)
        parts (re-seq re-ysi string)
        exprs (map
                #(cond
                   (re-matches (re/re #"\$$symw$bpar") %)
                   (build-ysx {:ysx (subs % 1)})
                   (re-matches (re/re #"\$$symw") %)
                   (Sym (subs % 1))
                   (re-matches (re/re #"\$$bpar") %)
                   (build-ysx {:ysx (subs % 1)})
                   :else
                   (Str %))
                parts)]
    (if (= 1 (count exprs))
      (first exprs)
      (Lst (cons (Sym 'str) exprs)))))

(defn build-node [node]
  (let [[key] (first node)]
    (case key
      :ysm (build-ysm node)
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
