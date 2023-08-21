(ns yamlscript.ysreader
  (:use yamlscript.debug)
  (:require [yamlscript.ast :refer :all])
  (:refer-clojure :exclude [read-string resolve]))

(def re-ignore
  #"(?x)
    (?:                       # Ignorables
      \#\!.*\n? |               # hashbang line
      [\s,]+    |               # whitespace, commas,
      ;.*\n?                    # comments
    )
  ")

(def re-tokenize
  (re-pattern
    (str
      re-ignore "|"
      #"(?x)
      (?:                       # Symbols and operators
        :\w+(?:-\w+)*[?!]? |      # Keyword token
        \w+(?:-\w+)*[?!]?\( |     # Symbol followed by paren
        \w+(?:-\w+)*[?!]? |       # Symbol token
        [-+*/<=>|&]{1,3} |        # Operator token
        \\[^\s] |                 # Character token
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
      )")))

(defn wrap-parens [expr]
  (str "(" expr "\n)"))

(defn lex-tokens [expr]
  (->> expr
    (re-seq re-tokenize)
    (remove #(re-matches re-ignore %1))))

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
                read-forms
                #__)]
    (case (count forms)
      0 nil
      1 (first forms)
      (vec forms))))

nil
