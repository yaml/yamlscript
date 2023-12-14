;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.ysreader is responsible for reading YAMLScript ysexpr (yes
;; expression) strings into a set of Clojure AST nodes.

(ns yamlscript.ysreader
  (:use yamlscript.debug)
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
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

(defn is-narg? [token]
  (and token (re-matches re/narg (str token))))

(defn is-number? [token]
  (and token (re-matches re/lnum (str token))))

(defn is-operator? [token]
  (and token (re-matches re/oper (str token))))

(defn is-quote? [token]
  (and token (= "'" (str token))))

(defn is-string? [token]
  (and token (re-matches re/strg (str token))))

(defn is-fq-symbol? [token]
  (and token
    (re-matches re/fqsm (str token))))

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
      $lamb |                   # Lambda start token
      $narg |                   # Numbered argument token
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
    (remove #(re-matches re/ignr %1))
    (#(if (System/getenv "YS_LEX_DEBUG")
        (www %)
        %))))

(declare read-form)

(defn yes-expr [expr]
  (if (= (count expr) 3)
    (let [[a op b] expr]
      (if (is-operator? (:Sym op))
        (let [op (cond
                   (= op (Sym '..)) (Sym 'rng)
                   (= op (Sym '||)) (Sym 'or)
                   (= op (Sym '&&)) (Sym 'and)
                   :else op)]
          [op a b])
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

(defn lambda-arg-list [node]
  (let [args (atom {})
        maxn (atom 0)]
    (walk/prewalk
      #(if (map? %)
         (let [[[key val]] (seq %)
               val (str val)]
           (if (and (= :Sym key) (re-matches #"_\d+" val))
             (let [n (parse-long (subs val 1))]
               (swap! args assoc n true)
               (swap! maxn max n))
             %))
         %)
      node)
    (map
      #(if (get @args %)
         (Sym (str "_" %))
         (Sym "_"))
      (range 1 (inc @maxn)))))

(defn read-lambda [[_ & tokens]]
  (loop [tokens tokens
         list []]
    (when (not (seq tokens))
      (throw (Exception. "Unexpected end of input")))

    (if (= (first tokens) ")")
      (let [form (yes-expr list)
            args (lambda-arg-list form)
            expr (Lst [(Sym 'fn) (Vec args) (Lst form)])]
        [expr (rest tokens)])
      (let [[form tokens] (read-form tokens)]
        (recur tokens (conj list form))))))

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
    (is-narg? token) (let [n (subs token 1)
                           n (if (empty? n) 1 (parse-long n))
                           _ (when (or (<= n 0) (> n 20))
                               (throw (Exception.
                                        (str "Invalid numbered argument: "
                                          token))))
                           n (str "_" n)]
                       [(Sym n) tokens])
    (is-number? token) [(Int token) tokens]
    (is-operator? token) [(Sym token) tokens]
    (is-string? token) [(Str (normalize-string token)) tokens]
    (is-keyword? token) [(Key (subs token 1)) tokens]
    (is-character? token) [(Chr (subs token 1)) tokens]
    ,
    (is-fq-symbol? token)
    (let [sym (str/replace token #"\." "/")
          sym (str/replace sym #"::" ".")]
      [(Sym sym) tokens])
    ,
    (is-symbol? token) [(Sym token) tokens]
    (is-namespace? token) [(Spc token) tokens]
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
      "\\(" (read-lambda tokens)
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
      (if (and
            (= 3 (count forms))
            (is-operator? (:Sym (second forms))))
        (let [op (second forms)
              op (cond
                   (= op (Sym '||)) (Sym 'or)
                   (= op (Sym '&&)) (Sym 'and)
                   (= op (Sym '..)) (Sym 'rng)
                   :else op)]
          (Lst [op (first forms) (last forms)]))
        (if (and
              (> (count forms) 3)
              (some #(->> forms
                       (partition 2)
                       (map second)
                       (apply = %))
                (map Sym '[+ - * / || && .])))
          (let [op (second forms)
                op (cond
                     (= op (Sym '||)) (Sym 'or)
                     (= op (Sym '&&)) (Sym 'and)
                     :else op)]
            (Lst (cons op (vec (map second (partition 2 (cons nil forms)))))))
          (vec forms))))))

(comment
  (read-string "1 + 2")
  (read-string
    "[\"a\" :b \\c 42 true false nil
     (a b c) [a b c] {:a b :c \"d\"}]")
  )
