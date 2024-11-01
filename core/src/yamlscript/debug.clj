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

(def width 50)

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
    (and (string? value)
      (re-find #"\n." value) (> (count (pr-str value)) width))
    (str "\"\"\"\\\n" value "\"\"\"")
    :else (if (> (count (pr-str value)) 80)
            (str/trim-newline
              (with-out-str
                (pp/pprint (condf value
                             map? (into (sorted-map) value)
                             set? (apply sorted-set value)
                             value))))
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

(defn remove-ttt [s]
  (let [tokens (re-seq #"(?:\".*?\"|\(TTT |[()]|[^()\"]+)" s)
        list
        (loop [tokens tokens list [] level 0 levels '()]
          (let [[token & tokens] tokens]
            (condp = token
              nil list
              "(TTT "
              (recur tokens list (inc level) (cons (inc level) levels))
              "("
              (recur tokens (conj list token) (inc level) levels)
              ")"
              (recur
                tokens (if (= (first levels) level)
                         list (conj list token))
                (dec level) (if (= (first levels) level)
                              (rest levels) levels))
              (recur tokens (conj list token) level levels))))]
    (str/join "" (remove nil? list))))

(defn ttt-fmt
  ([xs width]
   (str/join ", "
     (for [x xs]
       (cond
         (util/macro? x) (str "'" x)
         (fn? x) (str x)
         ,
         (string? x)
         (let [s (pr-str x)
               s (str/replace s "\n" "\\n")]
           (if (> (count x) width)
             (str (subs s 0 width) "...(" (count x) " chars)")
             s))
         ,
         (or (vector? x) (map? x) (set? x))
         (let [s (pr-str x)]
           (if (> (count s) width)
             (str (subs s 0 width) "...(" (util/type-name x) " "
               (count (take 999 x)) " items)")
             s))
         ,
         (coll? x)
         (let [s (str (pr-str (take 30 x)))]
           (if (> (count s) width)
             (str (subs s 0 width) "...(" (util/type-name x) " "
               (count (take 999 x)) " items)")
             s))
         ,
         (nil? x) "nil"
         (char? x) (pr-str x)
         (symbol? x) (str "'" x)
         :else (str x)))))
  ([s width _]
   (let [width 100
         t (ttt-fmt s (* 2 width))
         t (remove-ttt t)]
     (if (> (count t) width)
       (str (subs t 0 width) "...(" (count s) " chars)")
       t))))

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
  '(def do if let quote recur try))

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
        fargs (ttt-fmt args width true)]
    (if name
      `((fn [& xs#]
          (swap! ttt-ctr inc)
          (util/eprintln (str "+" @ttt-ctr " >>> " ~name
                      "(" (ttt-fmt xs# width) ")"))
          (apply ~fun xs#))
        ~@args)
      `(do
         (swap! ttt-ctr inc)
         (when ~fname
           (util/eprintln (str "+" @ttt-ctr " >>> " ~fname
                       "(" ~fargs ")")))
         (~@form)))))

(comment
  (macroexpand '(TTT (q USER)))
  (ns-resolve 'clojure.core 'let)
  )
