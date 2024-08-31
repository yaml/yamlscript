;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; This is the YAMLScript standard library.

(ns ys.std
  (:require
   [yamlscript.debug]
   [babashka.fs :as fs]
   [babashka.http-client :as http]
   [babashka.process :as process]
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [flatland.ordered.map]
   [ys.ys :as ys]
   [yamlscript.common :as common]
   [yamlscript.util :as util])
  (:refer-clojure :exclude [print
                            reverse
                            replace]))


(declare die)
;; Guard against billion laughs style attacks
(def _max-alias-size (* 1024 1024))


;;------------------------------------------------------------------------------
;; TODO fix that this prints _X as well
;; TODO Move to yamlscript.debug
;;------------------------------------------------------------------------------
(defmacro _X [xs]
  (let [[fun# & args#] xs
        args# (map pr-str args#)
        #_#_args# (map (fn [x]
                     (let [y (str/replace x #"\(_X " "")
                           n (/ (- (count x) (count y)) 4)]
                       (subs y 0 (- (count y) n)))) args#)
        args# (str/join " -> " args#)]
    `(do
       (clojure.core/print
         ";;" '~fun# "->" ~args# "\n")
       (~@xs))))


;;------------------------------------------------------------------------------
;; Special functions
;;------------------------------------------------------------------------------

;; Used to run a YAMLScript file as a Bash script:
(defmacro source [& xs])


;;------------------------------------------------------------------------------
;; Short named function aliases
;;------------------------------------------------------------------------------

(intern 'ys.std 'a clojure.core/identity)

(intern 'ys.std 'fun clojure.core/partial)

(intern 'ys.std 'just clojure.core/identity)

(intern 'ys.std 'len clojure.core/count)


;;------------------------------------------------------------------------------
;; Quoting functions
;;------------------------------------------------------------------------------

(defmacro q [x] `(quote ~x))
(defn qr [s] (re-pattern s))
(defmacro qw [& xs]
  `(vec (map (fn [w#]
               (cond
                 (nil? w#) "nil"
                 ,
                 (not (re-matches #"^[-\w]+$" (str w#)))
                 (clojure.core/die (str "Invalid qw word: '" w# "'"))
                 ,
                 :else (str w#)))
          '(~@xs))))


;;------------------------------------------------------------------------------
;; Alternate truth functions
;;------------------------------------------------------------------------------

(defn falsey? [x]
  (cond
    (number? x) (zero? x)
    (seqable? x) (empty? x)
    x false
    :else true))

(defn truey? [x] (not (falsey? x)))

(defmacro or?
  ([] nil)
  ([x] (if (truey? x) x nil))
  ([x & next]
      `(if (truey? ~x) ~x (or? ~@next))))

(defmacro ||| [x & xs] `(or? ~x ~@xs))

(defmacro and?
  ([] true)
  ([x] (if (truey? x) x nil))
  ([x & next]
      `(if (truey? ~x) (and? ~@next) nil)))

(defmacro &&& [x & xs] `(and? ~x ~@xs))

(comment
  (||| 0 42) ;; 42
  (&&& 42 0) ;; nil
  (||| 0 42 99) ;; 42
  (&&& 42 99) ;; 99
  )


;;------------------------------------------------------------------------------
;; Named function aliases for infix operators
;; TODO make these be polymorphic
;;------------------------------------------------------------------------------
(intern 'ys.std 'eq clojure.core/=)
(intern 'ys.std 'ne clojure.core/not=)
(intern 'ys.std 'gt clojure.core/>)
(intern 'ys.std 'ge clojure.core/>=)
(intern 'ys.std 'lt clojure.core/<)
(intern 'ys.std 'le clojure.core/<=)


;;------------------------------------------------------------------------------
;; Common type conversion functions
;;------------------------------------------------------------------------------
(defn to-bool [x] (boolean x))
(defn to-booly [x] (if (truey? x) true false))
(defn to-float [x] (parse-double x))
(defn to-int [x] (parse-long x))
(defn to-list [x] (apply list x))

(defn to-map
  ([] {})
  ([x] (apply hash-map x))
  ([k v & xs] (apply hash-map k v xs)))

(defn to-num [x]
  (cond
    (ratio? x) (double x)
    (number? x) x
    (string? x) (or (if (re-find #"\." x)
                      (parse-double x)
                      (parse-long x))
                  0)
    (seqable? x) (count x)
    (boolean? x) (if x 1 0)
    :else (die (str "Can't convert " (type x) " to number"))))

(defn to-set
  ([] (set []))
  ([x] (if (map? x)
         (set (keys x))
         (set x)))
  ([x & xs] (apply set x xs)))

(defn to-vec
  ([] [])
  ([x] (if (map? x)
         (vec (flatten (seq x)))
         (vec x)))
  ([x & xs] (apply vector x xs)))


;;------------------------------------------------------------------------------
;; Math functions
;;------------------------------------------------------------------------------
(intern 'ys.std 'sum clojure.core/+)
(intern 'ys.std 'sub clojure.core/-)
(intern 'ys.std 'mul clojure.core/*)

(defn div
  ([x y]
   (let [a (/ x y)]
     (if (ratio? a)
       (double a)
       a)))
  ([x y & xs]
   (reduce div (div x y) xs)))

(defn pow
  ([x y]
   (if (and (integer? x) (integer? y) (>= y 0))
     (let [a (Math/pow x y)]
       (if (<= a Long/MAX_VALUE)
         (long a)
         a))
     (Math/pow x y)))
  ([x y & xs]
    (let [[& xs] (clojure.core/reverse (conj xs y x))]
      (reduce #(pow %2 %1) 1 xs))))

(defn sqr  [x] (pow x 2))
(defn cube [x] (pow x 3))
(defn sqrt [x] (Math/sqrt x))

(defn add+ [x & xs]
  (cond
    (string? x) (apply str x xs)
    (map? x) (apply merge x xs)
    (seqable? x) (apply concat x xs)
    :else (apply + x (map to-num xs))))

(defn div+ [& xs] (double (apply / xs)))

(defn mul+
  ([x y]
   (cond
     (and (string? x) (number? y)) (apply str (repeat y x))
     (and (number? x) (string? y)) (apply str (repeat x y))
     (and (sequential? x) (number? y)) (apply concat (repeat y x))
     (and (number? x) (sequential? y)) (apply concat (repeat x y))
     :else  (* x y)))
  ([x y & xs]
    (reduce mul+ (mul+ x y) xs)))


;;------------------------------------------------------------------------------
;; YAML Anchor and alias functions
;;------------------------------------------------------------------------------
(defn _& [sym val]
  (when (> (count (str val)) _max-alias-size)
    (die "Anchored node &" sym " exceeds max size of " _max-alias-size))
  (swap! common/stream-anchors_ assoc sym val)
  (swap! common/doc-anchors_ assoc sym val)
  val)

(defn _* [sym]
  (or
    (get @common/doc-anchors_ sym)
    (die "Anchor not found: &" sym)))

(defn _** [sym]
  (or
    (get @common/stream-anchors_ sym)
    (die "Anchor not found: &" sym)))


;;------------------------------------------------------------------------------
;; YAMLScript document result stashing functions
;;------------------------------------------------------------------------------
(defn +++* [value]
  (let [index (keyword (str (swap! common/$# inc)))]
    (reset! common/doc-anchors_ {})
    (swap! common/$ assoc index value)
    value))

(defmacro +++ [& forms]
  `(~'+++* (do ~@forms)))

(defn $$ [] (->> @common/$# str keyword (get @common/$)))


;;------------------------------------------------------------------------------
;; Control functions
;;------------------------------------------------------------------------------

(defmacro value [s]
  `(cond
     (string? ~s) (var-get (ns-resolve *ns* (symbol ~s)))
     (symbol? ~s) (var-get (ns-resolve *ns* ~s))
     (var? ~s) (var-get ~s)))

(defmacro call [f & args]
  `(let [f# (if (or
                  (symbol? ~f)
                  (string? ~f)
                  (var? ~f))
              (value ~f)
              ~f)]
     (f# ~@args)))

(intern 'ys.std 'die util/die)

(defmacro each [bindings & body]
  `(do
     (doall (for [~@bindings] (do ~@body)))
     nil))

(defn err [& xs]
  (binding [*out* *err*]
    (apply clojure.core/print xs)
    (flush)))

(defn exit
  ([] (exit 0))
  ([rc] (System/exit rc)))

;; `if` is a special form in Clojure, but we can make resolve with this for use
;; in dot chaining.
(defn if [cond then else] (if cond then else))

(defn sleep [s]
  (Thread/sleep (int (* 1000 s))))

(defn throw [e] (throw e))


;;------------------------------------------------------------------------------
;; String functions
;;------------------------------------------------------------------------------
(intern 'ys.std 'chomp clojure.string/trim-newline)

(defn join
  ([xs] (join "" xs))
  ([sep seq]
   (let [[sep seq] (if (= (type sep) java.lang.String) [sep seq] [seq sep])]
     (str/join sep seq)))
  ([sep x & xs]
   (str/join sep (cons x xs))))

(intern 'ys.std 'lc clojure.string/lower-case)

(intern 'ys.std 'lines clojure.string/split-lines)

(defn pretty [o]
  (str/trim-newline
    (with-out-str
      (pp/pprint o))))

(defn replace
  ([x] (clojure.core/replace x))
  ([x y] (clojure.core/replace x y))
  ([x y z] (clojure.string/replace x y z)))

(intern 'ys.std 'replace1 clojure.string/replace-first)

(defn split
  ([s] (if (empty? s)
         []
         (clojure.string/split s #"")))
  ([s r]
    (let [[s r] (if (= java.util.regex.Pattern (type s)) [r s] [s r])
          r (if (string? r) (re-pattern r) r)]
      (clojure.string/split s r))))

(intern 'ys.std 'trim clojure.string/trim)
(intern 'ys.std 'triml clojure.string/triml)
(intern 'ys.std 'trimr clojure.string/trimr)

(intern 'ys.std 'uc clojure.string/upper-case)
(intern 'ys.std 'uc1 clojure.string/capitalize)

(defn words [s]
  (clojure.string/split s #"\s+"))


;;------------------------------------------------------------------------------
;; Collection functions
;;------------------------------------------------------------------------------
(defn get+ [coll key]
  (cond
    (map? coll) (condp = (type key)
                  String (get coll key)
                  clojure.lang.Keyword (get coll key)
                  clojure.lang.Symbol (or
                                        (get coll (str key))
                                        (get coll (keyword key))
                                        (get coll key))
                  (get coll key))
    (nil? coll) nil
    (seqable? coll) (if (number? key)
                      (nth coll key)
                      (die "Can't (get+ " coll " "
                        (if (nil? key) "nil" key) ")"))
    :else (die "Can't (get+ " coll " "
            (if (nil? key) "nil" key) ")")))

(defn grep [a b]
  (let [[a b] (if (seqable? b) [a b] [b a])
        _ (when-not (seqable? b) (die "No seqable arg passed to grep"))
        t (type a)]
    (cond
      (= t java.util.regex.Pattern) (filter #(re-find a %1) b)
      (fn? a) (filter a b)
      :else (filter #(= a %1) b))))

(defn has? [coll x]
  (boolean
    (if (and (string? coll) (string? x))
      (re-find (re-pattern x) coll)
      (some (set coll) [x]))))

(defn in? [x coll]
  (boolean
    (if (and (string? coll) (string? x))
      (re-find (re-pattern x) coll)
      (some (set coll) [x]))))

(defn omap [& xs]
  (apply flatland.ordered.map/ordered-map xs))

(defn reverse [x]
  (cond
    (string? x) (clojure.string/reverse x)
    (vector? x) (vec (clojure.core/reverse x))
    (seqable? x) (clojure.core/reverse x)
    :else (die "Can't reverse " x)))

(defn rng [a b]
  (let [[x y] (for [n [a b]] (if (char? n) (long n) n))]
    (cond
      (and (number? a) (number? b))
      (if (> y x)
        (range x (inc y))
        (range x (dec y) -1))
      (and (char? a) (char? b))
      (if (> y x)
        (map char (range x (inc y)))
        (map char (range x (dec y) -1)))
      :else
      (die "Can't rng(" (pr-str a) ", " (pr-str b) ")"))))


;;------------------------------------------------------------------------------
;; I/O functions
;;------------------------------------------------------------------------------
(defn out [& xs]
  (apply clojure.core/print xs)
  (flush))

(defn pp [o]
  (pp/pprint o))

(defn print [& xs]
  (apply clojure.core/print xs)
  (flush))

(def _println (resolve 'println))

(defn say [& xs]
  (apply _println xs))

(defn warn [& xs]
  (binding [*out* *err*]
    (apply _println xs)
    (flush)))


;;------------------------------------------------------------------------------
;; File system functions
;;------------------------------------------------------------------------------
(declare fs-f)

(intern 'ys.std 'fs-d fs/directory?)
(intern 'ys.std 'fs-e fs/exists?)
(intern 'ys.std 'fs-f fs/regular-file?)
(intern 'ys.std 'fs-l fs/sym-link?)
(intern 'ys.std 'fs-r fs/readable?)
(defn            fs-s [p] (not= 0 (fs/size p)))
(intern 'ys.std 'fs-w fs/writable?)
(intern 'ys.std 'fs-x fs/executable?)
(defn            fs-z [p] (= 0 (fs/size p)))

(defn fs-abs [p] (str (fs/canonicalize p)))
(intern 'ys.std 'fs-abs? fs/absolute?)

(defn fs-dirname [p]
  (str (fs/parent (fs/canonicalize p))))

(defn fs-filename [p]
  (str (fs/file-name (fs/canonicalize p))))

(defn fs-glob
  ([pat] (fs-glob "." pat))
  ([dir pat] (map str (fs/glob dir pat))))

(defn fs-ls
  ([] (fs-ls ""))
  ([d] (map str (fs/list-dir d))))

(defn fs-mtime [f]
  (fs/file-time->millis
    (fs/last-modified-time f)))

(defn fs-rel
  ([p] (str (fs/relativize (fs/cwd) p)))
  ([d p] (str (fs/relativize d p))))

(intern 'ys.std 'fs-rel? fs/relative?)

(defn fs-which [c]
  (when-let [p (fs/which c)] (str p)))


;;------------------------------------------------------------------------------
;; Regex functions
;;------------------------------------------------------------------------------

;; See: `qr` function above

(defn =-- [str rgx]
  (re-find rgx str))

(defn !-- [str rgx]
  (not (re-find rgx str)))


;;------------------------------------------------------------------------------
;; Java interop functions
;;------------------------------------------------------------------------------
(defn new [class & args]
  (clojure.lang.Reflector/invokeConstructor
    class (into-array Object args)))


;;------------------------------------------------------------------------------
;; IPC functions
;;------------------------------------------------------------------------------
(defn exec [cmd & args]
  (apply process/exec cmd args))

(defn process [cmd & args]
  (apply process/process cmd args))

(defn sh [cmd & args]
  (apply process/sh cmd args))

(defn shell [cmd & args]
  (apply process/shell cmd args))

(defn shout [cmd & args]
  (str/trim-newline
    (:out (apply process/sh cmd args))))


;;------------------------------------------------------------------------------
;; External library functions
;;------------------------------------------------------------------------------
(defn use-pod [pod-name version]
  (ys/load-pod pod-name version))


;;------------------------------------------------------------------------------
;; HTTP functions
;;------------------------------------------------------------------------------

(defn curl [url]
  (let [url (if (re-find #":" url)
              url
              (str "https://" url))
        resp (http/get url)]
    (if-let [body (:body resp)]
      (str body)
      (die resp))))


;;------------------------------------------------------------------------------
(comment
  )
