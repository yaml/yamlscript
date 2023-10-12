(ns yamlscript.ysreader
  (:use yamlscript.debug)
  (:require
   [clojure.string :as str]
   [yamlscript.ast :refer :all])
  (:refer-clojure :exclude [read-string resolve]))

(def re-map
  {:ignr #"(?x)
         (?:                      # Ignorables
           \#\!.*\n? |              # hashbang line
           [\s,]+    |              # whitespace, commas,
           ;.*\n?                   # comments
         )"
   :char #"\\."                   ; Character token
   :keyw #"(?:\:\w+(?:-\w+)*)"    ; Keyword token
   :oper #"[-+*/<=>|&]{1,3}"      ; Operator token
   :str  #"(?x)
         \#?                      # Possibly a regex
         \"(?:                    # Quoted string
           \\. |                    # Escaped char
           [^\\\"]                  # Any other char
         )*\"?                      # Ending quote
         "
   :sym  #"\w+(?:-\w+)*[?!]?"     ; Symbol token
   :symp #"\w+(?:-\w+)*[?!]?\("   ; Symbol followed by paren
   })

(defn is-character? [token]
  (and token (re-matches (:char re-map) (str token))))

(defn is-keyword? [token]
  (and token (re-matches (:keyw re-map) (str token))))

(defn is-number? [token]
  (and token (re-matches #"-?\d+" (str token))))

(defn is-operator? [token]
  (and token (re-matches (:oper re-map) (str token))))

(defn is-string? [token]
  (and token (re-matches (:str re-map) (str token))))

(defn is-symbol? [token]
  (and token (re-matches (:sym re-map) (str token))))

(defn is-symbol-paren? [token]
  (and token (re-matches (:symp re-map) (str token))))

(defn re-expand [re]
  (re-pattern
    (reduce
      (fn [re [k v]]
        (let [pat (re-pattern (str #"\$" (subs (str k) 1) #"(?!\w)"))]
          (str/replace re pat (str/re-quote-replacement v))))
      re re-map)))

(def re-tokenize
  (re-expand
    #"(?x)
    (?:                       # Symbols and operators
      $keyw |                   # Keyword token
      $symp |                   # Symbol followed by paren
      $sym |                    # Symbol token
      $oper |                   # Operator token
      $char |                   # Character token
                              # Reader macros
      \#\_ |                    # Ignore next form
      \#\' |                    # Var
      \#\( |                    # Lambda
      \#\{ |                    # HashSet
      \#\? |                    # Reader conditional
                              # Other tokens
      ~@ |                      # Unquote-splice token
      [\[\]{}()'`~^@] |         # Single character tokens

      $str                    # String token
    )"))

(defn wrap-parens [expr]
  (str "(" expr "\n)"))

(defn lex-tokens [expr]
  (->> expr
    (re-seq re-tokenize)
    (remove #(re-matches (:ignr re-map) %1))))

(declare read-form)

(defn yes-expr [expr]
  (if (= (count expr) 3)
    (let [[a b c] expr]
      (if (is-operator? (:Sym b))
        [b a c]
        expr))
    expr))

(defn read-list [[_ & tokens] type end]
  (loop [tokens tokens
         list []]
    (when (not (seq tokens))
      (throw (Exception. "Unexpected end of input")))

    (if (= (first tokens) end)
      (let [list (if (= type List)
                   (yes-expr list)
                   list)]
        [(type list) (rest tokens)])
      (let [[form tokens] (read-form tokens)]
        (recur tokens (conj list form))))))

(defn normalize-string [string]
  (-> string
    (subs 1 (- (count string) 1))
    (str/replace #"\\ " " ")))

(defn read-scalar [[token & tokens]]
  (cond
    (= "nil" token) [(Nil) tokens]
    (= "true" token) [(True) tokens]
    (= "false" token) [(False) tokens]
    (is-number? token) [(LNum token) tokens]
    (is-operator? token) [(Sym token) tokens]
    (is-string? token) [(Str (normalize-string token)) tokens]
    (is-keyword? token) [(Key (subs token 1)) tokens]
    (is-character? token) [(Char (subs token 1)) tokens]
    (is-symbol? token) [(Sym token) tokens]
    :else (throw (Exception. (str "Unexpected token: '" token "'")))))

(defn read-form [tokens]
  (let [token (first tokens)
        [token tokens]
        (if (is-symbol-paren? token)
          (let [sym (subs token 0 (-> token count dec))]
            ["(" (conj (rest tokens) sym "(")])
          [token tokens])]
    (case token
      "(" (read-list tokens List ")")
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
      (vec forms))))

(comment
  (read-string
    "[\"a\" :b \\c 42 true false nil
     (a b c) [a b c] {:a b :c \"d\"}]")
  )
