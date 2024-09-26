;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.debug library defines a set of Clojure debugging functions.

(ns yamlscript.debug
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [yamlscript.debug :as debug]
   [yamlscript.util :refer [die macro? when-lets]]
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

;;------------------------------------------------------------------------------
;; XXX move to common
(defn eprintln [& xs]
  (binding [*out* *err*]
    (apply println xs)))

(def ttt-ctr (atom 0))

(defn type-name [x]
  (cond
    (map? x) "Map"
    (set? x) "Set"
    (vector? x) "Vector"
    (list? x) "List"
    (seq? x) "Seq"
    :else (type x)))
;;------------------------------------------------------------------------------
#_(defn find-var-by-value [x]
  (let [all-the-vars (mapcat (fn [ns]
                               (vals (ns-publics ns)))
                             (all-ns))]
    (first (filter (fn [var]
                     (identical? x @var)) all-the-vars))))
#_(time (prn (meta (find-var-by-value inc))))

(defn ttt-fmt [xs]
  (str/join ", "
    (for [x xs]
      (cond
        (macro? x) (str "'" x)
        (fn? x) (str x)
        ,
        (string? x)
        (let [s (pr-str x)
              s (str/replace s "\n" "\\n")]
          (if (> (count x) 50)
            (str (subs s 0 50) "...(" (count x) " chars)")
            s))
        ,
        (or (vector? x) (map? x) (set? x))
        (let [s (pr-str x)]
          (if (> (count s) 50)
            (str (subs s 0 50) "...(" (type-name x) " "
              (count (take 999 x)) " items)")
            s))
        ,
        (coll? x)
        (let [s (str "'" (pr-str (take 30 x)))]
          (if (> (count s) 50)
            (str (subs s 0 50) "...(" (type-name x) " "
              (count (take 999 x)) " items)")
            s))
        ,
        (nil? x) "nil"
        (char? x) (pr-str x)
        (symbol? x) (str "'" x)
        :else (str x)))))

(def ys-macros
  '(&&& ||| and? call +def each or? q qw source use value
     TTT clojure.core/DBG))

(def clj-specials '(catch def let if do quote recur))

; bakk-account  bottle-song lazy-primes secret-handshake
(def skip-trace
  (concat ys-macros
    '(TTT clojure.core/DBG
       catch def let if do quote recur)))

(defmacro TTT [form]
  (let [[fun & args] form
        name (when (and
                     (not (macro? fun))
                     (not (some #{fun} debug/skip-trace)))
               (-> fun
                 str
                 (str/replace #"@.*" "")))
        fname (when-lets [fname (str fun)
                          _ (not= fname "TTT")]
                fname)
        fargs (str/replace (ttt-fmt args) #"\(TTT " "")]
    (if name
      `((fn [& xs#]
          (swap! ttt-ctr inc)
          (eprintln (str @ttt-ctr " >>> " ~name
                      "(" (ttt-fmt xs#) ")"))
          (apply ~fun xs#))
        ~@args)
      `(do
         (swap! ttt-ctr inc)
         (when ~fname
           (eprintln (str @ttt-ctr " >>> " ~fname
                       "(" ~fargs ")")))
         (~@form)))))

(comment
  (macroexpand '(TTT (q USER)))
  (ns-resolve 'clojure.core 'let)
  )

#_(defmacro TTT [form] (WWW form))

(intern 'clojure.core 'YSC YSC)
(intern 'clojure.core 'DBG DBG)
(intern 'clojure.core 'PPP PPP)
(intern 'clojure.core 'WWW WWW)
(intern 'clojure.core 'XXX XXX)
(intern 'clojure.core 'YYY YYY)
(intern 'clojure.core 'ZZZ ZZZ)

(comment
  )
