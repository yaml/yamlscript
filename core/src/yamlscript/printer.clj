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
                                      (print-node (first %1))
                                      " "
                                      (print-node (second %1))))))
             "}")
      :Str (str \" (pr-string val) \")
      :Rgx (str \# \" (pr-regex val) \")
      :Chr (str "\\" val)
      :Spc (str/replace val #"::" ".")
      :Sym (str val)
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
  (let [list (:Top node)
        code (->> list
               (map print-node)
               (apply str))]
    code))

(comment
  www
  (print :Empty)
  (read-string "
(defmacro each [bindings & body]
  `(do
     (doall (for [~@bindings] (do ~@body)))
     nil)))")
  (print
    {:Lst [{:Sym 'a} {:Sym 'b} {:Sym 'c}]})
  (print {:Map [{:Str "foo"} {:Str "\\a"}]})
  )
