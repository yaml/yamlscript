;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.debug library defines a set of Clojure debugging functions.

(ns yamlscript.debug
  (:require [clojure.pprint :as pp]))

(defn dump [o]
  (str
    "---\n"
    (with-out-str
      (pp/pprint o))
    "...\n"))

(defn xxx [& o]
  (let [o (if (= 1 (count o)) (first o) o)]
    (throw (Exception. ^String (dump o)))))

(defn www [& o]
  (let [l (last o)
        o (if (= 1 (count o)) (first o) o)]
    ;; XXX need to print to stderr (with sci)
    (binding [*out* *out*]
      (print (dump o)))
    l))

(defn xxx+ [& o]
  (binding [*print-meta* true]
    (apply xxx o)))

(defn www+ [& o]
  (binding [*print-meta* true]
    (apply www o)))

(comment
  (www {:a 1 :b 2})
  (xxx {:a 1 :b 2})
  (www+ {:a 1 :b 2})
  (xxx+ {:a 1 :b 2})
  )
