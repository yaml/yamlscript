;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.ysreader is responsible for reading YAMLScript ysexpr (yes
;; expression) strings into a set of Clojure AST nodes.

(ns yamlscript.ysreader
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [yamlscript.ast :as ast :refer
    [Bln Chr Flt Form Int Key Lst Map Nil
     QSym Qts Rgx Set Spc Str Sym Tok Tup Vec]]
   [yamlscript.re :as re]
   [yamlscript.debug]
   [yamlscript.util :as util :refer [die if-lets]])
  (:refer-clojure :exclude [read-string]))

(defn is-clojure-comment? [token]
  (re-matches re/ccom (str token)))

(defn is-inline-comment? [token]
  (re-matches re/icom (str token)))

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

(defn is-dot-sym? [token]
  (re-matches re/dots (str token)))

(defn is-dot-special? [token]
  (re-matches re/dotx (str token)))

(defn is-operator? [token]
  (let [t (str token)]
    (and (re-matches re/osym t)
      (not= t "&"))))

(defn is-quote? [token]
  (re-matches re/quot (str token)))

(defn is-special? [token]
  (re-matches re/spec (str token)))

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

(defn is-alias-symbol? [token]
  (re-matches re/asym (str token)))

(defn is-symbol? [token]
  (or
    (re-matches re/fsym (str token))
    (re-matches re/ysym (str token))
    (re-matches re/vsym (str token))
    (re-matches re/ssym (str token))
    (re-matches re/asym (str token))
    (re-matches re/splt (str token))))

(defn is-default-symbol? [token]
  (re-matches re/dsym (str token)))

(defn is-symbol-paren? [token]
  (re-matches re/psym (str token)))

