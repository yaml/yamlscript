;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.constructor is responsible for converting the YAMLScript AST
;; into a Clojure AST.

(ns yamlscript.constructor
  (:use yamlscript.debug)
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [yamlscript.ast :refer :all]))

(declare
  construct-node
  declare-undefined
  maybe-call-main
  check-let-bindings)

(defn construct
  "Construct resolved YAML tree into a YAMLScript AST."
  [node]
  (let [ctx {:lvl 0 :defn false}]
    (->> node
      (#(construct-node % ctx))
      (#(if (vector? %)
          %
          [%]))
      (hash-map :Top)
      declare-undefined
      maybe-call-main)))

(defn construct-call [pair]
  (let [pair (if (= '=> (get-in pair [0 :Sym]))
               (rest pair)
               pair)
        pair (if (= 1 (count pair))
               (first pair)
               (Lst (flatten [pair])))]
    pair))

(defn construct-ysm [node ctx]
  (let [nodes (:ysm node)
        pairs (vec (map vec (partition 2 nodes)))
        construct-side (fn [n c] (if (vector? n)
                                   (vec (map #(construct-node % c) n))
                                   (construct-node n c)))
        node (loop [pairs pairs, new []]
               (if (seq pairs)
                 (let [pairs (if (> (:lvl ctx) 1)
                               (check-let-bindings pairs ctx)
                               pairs)
                       [[lhs rhs] & pairs] pairs
                       lhs (construct-side lhs ctx)
                       rhs (construct-side rhs ctx)
                       pair [lhs rhs]
                       new (conj new (construct-call pair))]
                   (recur pairs, new))
                 new))]
    node))

(defn construct-node [node ctx]
  (when (vector? ctx) (throw (Exception. "ctx is a vector")))
  (let [[[key]] (seq node)
        ctx (update-in ctx [:lvl] inc)]
    (case key
      :ysm (construct-ysm node ctx)
      ,    node)))

;;------------------------------------------------------------------------------
;; Fix-up functions
;;------------------------------------------------------------------------------
(defn apply-let-bindings [lets rest ctx]
  [[(Sym "let")
    (vec
      (concat
        [(Vec (->>
                lets
                flatten
                (filter #(not= {:Sym 'def} %))
                ;; Handle RHS is mapping
                (partition 2)
                (map #(let [[k v] %]
                        (if (:ysm v)
                          [k (Lst (get-in
                                    (construct-ysm v ctx)
                                    [0 :Lst]))]
                          %)))
                (mapcat identity)
                vec))]
        (construct-ysm {:ysm (mapcat identity rest)} ctx)))]])

(mapcat identity [{:Sym 'b} {:Lst [{:Sym 'c} {:Sym 'd}]}])

(defn check-let-bindings [pairs ctx]
  (let [[lets rest]
        (map vec
          (split-with
            #(= 'def (get-in % [0 0 :Sym]))
            pairs))]
    (if (seq lets)
      (apply-let-bindings lets rest ctx)
      pairs)))

(defn get-declares [node defns]
  (let [declare (atom {})
        defined (atom {})]
    (walk/prewalk
      #(let [defn-name (when (= 'defn (get-in % [:Lst 0 :Sym]))
                         (get-in % [:Lst 1 :Sym]))
             sym-name (get-in % [:Sym])]
         (when defn-name (swap! defined assoc defn-name true))
         (when (and sym-name
                 (get defns sym-name)
                 (not (get @defined sym-name)))
           (swap! declare assoc sym-name true))
         %)
      node)
    @declare))

(defn declare-undefined [node]
  (let [defn-names (map #(get-in % [:Lst 1 :Sym])
                     (filter #(= 'defn (get-in % [:Lst 0 :Sym]))
                       (rest (get-in node [:Top]))))
        defn-names (zipmap defn-names (repeat true))
        declares (map Sym
                   (keys (get-declares node defn-names)))
        form (Lst (cons (Sym 'declare) declares))]
    (if (seq declares)
      (if (= 'ns (get-in node [:Top 0 :Lst 0 :Sym]))
        (update-in node [:Top]
          #(vec (concat [(first %)] [form] (rest %))))
        (update-in node [:Top]
          #(vec (concat [form] %))))
      node)))

(def call-main
  (Lst [(Sym 'apply)
        (Sym 'main)
        (Sym 'ARGV)]))

(defn maybe-call-main [node]
  (let [need-call-main (atom false)]
    (walk/prewalk
      #(let [main (and (= 'defn (get-in % [:Lst 0 :Sym]))
                    (= 'main (get-in % [:Lst 1 :Sym])))]
         (when main (reset! need-call-main true))
         %)
      node)
    (if @need-call-main
      (update-in node [:Top] conj call-main)
      node)))

(comment
  (construct
    {:ysm
     ([{:Sym 'defn} {:Sym 'foo} {:Vec [{:Sym 'x}]}]
      {:ysm
       '([{:Sym 'def} {:Sym 'y}]
         {:Lst ({:Sym 'add} {:Sym 'x} {:Int 1})}
         [{:Sym 'def} {:Sym 'x}]
         {:Lst [{:Sym 'times} {:Sym 'y} {:Sym 'x}]}
         {:Sym '=>}
         {:Sym 'y})})})
  (construct :Nil)
  (construct {:do [{:Sym 'a} [{:Sym 'b} {:Sym 'c}]]})
  )
