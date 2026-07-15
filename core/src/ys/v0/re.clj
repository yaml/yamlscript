;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The ys.v0.re namespace defines the regex tokens shared between the YS
;; standard library and the YS compiler. The compiler-only tokens live in
;; yamlscript.re, which composes over these.

(ns ys.v0.re
  (:require
   [clojure.string :as str]))

(defn re
  "Expand regex template variables."
  [rgx]
  (loop [rgx (str rgx)]
    (let [match (re-find #"\$([a-zA-Z]+)" rgx)]
      (if match
        (let [var (second match)
              val (var-get
                    (resolve
                      (symbol (str "ys.v0.re/" var))))
              rgx (str/replace
                    rgx
                    (re-pattern (str #"\$" var #"(?![a-zA-Z])"))
                    (str/re-quote-replacement val))]
          (recur rgx))
        (re-pattern rgx)))))

;; Numeric literal tokens

;; Integer literal token
(def inum #"(?:[-+]?(?:0|[1-9][0-9]*))")
;; Big integer literal token
(def ibig (re #"(?:(?:$inum)N)"))
;; Hexadecimal literal token
(def hnum #"(?:[-+]?0x[0-9a-fA-F]+)")
;; Octal literal token
(def onum #"(?:[-+]?0o[0-7]+)")
;; Rational literal token
(def rnum #"(?:[-+]?[0-9]+/[0-9]+)")
;; Radix integer literal token
(def bnum #"(?:[-+]?(?:[2-9]|[12][0-9]|3[0-6])r[0-9a-zA-Z]+)")
;; Floating point literal token
(def fnum (re #"(?:$inum\.[0-9]+(?:[eE]$inum)?)"))
;; Big floating point literal token
(def fbig (re #"(?:(?:$fnum|$inum\.?)M)"))
;; Special number literal token
(def snum #"(?:\\\\(?:Inf|-Inf|NaN))")
;; Numeric literal token
(def xnum (re #"(?:$fbig|$fnum|$hnum|$onum|$rnum|$bnum|$ibig|$inum|$snum)"))

;; Symbol tokens

(def alph #"(?:[a-zA-Z])")                 ; Alpha
(def anum #"(?:[a-zA-Z0-9])")              ; Alphanumeric
(def symw (re #"(?:$alph$anum*(?:-$anum+)*)"))  ; Symbol word
(def keyw (re #"(?:\:$symw)"))             ; Keyword token

(comment
  )
