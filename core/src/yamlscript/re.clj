;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.re library defines the regex parts that are used to match
;; YAMLScript ysexpr tokens.
;;
;; It defines an `re` function that takes a regex template and expands the
;; interpolations to create a regex pattern.

(ns yamlscript.re
  (:require
   [clojure.string :as str]
   [yamlscript.debug :refer [www]])
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

(def char #"(?x)
            \\\\
            (?:
              newline |
              space |
              tab |
              formfeed |
              backspace |
              return |
              .
            )")                            ; Character token
(def tend #"(?=[\.\,\s\]\}\)]|$)")        ; End of token
(def comm #";.*(?:\n|\z)")                 ; Comment token
(def ignr #"(?x)
            (?:                            # Ignorables
              |                              # Empty
              \#\!.*\n? |                    # hashbang line
              [\s,]+    |                    # whitespace, commas,
              ;.*\n?                         # comments
            )")
(def spec #"(?:~@|[~@`^])")                ; Special token
(def quot #"(?:\\')")                      ; Quote token
(def dotn #"(?:\.-?\d+)")                  ; Dot operator followed by number
(def dots (re #"(?:\.\w+(?:_\w+)+)$tend")) ; Dot operator word with _ allowed
(def mnum #"(?:[-+]?\d[-+/*%.:\w]+)")      ; Maybe Number token
(def inum #"-?\d+")                        ; Integer literal token
(def fnum (re #"$inum\.\d*(?:e$inum)?"))   ; Floating point literal token
(def xnum (re #"(?:$fnum|$inum)"))         ; Numeric literal token
(def xsym #"(?:\=\~)")                     ; Special operator token
(def osym #"(?:[-+*/%<>!=~|&.]{1,3})")     ; Operator symbol token
(def anon #"(?:\\\()")                     ; Anonymous fn start token
(def narg #"(?:%\d+)")                     ; Numbered argument token
(def regx #"(?x)                           # Regular expression
            / (?=\S)                         # opening slash
            (?:
              \\. |                          # Escaped char
              [^\\\/\n]                      # Any other char
            )+/                              # Ending slash
            ")
(def dstr #"(?x)
            \"(?:                          # Double quoted string
              \\. |                          # Escaped char
              [^\\\"]                        # Any other char
            )*\"                             # Ending quote
            ")
(def sstr #"(?x)
            '(?:                           # Single quoted string
              '' |                           # Escaped single quote
              [^']                           # Any other char
            )*'                              # Ending quote
            ")
(def pnum #"(?:\d+)")                      ; Positive integer
(def anum #"[a-zA-Z0-9]")                  ; Alphanumeric
(def symw (re #"(?:$anum+(?:->?$anum+)*)"))  ; Symbol word
(def vsym (re #"(?:\$$symw|\$(?=\.))"))    ; Variable lookup symbol
(def ssym (re #"(?:\$\$|\$\#|\$)"))        ; Special symbols
(def keyw (re #"(?:\:$symw)"))             ; Keyword token
                                           ; Clojure symbol
(def csym #"(?:[-a-zA-Z0-9_*+?!<=>$]+(?:\.(?=\ ))?)")
(def ysym (re #"(?:$symw[?!.]?|_)"))       ; YS symbol token
(def splt (re #"(?:$ysym\*)"))             ; Splat symbol
(def asym (re #"(?:\*$symw)"))             ; Alias symbol
(def dsym (re #"(?:$symw=)"))              ; YS symbol with default
(def nspc (re #"(?:$symw(?:\:\:$symw)+)")) ; Namespace symbol
(def fsym (re #"(?:(?:$nspc|$symw)\/$ysym)"))  ; Fully qualified symbol
                                           ; Symbol followed by paren
(def psym (re #"(?:(?:$fsym|$ysym)\()"))

(def eqop (re #"(?:\|\||[-+*/.])"))
(def defk (re #"(\[.*\]|\{.*\}|$symw) +($eqop?)="))  ; Pair key for def/let call
(def dfnk (re #"^defn ($ysym)(?:\((.*)\))?$")) ; Pair key for defn call
(def afnk (re #"^fn ($ysym)(?:\((.*)\))?$"))   ; Pair key for a fn call

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
  www
  )
