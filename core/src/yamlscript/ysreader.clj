;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.ysreader is responsible for reading YS expression nodes
;; (YAML scalar nodes with the :expr key) into a set of Clojure AST nodes.

(ns yamlscript.ysreader
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [yamlscript.ast :as ast :refer
    [Bln Chr Form Key Lst Map Nil Num
     QSym Qts Rgx Set Spc Str Sym Tok Tup Vec]]
   [yamlscript.common]
   [yamlscript.global :as global]
   [yamlscript.re :as re])
  (:refer-clojure :exclude [read-string]))

(defn is-clojure-comment?
  "We match Clojure style line comments (starting with `;`) as tokens and then
  error on them, because YS does not support them. YS used to support them in
  very early versions, but YAML comment syntax was a more natural fit for YS.
  We might make use of the semicolon in the future, but for now we error on
  them."
  [token] (re-matches re/ccom (str token)))

(defn is-inline-comment?
  "YAML doesn't support comments in the middle of a multi-line scalar. YS
  supports a them in code mode using a syntax that consists of a backslash
  followed by a double quoted string. (Hard to show a clear example in this
  double quoted docstring)"
  [token] (re-matches re/icom (str token)))

(defn is-character?
  "A character literal token. YS uses a double backslash for character literals,
  whereas Clojure uses a single backslash. This is because backslash is YS's
  escape character for various things like anonymous functions."
  [token] (re-matches re/char (str token)))

(defn is-keyword?
  "YS keywords are stricter than Clojure keywords. They still start with a
  colon, but then only allow alphanumeric words separated by a single hyphen."
  [token] (re-matches re/keyw (str token)))

(defn is-namespace?
  "YS namespace tokens are simple words separated by '::'. Clojure uses '.' but
  the dot is used for the dot chaining operator in YS so that wouldn't work
  for namespace names."
  [token]
  (re-matches re/nspc (str token)))

(defn is-narg?
  "A numbered argument found inside an anonymous function. YS currently supports
  the Clojure style %1 %2 etc, but prefers _1 _2 etc. %1 will be removed in v1.
  YS supports _ to mean _1, but doesn't support % for %1 because % is used for
  the remainder operator."
  [token]
  (re-matches re/narg (str token)))

(defn is-bad-number?
  "YS will be greedy when lexing tokens that start with a digit like '1A2B'.
  That would be lexed as a single token and then error on it as a bad number."
  [token]
  (and
    (re-matches re/mnum (str token))
    (not (re-matches re/xnum (str token)))))

(defn is-number?
  "A valid number token including integers, floats, ratios, radix, hex, octal,
  big ints and floats and special numbers like .inf, .NaN, etc."
  [token] (re-matches re/xnum (str token)))

(defn is-dot-num?
  "A dot operator followed by a number is used for list indexing and needs to be
  lexed as a single token."
  [token] (re-matches re/dotn (str token)))

(defn is-dot-sym?
  "A dot operator followed by a symbol is used for map key lookup and needs to
  be lexed as a single token."
  [token] (re-matches re/dots (str token)))

(defn is-dot-special?
  "A dot operator followed by certain punctuation characters is used for special
  operations like .++, .--, .?, .!!, etc."
  [token] (re-matches re/dotx (str token)))

(defn is-operator?
  "Infix operator tokens like +, -, *, /, %, %%, **, ==, !=, >, >=, <, <=, &&,
  ||, &&&, ||| etc."
  [token]
  (let [t (str token)]
    (and
      (re-matches re/osym t)
      (not= t "&"))))

(defn is-colon-calls?
  "A colon chain call token like a:b is short for a.b()."
  [token] (re-matches re/ksym (str token)))

(defn is-quote?
  "Return true when token is the YAMLScript quote token."
  [token] (re-matches re/quot (str token)))

(defn is-special?
  "Return true when token is a special reader token."
  [token] (re-matches re/spec (str token)))

(defn is-clojure-symbol?
  "Return true when token is accepted as a raw Clojure symbol."
  [token] (re-matches re/csym (str token)))

(defn is-regex?
  "Return true when token is a regular-expression literal."
  [token]
  (re-matches re/regx (str token)))

(defn is-string?
  "Return true when token is a double-quoted string literal."
  [token]
  (re-matches re/dstr (str token)))

(defn is-single?
  "Return true when token is a single-quoted string literal."
  [token]
  (re-matches re/sstr (str token)))

