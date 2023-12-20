;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.ast library defines the YAMLScript AST nodes.

(ns yamlscript.ast
  (:use yamlscript.debug)
  (:refer-clojure :exclude [Vec]))

(defn Lst [list] {:Lst (vec list)})

(defn Vec [list] {:Vec (vec list)})

(defn Map [list]
  (if (even? (count list))
    {:Map (->> list
            (apply array-map)
            (mapcat seq)
            vec)}
    (throw (Exception. "Odd number of elements in map"))))

(defn Spc [s] {:Spc (symbol s)})

(defn Sym [s] {:Sym (symbol s)})

(defn Chr [s] {:Chr (symbol s)})

(defn Int [s] {:Int (parse-long s)})

(defn Flt [s] {:Flt (parse-double s)})

(defn Str [s] {:Str (str s)})

(defn Key [s] {:Key (keyword s)})

(defn Tok [s] {:Tok (str s)})

(defn Bln [b]
  (if (re-matches #"(true|True|TRUE)" b)
    {:Bln true}
    {:Bln false}))

(defn Nil []
  {:Nil nil})

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
