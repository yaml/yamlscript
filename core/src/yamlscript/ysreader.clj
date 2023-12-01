;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.ysreader is responsible for reading YAMLScript ysexpr (yes
;; expression) strings into a set of Clojure AST nodes.

(ns yamlscript.ysreader
  (:use yamlscript.debug)
  (:require
   [clojure.string :as str]
   [yamlscript.ast :refer :all]
   [yamlscript.re :as re])
  (:refer-clojure :exclude [read-string resolve]))

(defn is-comment? [token]
  (and token (re-matches re/comm (str token))))

(defn is-character? [token]
  (and token (re-matches re/char (str token))))

(defn is-keyword? [token]
  (and token (re-matches re/keyw (str token))))

(defn is-namespace? [token]
  (and token (re-matches re/nspc (str token))))

(defn is-number? [token]
  (and token (re-matches re/lnum (str token))))

(defn is-operator? [token]
  (and token (re-matches re/oper (str token))))

(defn is-quote? [token]
  (and token (= "'" (str token))))

(defn is-string? [token]
  (and token (re-matches re/strg (str token))))

(defn is-symbol? [token]
  (and token
    (or
      (re-matches re/fqsm (str token))
      (re-matches re/dyns (str token))
      (re-matches re/symb (str token)))))

(defn is-symbol-paren? [token]
  (and token (re-matches re/symp (str token))))

(def re-tokenize
  (re/re
    #"(?x)
    (?:
      $comm |                 # Comment
                              # Symbols and operators
      $keyw |                   # Keyword token
      $dyns |                   # Dynamic symbol
      $symp |                   # Symbol followed by paren
      $fqsm |                   # Fully qualified symbol
      $nspc |                   # Namespace symbol
      $symb |                   # Symbol token
      $oper |                   # Operator token
      $char |                   # Character token
                              # Reader macros
      \#\_ |                    # Ignore next form
      \#\' |                    # Var
      \#\( |                    # Lambda
      \#\{ |                    # HashSet
      \#\? |                    # Reader conditional
      ; |
                              # Other tokens
      ~@ |                      # Unquote-splice token
      [\[\]{}()'`~^@] |         # Single character tokens

      $strg                   # String token
    )"))

(defn wrap-parens [expr]
  (str "(" expr "\n)"))

(defn lex-tokens [expr]
  (->> expr
    (re-seq re-tokenize)
    (remove #(re-matches re/ignr %1))))

(declare read-form)

(defn yes-expr [expr]
  (if (= (count expr) 3)
    (let [[a b c] expr]
      (if (is-operator? (:Sym b))
        (let [op (cond
                   (= b (Sym '..)) (Sym 'rng)
                   :else b)
              op (cond
                   (= op (Sym '||)) (Sym 'or)
                   (= op (Sym '&&)) (Sym 'and)
                   :else op)]
          [op a c])
        expr))
    (if (and (> (count expr) 3)
          (some #(->> expr
                   (partition 2)
                   (map second)
                   (apply = %))
            (map Sym '[+ - * / || &&])))
      (let [op (second expr)
            op (cond
                 (= op (Sym '||)) (Sym 'or)
                 (= op (Sym '&&)) (Sym 'and)
                 :else op)
            ]
        (->> expr
          (cons nil)
          (partition 2)
          (map second)
          (cons op)))
      expr)))

(defn read-list [[_ & tokens] type end]
  (loop [tokens tokens
         list []]
    (when (not (seq tokens))
      (throw (Exception. "Unexpected end of input")))

    (if (= (first tokens) end)
      (let [list (if (= type Lst)
                   (yes-expr list)
                   list)]
        [(type list) (rest tokens)])
      (let [[form tokens] (read-form tokens)]
        (recur tokens (conj list form))))))

(defn normalize-string [string]
  (-> string
    (subs 1 (- (count string) 1))
    (str/replace #"\\ " " ")
    (str/replace #"\\n" "\n")))

(defn read-scalar [[token & tokens]]
  (cond
    (is-comment? token) []
    (= "nil" token) [(Nil) tokens]
    (= "true" token) [(Bln token) tokens]
    (= "false" token) [(Bln token) tokens]
    (is-number? token) [(Int token) tokens]
    (is-operator? token) [(Sym token) tokens]
    (is-string? token) [(Str (normalize-string token)) tokens]
    (is-keyword? token) [(Key (subs token 1)) tokens]
    (is-character? token) [(Chr (subs token 1)) tokens]
    (is-symbol? token) [(Sym token) tokens]
    (is-namespace? token) [(Sym token) tokens]
    :else (throw (Exception. (str "Unexpected token: '" token "'")))))

(defn read-quoted-form [[_ & tokens]]
  (let [[form tokens] (read-form tokens)]
    [(Lst [(Sym "quote") form]) tokens]))

(defn read-form [tokens]
  (let [token (first tokens)
        [token tokens]
        (if (is-symbol-paren? token)
          (let [sym (subs token 0 (-> token count dec))]
            ["(" (conj (rest tokens) sym "(")])
          [token tokens])]
    (case token
      "(" (read-list tokens Lst ")")
      "[" (read-list tokens Vec "]")
      "{" (read-list tokens Map "}")
      "'" (read-quoted-form tokens)
      ,   (read-scalar tokens))))

(defn read-forms [tokens]
  (loop [tokens tokens
         forms []]
    (if (seq tokens)
      (let [[form tokens] (read-form tokens)]
        (recur tokens (conj forms form)))
      forms)))

(defn read-string [string]
  (let [forms (->> string
                lex-tokens
                read-forms)]
    (case (count forms)
      0 nil
      1 (first forms)
      (vec forms))))

(comment
  (read-string "(1 .. 5)")
  (read-string
    "[\"a\" :b \\c 42 true false nil
     (a b c) [a b c] {:a b :c \"d\"}]")
  )
