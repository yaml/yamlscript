(ns yamlscript.ysreader
  (:use yamlscript.debug)
  (:require
   [clojure.string :as str]
   [yamlscript.ast :refer :all])
  (:refer-clojure :exclude [read-string resolve]))

(def re-map
  {:ignr #"(?x)
         (?:                  # Ignorables
           \#\!.*\n? |          # hashbang line
           [\s,]+    |          # whitespace, commas,
           ;.*\n?               # comments
         )"
   :char #"\\."               ; Character token
   :keyw #"(?:\:\w+(?:-\w+)*)"     ; Keyword token
   :oper #"[-+*/<=>|&]{1,3}"  ; Operator token
   :sym #"\w+(?:-\w+)*[?!]?"  ; Symbol token
   :symp #"[-+*/<=>|&]{1,3}"  ; Symbol followed by paren
   })

(defn re-expand [re]
  (re-pattern
    (reduce
      (fn [re [k v]]
        (let [pat (re-pattern (str #"\$" (subs (str k) 1)))]
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
      \#?                       # Possibly a regex

      \"(?:                   # Quoted string
        \\. |                   # Escaped char
        [^\\\"]                 # Any other char
      )*\"?                     # Ending quote
    )"))

(defn wrap-parens [expr]
  (str "(" expr "\n)"))

(defn lex-tokens [expr]
  (->> expr
    (re-seq re-tokenize)
    (remove #(re-matches (:ignr re-map) %1))))

(declare read-form)

(defn read-list [[_ & tokens] type end]
  (loop [tokens tokens
         list []]
    (when (not (seq tokens))
      (throw (Exception. "Unexpected end of input")))

    (if (= (first tokens) end)
      [(type list) (rest tokens)]
      (let [[form tokens] (read-form tokens)]
        (recur tokens (conj list form))))))

(defn read-scalar [[token & tokens]]
  (cond
    (= "true" token) [(True) tokens]
    (= "false" token) [(False) tokens]
    (= "nil" token) [(Nil) tokens]
    (re-find #"^\"" token) [(Str (subs token 1 (- (count token) 1))) tokens]
    (re-find #"^:\w" token) [(Key (subs token 1)) tokens]
    (re-find #"^-?\d+$" token) [(LNum token) tokens]
    (re-find #"^\w" token) [(Sym token) tokens]
    (re-find #"^\\.$" token) [(Char (subs token 1)) tokens]
    :else (throw (Exception. (str "Unexpected token: '" token "'")))))

(defn read-form [tokens]
  (let [token (first tokens)]
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
