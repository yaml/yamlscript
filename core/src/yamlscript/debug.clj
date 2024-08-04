;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.debug library defines a set of Clojure debugging functions.

(ns yamlscript.debug
  (:require
   [clojure.pprint :as pp]
   [clj-yaml.core :as yaml]
   [yamlscript.util :refer [die]])
  (:refer-clojure :exclude [PPP WWW XXX YYY ZZZ]))

(defn -dump [o]
  (let
   [o (if (= 1 (count o)) (first o) o)]
    (str
      "<<<\n"
      (with-out-str
        (pp/pprint o))
      ">>>\n")))

(defn PPP [& o]
  (let [l (last o)]
    (print (-dump o))
    (flush)
    l))

(intern 'clojure.core 'PPP PPP)

(defn WWW [& o]
  (let [l (last o)]
    (binding [*out* *err*]
      (print (-dump o))
      (flush))
    l))

(intern 'clojure.core 'WWW WWW)
(intern 'clojure.core '_DBG WWW)

(defn XXX [& o]
  (die "\n" (-dump o)))

(intern 'clojure.core 'XXX XXX)

(defn YYY [& o]
  (let [l (last o)
        o (if (= 1 (count o)) (first o) o)]
    (print (yaml/generate-string o))
    (flush)
    l))

(intern 'clojure.core 'YYY YYY)

; TODO Turn on stack trace printing
(defn ZZZ [& o]
  (die "\n" (-dump o)))

(intern 'clojure.core 'ZZZ ZZZ)

(comment
  (WWW {:a 1 :b 2})
  (XXX {:a 1 :b 2})
  )
