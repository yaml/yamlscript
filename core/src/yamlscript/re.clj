;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.re library defines the regex parts that are used to match
;; YS ysexpr tokens.
;;
;; It defines an `re` function that takes a regex template and expands the
;; interpolations to create a regex pattern.

(ns yamlscript.re
  (:require
   [clojure.string :as str]
   [ys.v0.common]
   [ys.v0.re :as v0re])
  (:refer-clojure :exclude [char quot]))

(defn re
  "Expand regex template variables."
  [rgx]
  (loop [rgx (str rgx)]
    (let [match (re-find #"\$([a-zA-Z]+)" rgx)]
      (if match
        (let [var (second match)
              val (var-get
                    (resolve
                      (symbol (str "yamlscript.re/" var))))
              rgx (str/replace
                    rgx
                    (re-pattern (str #"\$" var #"(?![a-zA-Z])"))
                    (str/re-quote-replacement val))]
          (recur rgx))
        (re-pattern rgx)))))

(def char
  "A character literal token.
   Clojure uses a single backslash for escaping character literals.
   YS uses a double backslash for escaping character literals.
   Example: \\x or \\newline"
  #"(?x)(?:
    \\\\
    (?:
      newline |
      space |
      tab |
      formfeed |
      backspace |
      return |
      .
    ))")
(def tend "Token ending lookahead"
  #"(?=[\.\,\s\]\}\)]|$)")
(def ccom #"(?:;.*(?:\n|\z))")             ; Clojure comment
(def ignr #"(?x)
            (?:                            # Ignorables
              |                              # Empty
              \#\!.*\n? |                    # hashbang line
              [\s,]+    |                    # whitespace, commas,
            )")
#_(def spec #"(?:~@|[~@`^])")                ; Special token
(def spec #"(?:\@)")                       ; Special token
(def quot #"(?:\\')")                      ; Quote token
(def dotx #"(?x)                           # Dot special operator
            (?:\.
              (?:
                \( |
                \?{1,2} |
                \!{1,2} |
                \+\+ |
                \-\- |
                [\#\@] |
                \$(?!\w) |
                \> |
                \>\>\>
              )
            )")
(def dotn #"(?:\.-?\d+)")                  ; Dot operator followed by number
(def ukey #"(?:\w+(?:_\w+)+)")             ; Word with _ allowed

;; Numeric literal tokens for code mode (shared tokens live in ys.v0.re)

(def inum v0re/inum)
(def ibig v0re/ibig)
(def hnum v0re/hnum)
(def onum v0re/onum)
(def rnum v0re/rnum)
(def bnum v0re/bnum)
(def fnum v0re/fnum)
(def fbig v0re/fbig)
(def snum v0re/snum)
(def xnum v0re/xnum)
;; Maybe a number token
(def mnum (re #"(?x)
                (?:
                  $xnum
                  (?:\.[0-9])?
                  .*?
                  (?=[\:\.\,\s\]\}\)]|$)   # End of token
                )
              "))

(comment
  (re-find mnum "1.2.a.3.4.5")
  (re-find fnum "42.0")
  )

(def xsym #"(?:[=!]~~?)")                  ; Special operator token

;; Operator symbol token
(def osym #"(?x)
            (?:
              => |
              // |
              [-+*/<>] |
              \*\* |
              [=<>!]= |
              \%{1,2} |
              [&|]{2,3} |
              [=!]~~? |
              \.{1,3}
            )")

(def anon #"(?:\\\()")                     ; Anonymous fn start token
(def sett #"(?:\\\{)")                     ; Set start token
(def narg #"(?:[_%]\d+)")                  ; Numbered argument token
(def regx #"(?x)(?:                        # Regular expression
            / (?=\S)                         # opening slash
            (?:
              \\. |                          # Escaped char
              [^\\\/\n]                      # Any other char
            )+/                              # Ending slash
            )")
(def dstr #"(?x)(?:
            \"(?:                          # Double quoted string
              \\. |                          # Escaped char
              [^\\\"]                        # Any other char
            )*\"                             # Ending quote
            )")
(def sstr #"(?x)(?:
            '(?:                           # Single quoted string
              '' |                           # Escaped single quote
              [^']                           # Any other char
            )*'                              # Ending quote
            )")
(def icom (re #"(?:\\$dstr)"))             ; Inline comment token
(def pnum #"(?:\d+)")                      ; Positive integer
(def alph v0re/alph)                       ; Alpha
(def anum v0re/anum)                       ; Alphanumeric
(def symw v0re/symw)                       ; Symbol word
(def vsym (re #"(?:\$$symw|\$(?=\.))"))    ; Variable lookup symbol
(def ssym (re #"(?:\$\$|\$\#|\$)"))        ; Special symbols
(def keyw v0re/keyw)                       ; Keyword token
(def jsym #"(?:~\w+)")                     ; Java interop symbol
                                           ; Dot operator word with _ allowed
(def dots (re #"(?:(?:\.(?:$jsym|$ukey))$tend)"))
                                           ; Clojure symbol
(def csym #"(?:[-a-zA-Z0-9_*+?!<=>$]+(?:\.(?=\ ))?)")
(def ysym (re #"(?:$symw[+?!]?|_)"))       ; YS symbol token
(def splt (re #"(?:$ysym\*)"))             ; Splat symbol
(def asym (re #"(?:\*$symw)"))             ; Alias symbol
(def dsym (re #"(?:$symw=)"))              ; YS symbol with default
(def nspc (re #"(?:$symw(?:\:\:$symw)+)")) ; Namespace symbol
(def fsym (re #"(?:(?:$nspc|$symw)/$ysym)"))  ; Fully qualified symbol
(def psym (re #"(?:(?:$fsym|$ysym|$jsym)\()"))   ; Symbol followed by paren
                                           ; Colon calls
(def ksym (re #"(?x)
                (?:
                  (?:
                    $fsym |
                    \$? $ysym |
                    $xnum |
                    $ukey |
                    [\)\]\}] |
                    \.
                      (?:
                        \d+ |
                        \# |
                        \-\- |
                        \+\+ |
                        \?\?? |
                        \!\!?
                      )
                  )
                  (?:
                    \.?
                    \:
                    (?:
                      (?:
                        $nspc |
                        $symw
                      ) /
                    )?
                    (?:
                      $jsym |
                      $symw [+?!]?
                    )
                  )+
                )"))

;; Pair key for def/let call
(def defk (re #"(?x)
                (?:
                  (
                    (?:
                      \[.*\] |
                      \{.*\} |
                      $symw  |
                      _
                    )
                    .*?
                  )
                  \ +
                  ((?:
                    \|\|\|? |
                    [-+*/.] |
                    \*\*
                  )?)
                  =
                )"))  ;;"

(def dfnk (re #"(?:^(defn-?) +($ysym)(?:\((.*)\))?$)")) ; Pair key for defn call
(def afnk (re #"(?:^(fn)( +$ysym)?(?:\((.*)\))?$)"))    ; Pair key for a fn call

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

(comment
  )
