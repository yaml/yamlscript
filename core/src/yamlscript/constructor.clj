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
  maybe-call-main)

(defn construct
  "Construct resolved YAML tree into a YAMLScript AST."
  [node]
  (->> node
    construct-node
    (#(if (vector? %)
        %
        [%]))
    (hash-map :Top)
    declare-undefined
    maybe-call-main))

(defn construct-call [pair]
  (let [pair (if (= '=> (get-in pair [0 :Sym]))
               (rest pair)
               pair)
        pair (if (= 1 (count pair))
               (first pair)
               (Lst (flatten [pair])))]
    pair))

(defn construct-err [o]
  (throw (Exception. (str "Can't construct " o))))

(defn construct-ysm [node]
  (let [nodes (:ysm node)
        pairs (partition 2 nodes)
        construct-side #(if (vector? %)
                          (vec (map construct-node %))
                          (construct-node %))
        node (loop [pairs pairs, new []]
               (if (seq pairs)
                 (let [[[lhs rhs] & pairs] pairs
                       lhs (construct-side lhs)
                       rhs (construct-side rhs)
                       pair [lhs rhs]
                       new (conj new (construct-call pair))]
                   (recur pairs, new))
                 new))]
    node))

#_(defn construct-ysm [node]
  (let [pairs (:ysm node)]
    (->> pairs
      (reduce
        #(conj %1
           (if (vector? %2)
             (map construct-node %2)
             (construct-node %2)))
        [])
      (partition 2)
      (map vec)
      (reduce #(conj %1
                 ((some-fn
                    construct-call
                    construct-err)
                  %2))
        [])
      (#(if (= 1 (count %))
          (first %)
          (Lst (flatten [(Sym "do") %])))))))


(defn construct-node [node]
  (let [[[key]] (seq node)]
    (case key
      :ysm (construct-ysm node)
      ,    node)))

;;------------------------------------------------------------------------------
;; Fix-up functions
;;------------------------------------------------------------------------------
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
      (update-in node [:Top]
        #(vec (concat [form] %)))
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
