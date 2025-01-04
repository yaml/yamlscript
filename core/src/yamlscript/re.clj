;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.re library defines the regex parts that are used to match
;; YAMLScript ysexpr tokens.
;;
;; It defines an `re` function that takes a regex template and expands the
;; interpolations to create a regex pattern.

(ns yamlscript.re
  (:require
   [clojure.string :as str]
   [yamlscript.common])
  (:refer-clojure :exclude [char quot]))

(defn re [rgx]
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

(def char #"(?x)(?:
            \\\\
            (?:
              newline |
              space |
              tab |
              formfeed |
              backspace |
              return |
              .
            ))")                           ; Character token
(def tend #"(?=[\.\,\s\]\}\)]|$)")         ; End of token
(def ccom #"(?:;.*(?:\n|\z))")             ; Clojure comment
(def ignr #"(?x)
            (?:                            # Ignorables
              |                              # Empty
              \#\!.*\n? |                    # hashbang line
              [\s,]+    |                    # whitespace, commas,
            )")
(def spec #"(?:~@|[~@`^])")                ; Special token
(def quot #"(?:\\')")                      ; Quote token
(def dotx #"(?x)                           # Dot special operator
            (?:\.
              (?:
                \?{1,2} |
                \!{1,2} |
                \+\+ |
                \-\- |
                \# |
                \$(?!\w) |
                \> | \>\>\>
              )
            )")
(def dotn #"(?:\.-?\d+)")                  ; Dot operator followed by number
(def ukey #"(?:\w+(?:_\w+)+)")             ; Word with _ allowed
(def inum #"(?:-?\d+)")                    ; Integer literal token
(def fnum (re #"(?:$inum\.\d+(?:e$inum)?)"))   ; Floating point literal token
(def xnum (re #"(?:$fnum|$inum)"))         ; Numeric literal token
                                           ; Maybe Number token
(def mnum (re #"(?x)
                (?: $xnum
                  (?:[-+/*%_] \d* |
                     \.\d+ |
                     \.\w+ [\?\!]? (?=[^\(\w\?\!] | $)
                  )*
                )
              "))
(def xsym #"(?:\=\~|!~)")                  ; Special operator token
(def osym #"(?:[-+*/%<>!=~|&.]{1,3})")     ; Operator symbol token
(def anon #"(?:\\\()")                     ; Anonymous fn start token
(def sett #"(?:\\\{)")                     ; Set start token
(def narg #"(?:%\d+)")                     ; Numbered argument token
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
(def alph #"(?:[a-zA-Z])")                 ; Alpha
(def anum #"(?:[a-zA-Z0-9])")              ; Alphanumeric
(def symw (re #"(?:$alph$anum*(?:-$anum+)*)"))  ; Symbol word
(def vsym (re #"(?:\$$symw|\$(?=\.))"))    ; Variable lookup symbol
(def ssym (re #"(?:\$\$|\$\#|\$)"))        ; Special symbols
(def keyw (re #"(?:\:$symw)"))             ; Keyword token
(def dots (re #"(?:(?:\.$ukey)$tend)"))    ; Dot operator word with _ allowed
                                           ; Clojure symbol
(def csym #"(?:[-a-zA-Z0-9_*+?!<=>$]+(?:\.(?=\ ))?)")
(def ysym (re #"(?:$symw[+?!]?|_)"))       ; YS symbol token
(def splt (re #"(?:$ysym\*)"))             ; Splat symbol
(def asym (re #"(?:\*$symw)"))             ; Alias symbol
(def dsym (re #"(?:$symw=)"))              ; YS symbol with default
(def nspc (re #"(?:$symw(?:\:\:$symw)+)")) ; Namespace symbol
(def fsym (re #"(?:(?:$nspc|$symw)/$ysym)"))  ; Fully qualified symbol
(def psym (re #"(?:(?:$fsym|$ysym)\()"))   ; Symbol followed by paren
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
                    )
                    ?$symw [+?!]?
                  )+
                )"))

(def eqop (re #"(?:\|\|\|?|[-+*/.]|\*\*)"))
                                           ; Pair key for def/let call
(def defk (re #"(?:((?:\[.*\]|\{.*\}|$symw).*?) +($eqop?)=)"))
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
