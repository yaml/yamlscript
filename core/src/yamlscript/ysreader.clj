;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.ysreader is responsible for reading YAMLScript ysexpr (yes
;; expression) strings into a set of Clojure AST nodes.

(ns yamlscript.ysreader
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [yamlscript.ast :refer
    [Bln Chr Int Key Lst Map Nil Rgx Spc Str Sym Tok Vec]]
   [yamlscript.re :as re]
   [yamlscript.debug :refer [www]])
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

(defn is-path? [token]
  (and token (re-matches re/path (str token))))

(defn is-syntax-quote? [token]
  (and token (= "`" (str token))))

(defn is-unquote-splice? [token]
  (and token (= "~@" (str token))))

(defn is-clojure-symbol? [token]
  (and token (re-matches re/csym (str token))))

(defn is-regex? [token]
  (and token (re-matches re/regx (str token))))

(defn is-string? [token]
  (and token (re-matches re/dstr (str token))))

(defn is-single? [token]
  (and token (re-matches re/sstr (str token))))

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
      $path |                   # Lookup path
      $lnum |                   # Number token
      $regx |                   # Regex token
      $spop |                   # Special operators (=~ etc)
      $csym |                   # Clojure symbol
      $narg |                   # Numbered argument token
      $oper |                   # Operator token
      $char |                   # Character token
      $anon |                   # Anonymous fn start token
                              # Reader macros
      \#\_ |                    # Ignore next form
      \#\' |                    # Var
      \#\( |                    # Anonymous fn
      \#\{ |                    # HashSet
      \#\? |                    # Reader conditional
      ; |
                              # Other tokens
      ~@ |                      # Unquote-splice token
      [\[\]{}()`~^@] |          # Single character tokens

      $dstr |                 # String token
      $sstr |                 # String token
    )"))

(defn wrap-parens [expr]
  (str "(" expr "\n)"))

(defn lex-tokens [expr]
  (->> expr
    (re-seq re-tokenize)
    (remove #(re-matches re/ignr %1))
    (remove empty?)
    (#(if (System/getenv "YS_LEX_DEBUG")
        (www %1)
        %1))))

(declare read-form)

(defn yes-expr [expr]
  (if (= (count expr) 3)
    (let [[a op b] expr]
      (if (is-operator? (:Sym op))
        (let [op (cond
                   (= op (Sym '..)) (Sym 'rng)
                   (= op (Sym '||)) (Sym 'or)
                   (= op (Sym '&&)) (Sym 'and)
                   (= op (Sym '%))  (Sym 'rem)
                   (= op (Sym '%%)) (Sym 'mod)
                   :else op)]
          [op a b])
        expr))
    (if (and (> (count expr) 3)
          (some #(->> expr
                   (partition 2)
                   (map second)
                   (apply = %1))
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

(defn anon-fn-arg-list [node]
  (let [args (atom {})
        maxn (atom 0)]
    (walk/prewalk
      #(if (map? %1)
         (let [[[key val]] (seq %1)
               val (str val)]
           (if (and (= :Sym key) (re-matches #"_\d+" val))
             (let [n (parse-long (subs val 1))]
               (swap! args assoc n true)
               (swap! maxn max n))
             %1))
         %1)
      node)
    (map
      #(if (get @args %1)
         (Sym (str "_" %1))
         (Sym "_"))
      (range 1 (inc @maxn)))))

(defn read-anon-fn [[_ & tokens]]
  (loop [tokens tokens
         list []]
    (when (not (seq tokens))
      (throw (Exception. "Unexpected end of input")))

    (if (= (first tokens) ")")
      (let [form (yes-expr list)
            args (anon-fn-arg-list form)
            expr (Lst [(Sym 'fn) (Vec [(Sym "&")(Vec args)]) (Lst form)])]
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
    (subs 1 (dec (count string)))
    (str/replace #"\\ " " ")
    (str/replace #"\\n" "\n")))

(defn normalize-single [string]
  (-> string
    (subs 1 (dec (count string)))
    (str/replace #"''" "'")))

(defn make-path [token]
  (let
   [[value & keys] (str/split token #"\.")
    form (map
           #(cond
              (is-number? %1) (Int %1)
              (is-symbol? %1) (Str %1)
              (is-string? %1) (Str (normalize-string %1))
              (is-single? %1) (Str (normalize-single %1))
              :else (throw (Exception. (str "Invalid path token: " %1))))
           keys)
    form (cons (Sym '__) (cons (Sym value) form))]
    (Lst form)))

(defn read-scalar [[token & tokens]]
  (cond
    (is-comment? token) []
    (= "nil" token) [(Nil) tokens]
    (= "true" token) [(Bln token) tokens]
    (= "false" token) [(Bln token) tokens]
    (is-narg? token) (let [n (subs token 1)
                           n (parse-long n)
                           _ (when (or (<= n 0) (> n 20))
                               (throw (Exception.
                                        (str "Invalid numbered argument: "
                                          token))))
                           n (str "_" n)]
                       [(Sym n) tokens])
    (is-number? token) [(Int token) tokens]
    (is-operator? token) [(Sym token) tokens]
    (is-unquote-splice? token) [(Tok token) tokens]
    (is-syntax-quote? token) [(Tok token) tokens]
    (is-string? token) [(Str (normalize-string token)) tokens]
    (is-single? token) [(Str (normalize-single token)) tokens]
    (is-keyword? token) [(Key (subs token 1)) tokens]
    (is-character? token) [(Chr (subs token 1)) tokens]
    (is-path? token) [(make-path token) tokens]
    (is-regex? token) [(Rgx (->> (subs token 1 (dec (count token))))) tokens]
    (is-fq-symbol? token)
    (let [sym (str/replace token #"\." "/")
          sym (str/replace sym #"::" ".")]
      [(Sym sym) tokens])
    ,
    (is-symbol? token) [(Sym token) tokens]
    (is-clojure-symbol? token)
    (throw (Exception. (str "Invalid symbol: '" token "'")))
    ,
    (is-namespace? token) [(Spc token) tokens]
    :else (throw (Exception. (str "Unexpected token: '" token "'")))))

(defn read-form [tokens]
  (let [token (first tokens)
        [token tokens]
        (if (is-symbol-paren? token)
          (let [sym (subs token 0 (-> token count dec))]
            ["(" (conj (rest tokens) sym "(")])
          [token tokens])]
    (case token
      "\\(" (read-anon-fn tokens)
      "(" (read-list tokens Lst ")")
      "[" (read-list tokens Vec "]")
      "{" (read-list tokens Map "}")
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
                       (apply = %1))
                (map Sym '[+ - * / || && .])))
          (let [op (second forms)
                op (cond
                     (= op (Sym '||)) (Sym 'or)
                     (= op (Sym '&&)) (Sym 'and)
                     :else op)]
            (Lst (cons op (vec (map second (partition 2 (cons nil forms)))))))
          (vec forms))))))

(comment
  www
  (read-string "1 + 2")
  (read-string
    "[\"a\" :b \\c 42 true false nil
     (a b c) [a b c] {:a b :c \"d\"}]")
  )
