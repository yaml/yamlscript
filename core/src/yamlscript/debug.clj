;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.debug library defines a set of Clojure debugging functions.

(ns yamlscript.debug
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [yamlscript.util :refer [die]]
   [yamlscript.common :as common])
  (:refer-clojure :exclude [YSC DBG PPP WWW XXX YYY ZZZ]))

(defn YSC [ys-str]
  (let [compile (var-get (resolve 'yamlscript.compiler/compile))
        pretty-format (var-get (resolve 'yamlscript.compiler/pretty-format))]
    (binding [*ns* (find-ns 'yamlscript.compiler)]
      (let [code (eval
                   (->
                     (if (re-find #"!yamlscript/v0" ys-str)
                       ys-str
                       (str "!yamlscript/v0\n" ys-str))
                     compile
                     pretty-format
                     (str/replace #"(?m)^\(\+\+\+ +(.*)\)$" "$1")
                     (str/replace #"(?s)^\(\+\+\+[ \n]+(.*)\)$" "$1")
                     (str/trim-newline)))]
        code))))

(defn fmt [value]
  (cond
    (and (string? value) (re-find #"\n." value) (> (count (pr-str value)) 50))
    (str "\"\"\"\\\n" value "\"\"\"")
    :else (if (> (count (pr-str value)) 80)
            (str/trim-newline
              (with-out-str
                (pp/pprint (cond (map? value) (into (sorted-map) value)
                                 (set? value) (apply sorted-set value)
                                 :else value))))
            (pr-str value))))

(defn- dump [values]
  (let [parts (map fmt values)
        text (str/join "\n" parts)]
    (if (re-find #"\n" text)
      (str ">>>\n" text "\n<<<\n")
      (str ">>>" text "<<<\n"))))

(defn PPP [& values]
  (print (dump values))
  (flush)
  (last values))

(defn DBG [& values]
  (binding [*out* *err*]
    (print (dump values))
    (flush))
  (last values))

(defn WWW [& values]
  (apply DBG values))

(defn XXX [& values]
  (apply DBG values)
  (die ""))

(defn YYY [& values]
  (print (dump values))
  (flush)
  (last values))

; TODO Turn on stack trace printing
(defn ZZZ [& values]
  (apply DBG values)
  (swap! common/opts assoc :stack-trace true)
  (die ""))

(intern 'clojure.core 'DBG YSC)
(intern 'clojure.core 'DBG DBG)
(intern 'clojure.core 'PPP PPP)
(intern 'clojure.core 'WWW WWW)
(intern 'clojure.core 'XXX XXX)
(intern 'clojure.core 'YYY YYY)
(intern 'clojure.core 'ZZZ ZZZ)

(comment
  )
