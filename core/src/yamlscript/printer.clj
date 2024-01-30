;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.printer is responsible for serializing YAMLScript Clojure AST
;; into Clojure code.

(ns yamlscript.printer
  (:require
   [clojure.string :as str]
   [yamlscript.debug :refer [www]])
  (:refer-clojure :exclude [print]))

(def string-escape
  {\" "\\\""
   \newline "\\n"})

(defn pr-string [s]
  (-> s
    (str/escape string-escape)))

(def regex-escape
  {\" "\\\""})

(defn pr-regex [s]
  (-> s
    (str/escape regex-escape)))

(defn print-node [node]
  (let [node (if (keyword? node) {node true} node)
        [type val] (first node)]
    (case type
      nil  ""
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
                                      (print-node (first %1))
                                      " "
                                      (print-node (second %1))))))
             "}")
      :Str (str \" (pr-string val) \")
      :Rgx (str \# \" (pr-regex val) \")
      :Chr (str "\\" val)
      :Spc (str/replace val #"::" ".")
      :Sym (str (str/replace val #"~" "--"))
      :Tok (str val)
      :Key (str val)
      :Int (str val)
      :Flt (str val)
      :Bln (str val)
      :Nil "nil"
      ,     (throw
              (Exception. (str "Unknown AST node type:"
                            node))))))

(defn print
  "Render a YAMLScript AST as Clojure code."
  [node]
  (let [list (or (:Top node) [node])
        code (->> list
               (map print-node)
               (apply str))]
    code))

(defn print-top [& node]
  (print {:Top node}))

(comment
  www
  (print '{:Top
           [{:Lst
             [{:Sym defn}
              {:Sym foo}
              nil
              {:Lst [{:Vec [{:Sym a}]} {:Lst [{:Sym say} {:Sym a}]}]}
              {:Lst [{:Vec []} {:Lst [{:Sym foo} {:Int 1}]}]}]}]})
  )
