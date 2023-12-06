;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.re library defines the regex parts that are used to match
;; YAMLScript ysexpr tokens.
;;
;; It defines an `re` function that takes a regex template and expands the
;; interpolations to create a regex pattern.

(ns yamlscript.re
  (:require
   [clojure.string :as str]
   [yamlscript.debug :refer :all])
  (:refer-clojure :exclude [char]))

(defn re [rgx]
  (loop [rgx (str rgx)]
    (let [match (re-find #"\$(\w+)" rgx)]
      (if match
        (let [var (second match)
              val (var-get
                    (resolve
                      (symbol (str "yamlscript.re/" var))))
              rgx (str/replace
                    rgx
                    (re-pattern (str #"\$" var #"(?!\w)"))
                    (str/re-quote-replacement val))]
          (recur rgx))
        (re-pattern rgx)))))

(def char #"(?x)
            \\
            (?:
              newline |
              space |
              tab |
              formfeed |
              backspace |
              return |
              .
            )")                            ; Character token
(def comm #";.*(?:\n|\z)")                 ; Comment token
(def ignr #"(?x)
            (?:                            # Ignorables
              \#\!.*\n? |                    # hashbang line
              [\s,]+    |                    # whitespace, commas,
              ;.*\n?                         # comments
            )")
(def lnum #"-?\d+")                        ; Integer token
                                           ; Operator token
(def oper #"(?:[-+*/<=>|&]{1,3}|\.\.|\|\||&&)")
(def strg #"(?x)
            \#?                            # Possibly a regex
            \"(?:                          # Quoted string
              \\. |                          # Escaped char
              [^\\\"]                        # Any other char
            )*\"?                            # Ending quote
            ")
(def symw #"(?:\w+(?:-\w+)*)")             ; Symbol word
(def perc #"(?:\%(?:[1-9]|1[0-9]|20)?)")          ; Percent symbol (for lambdas)
(def keyw (re #"(?:\:$symw)"))             ; Keyword token
(def symb (re #"(?:$symw[?!]?)"))          ; Symbol token
(def nspc (re #"(?:$symw(?:\.$symw)*)"))   ; Namespace symbol
(def fqsm (re #"(?:$nspc/$symb)"))         ; Fully qualified symbol
(def symp (re #"(?:(?:$fqsm|$symb)\()"))   ; Symbol followed by paren
(def dyns (re #"(?:\*$symw\*)"))           ; Dynamic symbol
; Balanced parens
(def bpar #"(?x)
            (?:\(
              [^)(]*(?:\(
                [^)(]*(?:\(
                  [^)(]*(?:\(
                    [^)(]*(?:\(
                      [^)(]*(?:\(
                        [^)(]*
                      \)[^)(]*)*
                    \)[^)(]*)*
                  \)[^)(]*)*
                \)[^)(]*)*
              \)[^)(]*)*
            \))
          ")

(def re-ysi
  (re
    #"(?sx)
    (?:
      \$ $symw |
      \$ $bpar |
      .+?(?= \$ $symw | \$ $bpar | $)
    )"))

(comment
  re-ysi
  (re-seq re-ysi "foo $(bar()) , -1 $baz")
  (re-seq bpar "(a(b(c(d))e)f(g(h)i)j(k(l)m)n)o(p(q)r)s(t(u)v)w(x(y)z))")
  )