(def re-tokenize
  (re/re
    #"(?x)
    (?:
      $ccom |                 # Clojure comment
      $icom |                 # Inline comment
                              # Symbols and operators
      $quot |                   # Quote token
      $spec |                   # Special token
      $char |                   # Character token
      $keyw |                   # Keyword token
      $psym |                   # Symbol followed by paren
      $fsym |                   # Fully qualified symbol
      $nspc |                   # Namespace symbol
      $mnum |                   # Maybe a numeric literal token
      $regx |                   # Regex token
      $xsym |                   # Special operators (=~ etc)
      $dsym |                   # Symbol with default
      $vsym |                   # Variable lookup symbol
      $ssym |                   # Special symbols
      $asym |                   # Alias symbol
      $splt |                   # Splat symbol
      $csym |                   # Clojure symbol
      $narg |                   # Numbered argument token
      $dotn |                   # Dot operator followed by number
      $dots |                   # Dot operator word with _ allowed
      $dotx |                   # Special dot operators
      $osym |                   # Operator symbol token
      $anon |                   # Anonymous fn start token
      $sett |                   # Set start token
      $dstr |                 # String token
      $sstr |                 # String token
                              # Other tokens
      .                         # Single character tokens
    )"))

(defn lex-tokens [expr]
  (->> expr
    (re-seq re-tokenize)
    (remove #(re-matches re/ignr %1))
    (#(if (System/getenv "YS_LEX_DEBUG")
        (WWW %1)
        %1))))

(declare read-form yes-expr)

(defn- conj-seq [coll part & xs]
  (apply conj
    (cond-> coll
      (seq part)
      (conj (Form (yes-expr part))))
    xs))

(defn- qf [form]
  (cond
    (:Sym form) (let [sym (-> form :Sym str)]
                  (if (re-find #"^\$\w" sym)
                    (Sym (subs sym 1))
                    (if (= sym "$#")
                      form
                      (QSym (:Sym form)))))
    (:Lst form) (update-in form [:Lst]
                  (fn [lst] (map #(if (= {:Sym '_} %1)
                                    (Sym '_)
                                    %1) lst)))
    :else form))

(defn fix-dot-chain [expr]
  (if-lets [lst (or (get-in expr [0 :Lst]) expr)
            _ (= (first lst) {:Sym '_dot_})]
    [{:dot (vec (rest lst))}]
    expr))

(def sep (Sym '.))

(defn group-dots [forms]
  (fix-dot-chain
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
      forms)))

(defn yes-expr [expr]
  (fix-dot-chain
    (if (= (count expr) 3)
      (let [[a op b] expr]
        (if (is-operator? (:Sym op))
          (let [op (or (ast/operators op) op)] [op a b])
          expr))
      (if (and (> (count expr) 3)
            (some #(->> expr
                     (partition 2)
                     (map second)
                     (apply = %1))
              (map Sym '[+ - * / || ||| && &&& . ** = == > >= < <=])))
        (let [op (second expr)
              op (or (ast/operators op) op)]
          (->> expr
            (cons nil)
            (partition 2)
            (map second)
            (cons op)))
        expr))))

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
      (die "Unexpected end of input"))

    (if (= (first tokens) ")")
      (let [form (-> list group-dots yes-expr)
            args (anon-fn-arg-list form)
            expr (Lst [(Sym 'fn) (Vec [(Sym "&")(Vec args)]) (Form form)])]
        [expr (rest tokens)])
      (let [[form tokens] (read-form tokens)]
        (recur tokens (conj list form))))))

(defn read-list [[_ & tokens] type end sym]
  (loop [tokens tokens
         list []]
    (when (not (seq tokens))
      (die "Unexpected end of input"))

    (if (= (first tokens) end)
      (let [list (if (= type Lst)
                   (let [list (group-dots list)
                         forms (yes-expr list)]
                     (if sym
                       (if (not= list forms)
                         [sym (Form forms)]
                         (cons sym forms))
                       forms))
                   (group-dots list))]
        [(type list) (rest tokens)])
      (let [[form tokens] (read-form tokens)]
        (recur tokens (conj list form))))))

#_(def unescapes
  {"\\\\" "\\"
   "\\n" "\n"
   "\\t" "\t"
   "\\\"" "\""})

#_(defn str-unescape [s]
  (str/replace s #"(?:\\\\|\\n|\\t|\\\")"
    (fn [m] (get unescapes m))))

;; TODO do in one call
(defn str-unescape [s]
  (-> s
    (str/replace "\\\\" "\\")
    (str/replace ":\\ " ": ")
    (str/replace " \\#" " #")
    (str/replace "\\b" "\b")
    (str/replace "\\f" "\f")
    (str/replace "\\n" "\n")
    (str/replace "\\r" "\r")
    (str/replace "\\t" "\t")
    (str/replace "\\\"" "\"")))

(defn read-dq-string [string]
  (let [build-vstr @util/build-vstr]
    (-> string
      (subs 1 (dec (count string)))
      str-unescape
      (#(hash-map :vstr %))
      build-vstr)))

(defn sstr-unescape [s]
  (-> s
    (str/replace ":\\ " ": ")
    (str/replace " \\#" " #")))

(defn read-sq-string [string]
  (-> string
    (subs 1 (dec (count string)))
    sstr-unescape
    (str/replace #"''" "'")
    Str))

(declare read-form)

(defn add-specials [token tokens]
  (concat
    (condp = token
      ".#"   ["." "count(" ")"]
      ".#?"  ["." "count(" ")" "." "truey?(" ")"]
      ".#!"  ["." "count(" ")" "." "falsey?(" ")"]
      ".?"   ["." "truey?(" ")"]
      ".??"  ["." "boolean(" ")"]
      ".!"   ["." "falsey?(" ")"]
      ".!!"  ["." "not(" ")"]
      ".???" ["." "DBG(" ")"])
    tokens))

(defn read-scalar [[token & tokens]]
  (cond
    (map? token) [token tokens]
    (is-clojure-comment? token)
    (die "Clojure style comments are not allowed: '" token "'.")
    (is-inline-comment? token) [nil tokens]
    (= "nil" token) [(Nil) tokens]
    (= "true" token) [(Bln token) tokens]
    (= "false" token) [(Bln token) tokens]
    (is-narg? token) (let [n (subs token 1)
                           n (parse-long n)
                           _ (when (or (<= n 0) (> n 20))
                               (die "Invalid numbered argument: " token))
                           n (str "_" n)]
                       [(Sym n) tokens])
    (is-bad-number? token) (die "Invalid number: " token)
    (is-integer? token) [(Int token) tokens]
    (is-float? token) [(Flt token) tokens]
    (is-dot-special? token) [nil (add-specials token tokens)]
    (is-dot-num? token) (let [tokens (cons (subs token 1) tokens)]
                          [(Sym ".") tokens])
    (is-dot-sym? token) (let [tokens (cons (Sym (subs token 1)) tokens)]
                          [(Sym ".") tokens])
    (is-quote? token) (let [[value tokens] (read-form tokens)]
                        [(Tup [(Tok "'") value]) tokens])
    (is-special? token) (let [[value tokens] (read-form tokens)]
                          [(Tup [(Tok token) value]) tokens])
    (is-alias-symbol? token) [(Lst [(Sym '_**) (Qts (subs token 1))]) tokens]
    (is-operator? token) [(Sym token) tokens]
    (= "&" (str token)) [(Sym token) tokens]
    (is-string? token) [(read-dq-string token) tokens]
    (is-single? token) [(read-sq-string token) tokens]
    (is-keyword? token) [(Key (subs token 1)) tokens]
    (is-character? token) [(Chr (subs token 2)) tokens]
    (is-regex? token) [(Rgx (subs token 1 (dec (count token)))) tokens]

    (is-fq-symbol? token)
    (let [sym (str/replace token #"::" ".")]
      [(Sym sym) tokens])

    (is-symbol? token) [(Sym token) tokens]

    (is-default-symbol? token)
    (let [[value tokens] (read-form tokens)
          token (subs token 0 (-> token count dec))]
      [(Sym token value) tokens])

    (is-clojure-symbol? token)
    (die "Invalid symbol: '" token "'")

    (is-namespace? token) [(Spc token) tokens]
    :else (die "Unexpected token: '" token "'")))

(defn read-form [tokens]
  (let [token (first tokens)
        [token tokens sym]
        (if (is-symbol-paren? token)
          (let [sym (subs token 0 (-> token count dec))
                sym (str/replace sym #"::" ".")
                sym (if (= "DBG" sym) "_DBG" sym)]
            ["(" (cons "(" (rest tokens)) (Sym sym)])
          [token tokens nil])]
    (case token
      "\\(" (read-anon-fn tokens)
      "(" (read-list tokens Lst ")" sym)
      "[" (read-list tokens Vec "]" nil)
      "{" (read-list tokens Map "}" nil)
      "\\{" (read-list tokens Set "}" nil)
      ,   (read-scalar tokens))))

(defn read-forms [tokens]
  (loop [tokens tokens
         forms []]
    (if (seq tokens)
      (let [[form tokens] (read-form tokens)
            forms (if form (conj forms form) forms)]
        (recur tokens forms))
      forms)))

(defn read-string [string]
  (let [forms (-> string
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
          (Form expr))))))

(comment
  )
