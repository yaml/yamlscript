;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.ysreader is responsible for reading YAMLScript ysexpr (yes
;; expression) strings into a set of Clojure AST nodes.

(ns yamlscript.ysreader
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [yamlscript.ast :refer
    [Bln Chr Flt Int Key Lst Map Nil Rgx Spc Str Sym Tok Vec]]
   [yamlscript.re :as re]
   [yamlscript.debug :refer [www]]
   [yamlscript.util :as util
    :refer [if-lets]])
  (:refer-clojure :exclude [read-string]))

(defn is-comment? [token]
  (re-matches re/comm (str token)))

(defn is-character? [token]
  (re-matches re/char (str token)))

(defn is-keyword? [token]
  (re-matches re/keyw (str token)))

(defn is-namespace? [token]
  (re-matches re/nspc (str token)))

(defn is-narg? [token]
  (re-matches re/narg (str token)))

(defn is-bad-number? [token]
  (and
    (re-matches re/mnum (str token))
    (not (re-matches re/xnum (str token)))))

(defn is-integer? [token]
  (re-matches re/inum (str token)))

(defn is-float? [token]
  (re-matches re/fnum (str token)))

(defn is-dot-num? [token]
  (re-matches re/dotn (str token)))

(defn is-operator? [token]
  (re-matches re/osym (str token)))

(defn is-syntax-quote? [token]
  (= "`" (str token)))

(defn is-unquote-splice? [token]
  (= "~@" (str token)))

(defn is-clojure-symbol? [token]
  (re-matches re/csym (str token)))

(defn is-regex? [token]
  (re-matches re/regx (str token)))

(defn is-string? [token]
  (re-matches re/dstr (str token)))

(defn is-single? [token]
  (re-matches re/sstr (str token)))

(defn is-fq-symbol? [token]
  (re-matches re/fsym (str token)))

(defn is-symbol? [token]
  (or
    (re-matches re/fsym (str token))
    (re-matches re/esym (str token))
    (re-matches re/ysym (str token))
    (re-matches re/vsym (str token))
    (re-matches re/ssym (str token))))

(defn is-default-symbol? [token]
  (re-matches re/dsym (str token)))

(defn is-symbol-paren? [token]
  (re-matches re/psym (str token)))

