;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.printer is responsible for serializing YS Clojure AST into
;; Clojure code.

(ns yamlscript.printer
  (:require
   [clojure.string :as str]
   [yamlscript.common]
   [yamlscript.global])
  (:refer-clojure :exclude [print]))

(def string-escape
  {\\ "\\\\"
   \" "\\\""
   \backspace "\\b"
   \formfeed "\\f"
   \newline "\\n"
   \return "\\r"
   \tab "\\t"})

(defn pr-string [s]
  (str/escape s string-escape))

(def regex-escape
  {\" "\\\""})

(defn pr-regex [s]
  (-> s
    (str/escape regex-escape)))

(defn pr-symbol [s]
  (case s
    "ERR" "*err*"
    "IN" "*in*"
    "NS" "*ns*"
    "OUT" "*out*"
    "=~" "=--"
    "!~" "!--"
    "=~~" "=---"
    "!~~" "!---"
    "==" "="
    "===" "=="
    "=" (die
          "Operator '=' is not allowed in YS.\n"
          "Use '==' for equality comparison.")
    , s))

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
      :Set (str
             "#{"
             (str/join " " (map print-node val))
             "}")
      :Map (let [[start end] (if (:unordered @yamlscript.global/opts)
                               ["{" "}"]
                               ["(% " ")"])]
             (str
               start
               (str/join ", " (->> val
                                (partition 2)
                                (map #(str
                                        (print-node (first %1))
                                        " "
                                        (print-node (second %1))))))
               end))
      :Str (str \" (pr-string val) \")
      :Rgx (str \# \" (pr-regex val) \")
      :Chr (str "\\" val)
      :QSym (str "'" val)
      :Qts (str "'" val)
      :Spc (str/replace val #"::" ".")
      :Sym (pr-symbol (str val))
      :Tok (str val)
      :Tup (apply str (map print-node val))
      :Key (str val)
      :Int (str val)
      :Flt (str val)
      :Bln (str val)
      :Clj (with-out-str (clojure.core/print val))
      :Nil "nil"
      ,    (die "Unknown AST node type:" node))))

(defn print
  "Render a YS AST as Clojure code."
  [node]
  (let [list (or (:Top node) [node])
        code (->> list
               (map print-node)
               (apply str))]
    code))

(comment
  )
