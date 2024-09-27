;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.debug library defines a set of Clojure debugging functions.

(ns yamlscript.debug
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [yamlscript.global :as global]
   [yamlscript.util :as util])
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
  (util/die ""))

(defn YYY [& values]
  (print (dump values))
  (flush)
  (last values))

; TODO Turn on stack trace printing
(defn ZZZ [& values]
  (apply DBG values)
  (swap! global/opts assoc :stack-trace true)
  (util/die ""))

(def ttt-ctr (atom 0))

(defn ttt-fmt [xs]
  (str/join ", "
    (for [x xs]
      (cond
        (util/macro? x) (str "'" x)
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
            (str (subs s 0 50) "...(" (util/type-name x) " "
              (count (take 999 x)) " items)")
            s))
        ,
        (coll? x)
        (let [s (str "'" (pr-str (take 30 x)))]
          (if (> (count s) 50)
            (str (subs s 0 50) "...(" (util/type-name x) " "
              (count (take 999 x)) " items)")
            s))
        ,
        (nil? x) "nil"
        (char? x) (pr-str x)
        (symbol? x) (str "'" x)
        :else (str x)))))

(def ys-macros
  '(&&&
     |||
     and?
     call
     +def
     each
     or?
     q
     qw
     source
     use
     value
     TTT
     clojure.core/DBG))

(def clj-specials
  '(catch
    def
    let
     if
     do
     quote
     recur))

(def skip-trace (concat ys-macros clj-specials))

(defmacro TTT [form]
  (let [[fun & args] form
        name (when (and
                     (not (util/macro? fun))
                     (not (some #{fun} skip-trace)))
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
          (util/eprintln (str @ttt-ctr " >>> " ~name
                      "(" (ttt-fmt xs#) ")"))
          (apply ~fun xs#))
        ~@args)
      `(do
         (swap! ttt-ctr inc)
         (when ~fname
           (util/eprintln (str @ttt-ctr " >>> " ~fname
                       "(" ~fargs ")")))
         (~@form)))))

(comment
  (macroexpand '(TTT (q USER)))
  (ns-resolve 'clojure.core 'let)
  )
