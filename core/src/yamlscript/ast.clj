;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.ast library defines the YAMLScript AST nodes.

(ns yamlscript.ast
  (:require
   [yamlscript.util :refer [die if-lets]])
  (:refer-clojure :exclude [Vec]))

(defn Lst [list] {:Lst (vec list)})

(defn Form [list]
  (if-lets [_ (= 1 (count list))
            node (first list)
            _ (map? node)]
    node
    (Lst list)))

(defn Vec [list] {:Vec (vec list)})

(defn Tup [list] {:Tup (vec list)})

(defn Set [list] {:Set (vec list)})

(defn Map [list]
  (if (even? (count list))
    {:Map (->> list
            (apply array-map)
            (mapcat seq)
            vec)}
    (die "Odd number of elements in map")))

(defn Spc [s] {:Spc (symbol s)})

(defn Sym
  ([s] {:Sym (symbol s)})
  ([s d] {:Sym [(symbol s) d]}))

(defn QSym [s] {:QSym s})

(defn Qts [s] {:Qts (str s)})

(defn Chr [s] {:Chr (symbol s)})

(defn Int [s] {:Int (parse-long s)})

(defn Flt [s] {:Flt (parse-double s)})

(defn Str [s] {:Str (str s)})

(defn Key [s] {:Key (keyword s)})

(defn Tok [s] {:Tok (str s)})

(defn Rgx [s] {:Rgx (str s)})

(defn Bln [b]
  (if (re-matches #"(true|True|TRUE)" b)
    {:Bln true}
    {:Bln false}))

(defn Nil []
  {:Nil nil})

(defn Clj [c]
  {:Clj c})

(def operators
  {(Sym '.)   (Sym '_dot_)
   (Sym '..)  (Sym 'rng)
   (Sym '...) (Sym 'range)
   (Sym '+)   (Sym 'add+)
   (Sym '-)   (Sym 'sub+)
   (Sym '*)   (Sym 'mul+)
   (Sym '/)   (Sym 'div+)
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
   (Int "123")
   (Flt "1.23")
   (Str "foo")
   (Key "foo")]
  )
