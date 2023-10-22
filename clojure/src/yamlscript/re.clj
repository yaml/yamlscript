(ns yamlscript.re
  (:require
   [clojure.string :as str])
  (:refer-clojure :exclude [char]))

(def char #"\\.")                  ; Character token
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

(def dict
  {
   :char char
   :ignr ignr
   :keyw keyw
   :lnum lnum
   :oper oper
   :strg strg
   :symb symb
   :symp symp
   })

(defn re [s]
  (re-pattern
    (reduce
      (fn [re [k v]]
        (let [pat (re-pattern (str #"\$" (subs (str k) 1) #"(?!\w)"))]
          (str/replace re pat (str/re-quote-replacement v))))
      s dict)))

(comment)
