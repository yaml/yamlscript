;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.printer
  (:use yamlscript.debug)
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.pprint :as pp]
   [yamlscript.builder :as builder]
   [clj-yaml.core :as yaml])
  (:refer-clojure :exclude [print]))

(defn print-node [node]
  (let [node (if (keyword? node)
               {node true}
               node)
        [type val] (first node)]
    (case type
      :Empty ""
      :List (str
              "("
              (str/join " " (map print-node val))
              ")")
      :Vec (str
             "["
             (str/join " " (map print-node val))
             "]")
      :Map (str
             "{"
             (str/join ", " (->> val
                              (partition 2)
                              (map #(str
                                      (print-node (first %))
                                      " "
                                      (print-node (second %))))))
             "}")
      :Str (str \" val \")
      :Char (str "\\" val)
      :Sym (str val)
      :Key (str val)
      :LNum (str val)
      :DNum (str val)
      :True "true"
      :False "false"
      :Nil "nil"
      ,     (throw
              (Exception. (str "Unknown AST node type:"
                            node))))))

(defn pretty-format [s]
  (let [s (with-out-str (pp/write s))]
    (str s "\n")))

(defn print
  "Render a YAMLScript AST as Clojure code."
  [node]
  (let [string (print-node node)]
    (if (= string "")
      ""
      (-> string edn/read-string pretty-format))))

(comment
  (print :Empty)
  (print
    {:List [{:Sym 'a} {:Sym 'b} {:Sym 'c}]})
  )
