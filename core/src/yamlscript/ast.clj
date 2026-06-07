;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.ast library defines the YS AST nodes.

(ns yamlscript.ast
  (:require
   [clojure.string :as str]
   [yamlscript.common])
  (:refer-clojure :exclude [Vec]))

(defn Lst
  "Wrap forms in a list AST node."
  [list] {:Lst (vec list)})

(defn Form
  "Return a single map form as-is, otherwise wrap forms in a list."
  [list]
  (if-lets [_ (= 1 (count list))
            node (first list)
            _ (map? node)]
    node
    (Lst list)))

(defn Vec
  "Wrap forms in a vector AST node."
  [list] {:Vec (vec list)})

(defn Tup
  "Wrap forms in a tuple AST node used for grouped tokens."
  [list] {:Tup (vec list)})

(defn Set
  "Wrap forms in a set AST node."
  [list] {:Set (vec list)})

(defn Map
  "Wrap alternating key/value forms in an ordered map AST node."
  [list]
  (if (even? (count list))
    {:Map (->> list
            (apply array-map)
            (mapcat seq)
            vec)}
    (die "Odd number of elements in map")))

(defn Spc
  "Wrap a namespace token in a special namespace AST node."
  [s] {:Spc (symbol s)})

(defn Sym
  "Wrap a symbol, or symbol with default value, in a symbol AST node."
  ([s] {:Sym (symbol s)})
  ([s d] {:Sym [(symbol s) d]}))

(defn QSym
  "Wrap a symbol that should print as a quoted symbol."
  [s] {:QSym s})

(defn Qts
  "Wrap a value that should print as a quoted literal."
  [s] {:Qts (str s)})

(defn Chr
  "Wrap a character literal token in a character AST node."
  [s] {:Chr (symbol s)})

(defn Num
  "Parse and wrap an integer or arbitrary numeric literal."
  [s]
  (if (re-matches #"(?:0|[1-9][0-9]{0,17})" s)
    ;; Positive integers that fit in Long will be represented as :Int
    {:Int (parse-long s)}
    {:Num (read-string s)}))

(def special
  {".inf" "Infinity"
   ".Inf" "Infinity"
   ".INF" "Infinity"
   "+.inf" "Infinity"
   "+.Inf" "Infinity"
   "+.INF" "Infinity"
   "-.inf" "-Infinity"
   "-.Inf" "-Infinity"
   "-.INF" "-Infinity"
   ".nan" "NaN"
   ".NaN" "NaN"
   ".NAN" "NaN"})

(defn Flt
  "Parse and wrap a floating-point literal, including YAML specials."
  [s]
  (let [s (str/replace s
            #"^([-+]?(?:\.inf|\.Inf|\.INF|\.nan|\.NaN|\.NAN))"
            (fn [[_ k]]
              (get special k)))]
    {:Flt (parse-double s)}))

(defn Str
  "Wrap a value in a string AST node."
  [s] {:Str (str s)})

(defn Key
  "Wrap a token in a Clojure keyword AST node."
  [s] {:Key (keyword s)})

(defn Tok
  "Wrap a token that should print without extra conversion."
  [s] {:Tok (str s)})

(defn Rgx
  "Wrap a regular-expression literal body."
  [s] {:Rgx (str s)})

(defn Bln
  "Parse and wrap a YAML boolean scalar."
  [b]
  (if (re-matches #"(true|True|TRUE)" b)
    {:Bln true}
    {:Bln false}))

(defn Nil
  "Return the YAMLScript nil AST node."
  []
  {:Nil nil})

(defn Clj
  "Wrap raw Clojure data so the printer emits it directly."
  [c]
  {:Clj c})

(def operators
  {(Sym '.)   (Sym '_dot_)
   (Sym '..)  (Sym 'rng)
   (Sym '...) (Sym 'range)
   (Sym '+)   (Sym 'add+)
   (Sym '-)   (Sym 'sub+)
   (Sym '*)   (Sym 'mul+)
   (Sym '/)   (Sym 'div+)
   (Sym "//") (Sym 'quot)
   (Sym '!=)  (Sym 'not=)
   (Sym '||)  (Sym 'or)
   (Sym '&&)  (Sym 'and)
   (Sym '|||) (Sym 'or?)
   (Sym '&&&) (Sym 'and?)
   (Sym '%)   (Sym 'rem)
   (Sym '%%)  (Sym 'mod)
   (Sym '**)  (Sym 'pow)})

(comment
  [(Lst [1 2 3])
   (Vec [1 2 3])
   (Map [1 2 3 4 5 6])
   (Bln "true")
   (Bln "false")
   (Nil)
   (Sym "foo")
   (Chr "a")
   (Num "123")
   (Num "1.23")
   (Str "foo")
   (Key "foo")]
  )
