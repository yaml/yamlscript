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
  (:refer-clojure :exclude [num
                            print
                            reverse
                            replace
                            when]))


(declare die)
(def _max-alias-size (* 1024 1024))


;;------------------------------------------------------------------------------
;; TODO fix that this prints _T as well
;; TDOO Move to yamlscript.debug
;;------------------------------------------------------------------------------
(defmacro _T [xs]
  (let [[fun# & args#] xs
        args# (map pr-str args#)
        #_#_args# (map (fn [x]
                     (let [y (str/replace x #"\(_T " "")
                           n (/ (- (count x) (count y)) 4)]
                       (subs y 0 (- (count y) n)))) args#)
        args# (str/join " -> " args#)]
    `(do
       (clojure.core/print
         ";;" '~fun# "->" ~args# "\n")
       (~@xs))))


;;------------------------------------------------------------------------------
;; Short named functions for very common operations
;;------------------------------------------------------------------------------
(intern 'ys.std 'A clojure.core/partial)
(intern 'ys.std 'I clojure.core/identity)
(intern 'ys.std 'S clojure.core/count)

(defn            V [s] (var-get (resolve (symbol s))))

(defmacro        Q [x] `(quote ~x))
(defmacro        q [x] `(Q ~x))  ;; XXX deprecated?
(defmacro        qw [& xs]
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
;; TODO rename these. bool or to-bool or ->bool
;;------------------------------------------------------------------------------
(defn num [x]
  (condp = (type x)
    java.lang.Boolean (if x 1 0)
    java.lang.Long (long x)
    java.lang.Double (double x)
    java.lang.String (or
                       (if (re-find #"\." x)
                         (parse-double x)
                         (parse-long x))
                       0)
    clojure.lang.PersistentVector (count x)
    clojure.lang.PersistentList (count x)
    clojure.lang.PersistentArrayMap (count x)
    clojure.lang.PersistentHashMap (count x)
    clojure.lang.PersistentHashSet (count x)
    (die (str "Can't convert " (type x) " to number"))))

(defn to-bool [x] (boolean x))
(defn to-float [x] (parse-double x))
(defn to-int [x] (parse-long x))
(defn to-map
  ([] {})
  ([x] (apply hash-map x))
  ([k v & xs] (apply hash-map k v xs)))
(defn to-str [& xs] (apply str xs))
; toList
; toVec


;;------------------------------------------------------------------------------
;; Math functions
;;------------------------------------------------------------------------------
(intern 'ys.std 'sum clojure.core/+)
(intern 'ys.std 'sub clojure.core/-)
(intern 'ys.std 'mul clojure.core/*)
(intern 'ys.std 'div clojure.core//)

(defn pow [x y]
  (if (and (integer? x) (integer? y) (>= y 0))
    (let [a (Math/pow x y)]
      (if (<= a (Long/MAX_VALUE))
        (long a)
        a))
    (Math/pow x y)))

(defn squared [x] (pow x 2))
(defn cubed [x] (pow x 3))
(defn sqrt [x] (Math/sqrt x))

(defn +_ [x & xs]
  (cond
    (string? x) (apply str x xs)
    (vector? x) (apply concat x xs)
    (map? x) (apply merge x xs)
    :else (apply + x (map num xs))))

(defn *_
  ([x y]
   (cond
     (and (string? x) (number? y)) (apply str (repeat y x))
     (and (number? x) (string? y)) (apply str (repeat x y))
     (and (sequential? x) (number? y)) (apply concat (repeat y x))
     (and (number? x) (sequential? y)) (apply concat (repeat x y))
     :else  (* x y)))
  ([x y & xs]
    (reduce *_ (*_ x y) xs)))


;;------------------------------------------------------------------------------
;; YAML Anchor and alias functions
;;------------------------------------------------------------------------------
(defn _& [sym val]
  (clojure.core/when (> (count (str val)) _max-alias-size)
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
;; Dot chaining support
;; TODO make this public as `chain`
;; XXX Dot chaining compilation is going to change soon
;;------------------------------------------------------------------------------
(defn _dot [ctx key]
  (cond
    (symbol? key) (or
                    (get ctx (keyword key))
                    (get ctx (str key))
                    (get ctx key))
    (string? key) (get ctx key)
    (int? key) (let [n (if (< key 0)
                         (+ key (count ctx))
                         key)]
                 (if (map? ctx)
                   (or (get ctx (keyword (str n)))
                     (get ctx (str n))
                     (get ctx n))
                   (nth ctx n)))
    (keyword? key) (get ctx key)
    (list? key) (let [[fnc & args] key
                      nargs (map #(if (= '_ %1) ctx %1) args)
                      args (vec
                             (if (or (nil? args) (= nargs args))
                               (cons ctx args)
                               nargs))
                      value (apply fnc args)
                      value (if (instance? clojure.lang.LazySeq value)
                              (vec value)
                              value)]
                  value)
    :else (die "Invalid key: " key)))

(defn _-> [x & xs]
  (reduce _dot x xs))


;;------------------------------------------------------------------------------
;; Control functions
;;------------------------------------------------------------------------------

(defn call [f & args]
  (let [f (cond
            (string? f) (resolve (symbol f))
            (symbol? f) (resolve f)
            :else f)]
    (apply f args)))

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

(intern 'ys.std 'lines clojure.string/split-lines)

(intern 'ys.std 'lc clojure.string/lower-case)

(defn pretty [o]
  (str/trim-newline
    (with-out-str
      (pp/pprint o))))

(defn replace
  ([x] (clojure.core/replace x))
  ([x y] (clojure.core/replace x y))
  ([x y z] (clojure.string/replace x y z)))

(intern 'ys.std 'replace1 clojure.string/replace-first)

(intern 'ys.std 'trim clojure.string/trim)
(intern 'ys.std 'triml clojure.string/triml)
(intern 'ys.std 'trimr clojure.string/trimr)

(intern 'ys.std 'uc clojure.string/upper-case)
(intern 'ys.std 'uc1 clojure.string/capitalize)

(defn split
  ([s] (if (empty? s)
         []
         (clojure.string/split s #"")))
  ([s r]
    (let [[s r] (if (= java.util.regex.Pattern (type s)) [r s] [s r])
          r (if (string? r) (re-pattern r) r)]
      (clojure.string/split s r))))

(defn words [s]
  (clojure.string/split s #"\s+"))


;;------------------------------------------------------------------------------
;; Collection functions
;;------------------------------------------------------------------------------
(defn has [coll x]
  (boolean
    (if (and (string? coll) (string? x))
      (re-find (re-pattern x) coll)
      (some (set coll) [x]))))

(defn grep [a b]
  (let [[a b] (if (seqable? b) [a b] [b a])
        _ (when-not (seqable? b) (die "No seqable arg passed to grep"))
        t (type a)]
    (cond
      (= t java.util.regex.Pattern) (filter #(re-find a %1) b)
      (fn? a) (filter a b)
      :else (filter #(= a %1) b))))

(defn in [x coll]
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

(defn rng [x y]
  (if (> y x)
    (range x (inc y))
    (range x (dec y) -1)))


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
(defn abspath [& args]
  (apply util/abspath args))

(defn cwd [& args]
  (str (apply fs/cwd args)))

(defn dirname [& args]
  (apply util/dirname args))

(intern 'ys.std 'fs-d fs/directory?)
(intern 'ys.std 'fs-e fs/exists?)
(intern 'ys.std 'fs-f fs/regular-file?)
(intern 'ys.std 'fs-l fs/sym-link?)
(intern 'ys.std 'fs-r fs/readable?)
(defn fs-s [p] (not= 0 (fs/size p)))
(intern 'ys.std 'fs-w fs/writable?)
(intern 'ys.std 'fs-x fs/executable?)
(defn fs-z [p] (= 0 (fs/size p)))

(defn fs-cwd [] (str (fs/cwd)))
(defn fs-ls
  ([] (fs-ls "."))
  ([d] (map str (fs/list-dir d))))
(defn fs-mtime [f]
  (fs/file-time->millis
    (fs/last-modified-time f)))
(defn fs-glob
  ([pat] (fs-glob "." pat))
  ([dir pat] (map str (fs/glob dir pat))))
(intern 'ys.std 'fs-abs fs/absolute?)
(defn fs-rel
  ([p] (str (fs/relativize (fs/cwd) p)))
  ([d p] (str (fs/relativize d p))))
(defn fs-which [c]
  (when-let [p (fs/which c)] (str p)))


;;------------------------------------------------------------------------------
;; Regex functions
;;------------------------------------------------------------------------------
(defn rx [s]
  (re-pattern s))

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
(defn exec [cmd & xs]
  (apply process/exec cmd xs))

(defn process [cmd & xs]
  (apply process/process cmd xs))

(defn sh [cmd & xs]
  (apply process/sh cmd xs))

(defn shell [cmd & xs]
  (apply process/shell cmd xs))


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




(comment
  (call "inc" 41)

  (ns main (:require [ys.std :refer :all]))

  (binding [*ns* (the-ns 'main)]
    (eval (clojure.core/read-string "(resolve 'call)")))
  (binding [*ns* (the-ns 'main)]
    (eval (clojure.core/read-string "(call \"inc\" 41)")))

  (call "inc" 41)
  )
