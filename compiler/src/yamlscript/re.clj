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

(def char #"\\.")                  ; Character token
(def comm #";.*(?:\n|\z)")         ; Comment token
(def ignr #"(?x)
            (?:                    # Ignorables
              \#\!.*\n? |            # hashbang line
              [\s,]+    |            # whitespace, commas,
              ;.*\n?                 # comments
            )")
(def keyw #"(?:\:\w+(?:-\w+)*)")   ; Keyword token
(def lnum #"-?\d+")                ; Integer token
(def oper #"[-+*/<=>|&]{1,3}")     ; Operator token
(def strg #"(?x)
            \#?                    # Possibly a regex
            \"(?:                  # Quoted string
              \\. |                  # Escaped char
              [^\\\"]                # Any other char
            )*\"?                    # Ending quote
            ")
(def symb #"\w+(?:-\w+)*[?!]?")    ; Symbol token
(def symp #"\w+(?:-\w+)*[?!]?\(")  ; Symbol followed by paren

(comment
  )
