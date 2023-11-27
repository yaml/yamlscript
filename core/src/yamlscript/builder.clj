;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.builder is responsible parsing all the !ysx nodes into
;; YAMLScript AST nodes.

(ns yamlscript.builder
  (:use yamlscript.debug)
  (:require
   [clojure.string :as str]
   [yamlscript.ast :refer :all]
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
      (or (ysreader/read-string string)
        {:Empty nil}))))

(defn build-map [node]
  (Map (map build-node (:map node))))

(defn build-vec [node]
  (Vec (map build-node (:seq node))))

(defn build-node [node]
  (let [[key] (first node)]
    (case key
      :ysm (build-ysm node)
      :ysx (build-ysx node)
      :ysi (Str (:ysi node))
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

  (build {:ysx "; comment (foo bar)"})

  (build {:ysm [{:ysx "println"} {:str "Hello"}]})

  (build {:ysm [{:ysx "inc"} {:ysx "(6 * 7)"}]})

  (build {:ysm [{:ysx "a"} {:ysx "b c"}]})

  (build
    {:ysm [{:ysx "a"}
           {:ysx "b c"}]})
  )
