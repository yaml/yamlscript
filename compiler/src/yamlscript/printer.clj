;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.printer is responsible for serializing YAMLScript Clojure AST
;; into Clojure code.

(ns yamlscript.printer
  (:use yamlscript.debug)
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.pprint :as pp]
   [yamlscript.builder :as builder]
   [clj-yaml.core :as yaml])
  (:refer-clojure :exclude [print]))

(def string-escape
  {\\ "\\\\"
   \" "\\\""})

(defn print-node [node]
  (let [node (if (keyword? node)
               {node true}
               node)
        [type val] (first node)]
    (case type
      :Empty ""
      :Lst (str
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
      :Str (str \" (str/escape val string-escape) \")
      :Chr (str "\\" val)
      :Sym (str val)
      :Key (str val)
      :Int (str val)
      :Flt (str val)
      :Bln (str val)
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
  (if (= 'do (get-in node [:Lst 0 :Sym]))
    (let [nodes (rest (get-in node [:Lst]))]
      (->> nodes
        (map print-node)
        (str/join "\n")
        (#(str % "\n"))))
    (let [string (print-node node)]
      (if (= string "")
        ""
        (-> string edn/read-string pretty-format)))))

(comment
  (print :Empty)
  (print
    {:Lst [{:Sym 'a} {:Sym 'b} {:Sym 'c}]})
  (print {:Map [{:Str "foo"} {:Str "\\a"}]})
  )