(defn is-fq-symbol?
  "Return true when token is a namespace-qualified YS symbol."
  [token]
  (re-matches re/fsym (str token)))

(defn is-alias-symbol?
  "Return true when token names a YAML alias reference."
  [token]
  (re-matches re/asym (str token)))

(defn is-symbol?
  "Return true when token is any supported YAMLScript symbol form."
  [token]
  (or
    (re-matches re/fsym (str token))
    (re-matches re/ysym (str token))
    (re-matches re/vsym (str token))
    (re-matches re/ssym (str token))
    (re-matches re/asym (str token))
    (re-matches re/splt (str token))))

(defn is-default-symbol?
  "Return true when token is an argument symbol with a default marker."
  [token]
  (re-matches re/dsym (str token)))

(defn is-symbol-paren?
  "Return true when token is a symbol immediately followed by an open paren."
  [token]
  (re-matches re/psym (str token)))

(def re-tokenize
  (re/re
    #"(?x)
    (?:
      $ccom |                 # Clojure comment
      $icom |                 # Inline comment
                              # Symbols and operators
      $ksym |                   # Colon chain calls
      $quot |                   # Quote token
      $keyw |                   # Keyword token
      $psym |                   # Symbol followed by paren
      $fsym |                   # Fully qualified symbol
      $nspc |                   # Namespace symbol
      $mnum |                   # Maybe a numeric literal token
      $char |                   # Character token
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
      $spec |                   # Special token
      $dotx |                   # Special dot operators  # 22
      $osym |                   # Operator symbol token
      $anon |                   # Anonymous fn start token
      $sett |                   # Set start token
      $dstr |                 # Double quoted string token
      $sstr |                 # Single quoted string token
                              # Other tokens
      .                         # Single character tokens
    )"))

(comment
  (re-seq re-tokenize "5.1.1.:x")
  (re-matches re/mnum "5.1.1.:x")
  (def regexes
    [re/ccom re/icom re/ksym re/quot re/spec re/char re/keyw re/psym re/fsym
     re/nspc re/mnum re/regx re/xsym re/dsym re/vsym re/ssym re/asym re/splt
     re/csym re/narg re/dotn re/dots re/dotx re/osym re/anon re/sett re/dstr
     re/sstr])
  (map-indexed
    (fn [i v] (prn [i v])
      (let [rgx (re-pattern (str "^" v))
            result (re-find rgx "5.inc()")]
        (when result (prn result) (die 123))))
    regexes)
  )

(defn split-colon-calls
  "Expand a colon-call token into dot-call token pieces."
  [token]
  (let [tokens (str/split token #":")
        [token1 token2 & xtokens] tokens
        [start tokens] (if (re-find #"\.$" token1)
                         [[(-> token1 butlast str/join)
                           "."
                           (str ":" token2)]
                          xtokens]
                         [[token1] (vec (rest tokens))])
        start (if (re-find #"_." (first start))
                [(str \" (first start) \")]
                start)]
    (reduce
      #(conj %1 "." (str %2 "(") ")")
      start
      tokens)))

(defn get-special-expansion
  "Return special expansion for the current context."
  [token]
  (let [expanded
        (condp = token
          ".("   ". call("
          ".@"   ". deref( )"
          ".$"   ". last( )"
          ".#"   ". count( )"
          ".?"   ". truey?( )"
          ".!"   ". falsey?( )"
          ".??"  ". boolean( )"
          ".!!"  ". not( )"
          ".--"  ". dec+( )"
          ".++"  ". inc+( )"
          ".>"   ". DBG( )"
          ".>>>" ". DBG( )"
          (die "Unsupported dot special operation: " token))]
    (str/split expanded #" ")))

(defn re-lex-tokens
  "Reprocess lex tokens for YAMLScript parsing."
  [tokens]
  (reduce (fn [acc token]
            (cond
              (= token "+++")
              (vec (concat acc ["(" "ys::std/stream" ")"]))
              ,
              (is-colon-calls? token)
              (vec (concat acc (re-lex-tokens (split-colon-calls token))))
              ,
              (is-dot-special? token)
              (vec (concat acc (re-lex-tokens (get-special-expansion token))))
              ,
              :else
              (conj acc token)))
    []
    tokens))

(defn lex-tokens
  "Lex tokens into YAMLScript tokens."
  [expr]
  (let [tokens (->> expr
                 (re-seq re-tokenize)
                 (remove #(re-matches re/ignr %1))
                 re-lex-tokens)]
    ;; XXX Might be too hot of a path to check here:
    (if (System/getenv "YS_SHOW_LEX")
      (WWW tokens)
      tokens)))

(declare read-form yes-expr)

(defn- conj-seq
  "Append one grouped sequence part to an accumulated form."
  [coll part & xs]
  (apply conj
    (cond-> coll
      (seq part)
      (conj (Form (yes-expr part))))
    xs))

(defn- qf
  "Quote a form unless it already represents quoted syntax."
  [form]
  (condf form
    :Sym (let [sym (-> form :Sym str)]
           (if (re-find #"^\$\w" sym)
             (Sym (subs sym 1))
             (QSym (:Sym form))))
    :Lst (update-in form [:Lst]
           (fn [list]
             (vec (map #(if (= {:Sym '_} %1)
                          (Sym '_)
                          %1) list))))
    form))

(defn fix-dot-chain
  "Normalize dot-chain forms before grouping them."
  [expr]
  (if-lets [list (or (get-in expr [0 :Lst]) expr)
            _ (= (first list) {:Sym '_dot_})]
    [{:dot (vec (rest list))}]
    expr))

(def sep (Sym '.))

(defn group-dots
  "Group dot-chain token forms into a single dot AST node."
  [forms]
  (fix-dot-chain
    (if (>= (count forms) 3)
      (loop [[a b c :as xs] forms, grp [], acc []]
        (cond
          (empty? xs)
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

(defn yes-expr
  "Build an infix yes-expression list from expression forms."
  [expr]
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

(defn anon-fn-arg-list
  "Infer an anonymous function argument vector from body usage."
  [node]
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

(defn read-anon-fn
  "Read anon fn tokens into AST form."
  [[_ & tokens]]
  (loop [tokens tokens
         list []]
    (when (not (seq tokens))
      (die "Unexpected end of input"))

    (if (= (first tokens) ")")
      (let [form (-> list group-dots yes-expr)
            form (if (and (= 1 (count form))
                       (:Rgx (first form)))
                   [(Sym 're-find) (first form) (Sym '_1)]
                   form)
            form (walk/prewalk-replace {{:Sym '_} {:Sym '_1}} form)
            args (anon-fn-arg-list form)
            expr (Lst [(Sym 'fn) (Vec [(Sym "&") (Vec args)]) (Form form)])]
        [expr (rest tokens)])
      (let [[form tokens] (read-form tokens)]
        (recur tokens (conj list form))))))

(defn read-list
  "Read list tokens into AST form."
  [[_ & tokens] type end sym]
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
        (recur tokens (if form (conj list form) list))))))

;; TODO do in one call
(defn str-unescape
  "Unescape YAMLScript double-quoted string content."
  [s]
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

(defn read-dq-string
  "Read a double-quoted string token into an AST form."
  [string]
  (let [build-xstr @yamlscript.global/build-xstr]
    (-> string
      (subs 1 (dec (count string)))
      str-unescape
      (#(hash-map :xstr %))
      build-xstr)))

(defn sstr-unescape
  "Unescape YAMLScript single-quoted string content."
  [s]
  (-> s
    (str/replace ":\\ " ": ")
    (str/replace " \\#" " #")))

(defn read-sq-string
  "Read a single-quoted string token into an AST form."
  [string]
  (-> string
    (subs 1 (dec (count string)))
    sstr-unescape
    (str/replace #"''" "'")
    Str))

(declare read-form)

(comment
  (read-string "\\\\Inf / \\\\-Inf")
  )
(defn read-scalar
  "Read one scalar token into an AST form."
  [[token & tokens]]
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
    (is-number? token) (let [token (str/replace token #"^([-+]?)0o"
                                     (str "$1" "0"))
                             token (str/replace token #"\\\\" "##")]
                         [(Num token) tokens])
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

(defn read-form
  "Read form tokens into AST form."
  [tokens]
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
      "\\{" (read-list tokens Set "}" nil)
      ,   (read-scalar tokens))))

(defn read-forms
  "Read forms tokens into AST form."
  [tokens]
  (loop [tokens tokens
         forms []]
    (if (seq tokens)
      (let [[form tokens] (read-form tokens)
            forms (if form (conj forms form) forms)]
        (recur tokens forms))
      forms)))

(defn read-string
  "Read string tokens into AST form."
  [string]
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