(def re-tokenize
  (re/re
    #"(?x)
    (?:
      $comm |                 # Comment
                              # Symbols and operators
      $keyw |                   # Keyword token
      $esym |                   # Earmuff symbol
      $psym |                   # Symbol followed by paren
      $fsym |                   # Fully qualified symbol
      $nspc |                   # Namespace symbol
      $mnum |                   # Maybe a numeric literal token
      $regx |                   # Regex token
      $xsym |                   # Special operators (=~ etc)
      $dsym |                   # Symbol with default
      $vsym |                   # Variable lookup symbol
      $ssym |                   # Special symbols
      $csym |                   # Clojure symbol
      $narg |                   # Numbered argument token
      $dotn |                   # Dot operator followed by number
      $osym |                   # Operator symbol token
      $char |                   # Character token
      $anon |                   # Anonymous fn start token
      $dstr |                 # String token
      $sstr |                 # String token
                              # Other tokens
      ~@ |                      # Unquote-splice token
      [\[\]{}()`~^@] |          # Single character tokens
    )"))

(defn lex-tokens [expr]
  (->> expr
    (re-seq re-tokenize)
    (remove #(re-matches re/ignr %1))
    (#(if (System/getenv "YS_LEX_DEBUG")
        (www %1)
        %1))))

(declare read-form yes-expr)

(defn- conj-seq [coll part & xs]
  (apply conj
    (cond-> coll
      (seq part)
      (conj (Lst (yes-expr part))))
    xs))

(defn- qf [form]
  (cond
    (:Sym form) (let [sym (-> form :Sym str)]
                  (if (re-find #"^\$\w" sym)
                    (Sym (subs sym 1))
                    (if (= sym "$#")
                      form
                      (Lst [(Sym 'quote) form]))))
    (:Lst form) (Lst [(Sym 'quote) form])
    :else form))

(def sep (Sym '.))

(defn group-dots [forms]
  (if (>= (count forms) 3)
    (loop [[a b c :as xs] forms, grp [], acc []]
      (cond (empty? xs)
            (conj-seq acc grp)
            ,
            (and c (= sep b) (not= a sep) (not= c sep))
            (recur
              (drop 3 xs)
              [a b (qf c)]
              (conj-seq acc grp))
            ,
            (and b (= sep a) (seq grp))
            (recur
              (drop 2 xs)
              (conj grp a (qf b))
              acc)
            ,
            :else
            (recur
              (rest xs)
              []
              (conj-seq acc grp a))))
    forms))

(def operators
  {(Sym '.)  (Sym '__)
   (Sym '..) (Sym 'rng)
   (Sym '||) (Sym 'or)
   (Sym '&&) (Sym 'and)
   (Sym '%)  (Sym 'rem)
   (Sym '%%) (Sym 'mod)
   (Sym '**) (Sym 'pow)})

(defn yes-expr [expr]
  (if (= (count expr) 3)
    (let [[a op b] expr]
      (if (is-operator? (:Sym op))
        (let [op (or (operators op) op)] [op a b])
        expr))
    (if (and (> (count expr) 3)
          (some #(->> expr
                   (partition 2)
                   (map second)
                   (apply = %1))
            (map Sym '[+ - * / || && .])))
      (let [op (second expr)
            op (cond
                 (= op (Sym '.))  (Sym '__)
                 (= op (Sym '||)) (Sym 'or)
                 (= op (Sym '&&)) (Sym 'and)
                 :else op)]
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
      (let [form (-> list group-dots yes-expr)
            args (anon-fn-arg-list form)
            expr (Lst [(Sym 'fn) (Vec [(Sym "&")(Vec args)]) (Lst form)])]
        [expr (rest tokens)])
      (let [[form tokens] (read-form tokens)]
        (recur tokens (conj list form))))))

(defn read-list [[_ & tokens] type end sym]
  (loop [tokens tokens
         list []]
    (when (not (seq tokens))
      (throw (Exception. "Unexpected end of input")))

    (if (= (first tokens) end)
      (let [list (if (= type Lst)
                   (let [list (group-dots list)
                         forms (yes-expr list)]
                     (if sym
                       (if (not= list forms)
                         [sym (Lst forms)]
                         (cons sym forms))
                       forms))
                   (group-dots list))]
        [(type list) (rest tokens)])
      (let [[form tokens] (read-form tokens)]
        (recur tokens (conj list form))))))

(defn read-dq-string [string]
  (let [build-vstr @util/build-vstr]
    (-> string
      (subs 1 (dec (count string)))
      (#(hash-map :vstr %))
      build-vstr)))

(defn read-sq-string [string]
  (-> string
    (subs 1 (dec (count string)))
    (str/replace #"''" "'")
    Str))

(declare read-form)

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
    (is-bad-number? token) (throw
                             (Exception. (str "Invalid number: " token)))
    (is-integer? token) [(Int token) tokens]
    (is-float? token) [(Flt token) tokens]
    (is-dot-num? token) (let [tokens (cons (subs token 1) tokens)]
                          [(Sym ".") tokens])
    (is-operator? token) [(Sym token) tokens]
    (is-unquote-splice? token) [(Tok token) tokens]
    (is-syntax-quote? token) [(Tok token) tokens]
    (is-string? token) [(read-dq-string token) tokens]
    (is-single? token) [(read-sq-string token) tokens]
    (is-keyword? token) [(Key (subs token 1)) tokens]
    (is-character? token) [(Chr (subs token 1)) tokens]
    (is-regex? token) [(Rgx (->> (subs token 1 (dec (count token))))) tokens]

    (is-fq-symbol? token)
    (let [sym (str/replace token #"::" ".")]
      [(Sym sym) tokens])

    (is-symbol? token) [(Sym token) tokens]

    (is-default-symbol? token)
    (let [[value tokens] (read-form tokens)
          token (subs token 0 (-> token count dec))]
      [(Sym token value) tokens])

    (is-clojure-symbol? token)
    (throw (Exception. (str "Invalid symbol: '" token "'")))

    (is-namespace? token) [(Spc token) tokens]
    :else (throw (Exception. (str "Unexpected token: '" token "'")))))

(defn read-form [tokens]
  (let [token (first tokens)
        [token tokens sym]
        (if (is-symbol-paren? token)
          (let [sym (subs token 0 (-> token count dec))
                sym (str/replace sym #"::" ".")]
            ["(" (cons "(" (rest tokens)) (Sym sym)])
          [token tokens nil])]
    (case token
      "\\(" (read-anon-fn tokens)
      "(" (read-list tokens Lst ")" sym)
      "[" (read-list tokens Vec "]" nil)
      "{" (read-list tokens Map "}" nil)
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
                read-forms)
        num (count forms)]
    (case num
      0 nil
      1 (first forms)
      2 (vec forms)
      (let [forms (group-dots forms)
            expr (vec (yes-expr forms))]
        (if (= expr forms)
          (if (= 1 (count expr))
            (first expr)
            expr)
          (Lst expr))))))

(comment
  www
  (read-string "(1 * 4) + (2 * 3)")
  ; {:Lst [{:Sym +} {:Lst [{:Sym *} {:Int 1} {:Int 4}]} {:Lst [{:Sym *} {:Int 2} {:Int 3}]}]}
  ; {:Lst [{:Sym +} {:Lst [{:Sym *} {:Int 1} {:Int 4}]} {:Lst [{:Sym *} {:Int 2} {:Int 3}]}]}
  (read-string "a.b + c.d")
  ; {:Lst [{:Sym +} {:Lst [{:Sym __} {:Sym a} {:Sym b}]} {:Lst [{:Sym __} {:Sym c} {:Sym d}]}]}
  (read-string "1 + 2")
  (read-string "a b")
  (read-string "a.b")
  (read-string "a.b.3.c()")
  (read-string
    "[\"a\" :b \\c 42 true false nil
     (a b c) [a b c] {:a b :c \"d\"}]")
  )
