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
   [clojure.set :as set]
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
(defmacro _X [x]
  (let [[fun# & args#] x
        args# (map pr-str args#)
        args# (str/join " -> " args#)]
    `(do
       (clojure.core/print
         ";;" '~fun# "->" ~args# "\n")
       (~@x))))


;;------------------------------------------------------------------------------
;; Special functions
;;------------------------------------------------------------------------------

;; Used to run a YAMLScript file as a Bash script:
(defmacro source [& xs])

;; def destructuring
(declare +def-defn)

(defn- destructure-vector [V idx]
  (map-indexed
   (fn [i name]
     (+def-defn name `(get ~idx ~i)))
   V))

(defn- destructure-map [M idx]
  (map
   (fn [[k v]]
     (+def-defn k `(get ~idx ~v)))
   M))

(defn- destructure-idx [x idx]
  (let [root (gensym)]
    `(let [~root ~idx]
       ~@(cond
           (vector? x) (destructure-vector x root)
           (list? x) (destructure-vector x root)
           (map? x) (destructure-map x root)
           :else []))))

(defn- +def-defn [x y]
  (if (symbol? x)
    `(def ~x ~y)
    (destructure-idx x y)))

(defmacro +def [x y]
  (+def-defn x y))


;;------------------------------------------------------------------------------
;; Shorter named alias functions
;;------------------------------------------------------------------------------

(intern 'ys.std 'a clojure.core/identity)

(intern 'ys.std 'len clojure.core/count)


;;------------------------------------------------------------------------------
;; Quoting functions
;;------------------------------------------------------------------------------

(defmacro q [x] `(quote ~x))
(defn qr [S] (re-pattern S))
(defmacro qw [& xs]
  `(vec (map (fn [word#]
               (cond
                 (nil? word#) "nil"
                 (re-matches #"^[-\w]+$" (str word#)) (str word#)
                 :else (clojure.core/die
                         (str "Invalid qw word: '" word# "'"))))
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
  ([x & xs]
      `(if (truey? ~x) ~x (or? ~@xs))))

(defmacro ||| [x & xs] `(or? ~x ~@xs))

(defmacro and?
  ([] true)
  ([x] (if (truey? x) x nil))
  ([x & xs]
      `(if (truey? ~x) (and? ~@xs) nil)))

(defmacro &&& [x & xs] `(and? ~x ~@xs))


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
  ([x y & xs] (apply hash-map x y xs)))

(defn to-num [x]
  (cond
    (ratio? x) (double x)
    (number? x) x
    (string? x) (or
                  (if (re-find #"\." x)
                    (parse-double x)
                    (parse-long x))
                  0)
    (seqable? x) (count x)
    (char? x) (int x)
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
(intern 'ys.std 'add clojure.core/+)
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

(defn sum
  ([] 0)
  ([xs] (reduce + 0 (filter identity xs))))

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

(defn sqr  [N] (pow N 2))
(defn cube [N] (pow N 3))
(defn sqrt [N] (Math/sqrt N))

(defn- op-error [op x y]
  (die "Cannot " op "(" (pr-str x) " " (pr-str y) ")"))

(defn inc+ [x]
  (cond
    (number? x) (inc x)
    (char? x) (char (inc (long x)))
    :else (die "Cannot inc+(" (pr-str x) ")")))

(defn dec+ [x]
  (cond
    (number? x) (dec x)
    (char? x) (char (dec (long x)))
    :else (die "Cannot dec+(" (pr-str x) ")")))

(defn add+
  ([x y]
   (cond
     (some nil? [x y]) (die "Cannot add with a nil value")
     (number? x) (+ x (to-num y))
     (string? x) (str x y)
     (map? x) (merge x y)      ;; error if y is not a map
     (set? x) (set/union x y)  ;; error if y is not a set
     (seqable? x) (concat x y) ;; error if y is not seqable
     (char? x) (if (number? y)
                 (char (+ (int x) y))
                 (str x y))
     :else (+ (to-num x) (to-num y))))
  ([x y & xs]
   (when (apply not= (type x) (type y) (map type xs))
     (die "Cannot add+ multiple types when more than 2 arguments"))
   (reduce add+ (add+ x y) xs)))

(defn sub+
  ([x y]
   (cond
     (some nil? [x y]) (die "Cannot subtract with a nil value")
     (string? x) (str/replace x (str y) "")
     (map? x) (dissoc x y)              ;; error if y is not a map
     (set? x) (disj x y)                ;; error if y is not a set
     (seqable? x) (remove #(= y %1) x)  ;; error if y is not seqable
     (number? x) (- x (to-num y))
     (char? x) (cond (number? y) (char (- (long x) y))
                     (char? y) (- (long x) (long y))
                     :else (op-error "sub" x y))
     :else (+ (to-num x) (to-num y))))
  ([x y & xs]
   (when (apply not= (type x) (type y) (map type xs))
     (die "Cannot sub+ multiple types when more than 2 arguments"))
   (reduce sub+ (sub+ x y) xs)))

(comment
  (sub+ \C \1)
  (sub+ [1 nil "two" nil nil] nil)
  (add+ \A \B)
  )

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
(defn +++* [val]
  (let [idx (keyword (str (swap! common/$# inc)))]
    (reset! common/doc-anchors_ {})
    (swap! common/$ assoc idx val)
    val))

(defmacro +++ [& xs]
  `(~'+++* (do ~@xs)))

(defn $$ [] (->> @common/$# str keyword (get @common/$)))


;;------------------------------------------------------------------------------
;; Control functions
;;------------------------------------------------------------------------------

(defmacro value [x]
  `(let [var# (cond
                (string? ~x) (ns-resolve *ns* (symbol ~x))
                (symbol? ~x) (ns-resolve *ns* ~x)
                (var? ~x) ~x
                :else nil)]
     (when var# (var-get var#))))

(defmacro call [x & xs]
  `(let [f# (or (value ~x) ~x)]
     (when-not (fn? f#) (die "Can't call(" (pr-str f#) ")"))
     (f# ~@xs)))

(intern 'ys.std 'die util/die)

(defmacro each [bindings & body]
  `(doall (for ~bindings (do ~@body))))

(defn err [& xs]
  (binding [*out* *err*]
    (apply clojure.core/print xs)
    (flush)))

(defn exit
  ([] (exit 0))
  ([I] (System/exit I)))

;; `if` is a special form in Clojure, but we can make resolve with this for use
;; in dot chaining.
(defn if [cond then else] (if cond then else))

(defn sleep [I]
  (Thread/sleep (int (* 1000 I))))

(defn throw [e] (throw e))


;;------------------------------------------------------------------------------
;; String functions
;;------------------------------------------------------------------------------
(intern 'ys.std 'chomp clojure.string/trim-newline)
(intern 'ys.std 'index clojure.string/index-of)

(defn join
  ([Ss] (join "" Ss))
  ([S Ss]
   (if (= (type S) java.lang.String)
     (str/join S Ss)
     (str/join Ss S)))
  ([S x & xs]
   (str/join S (cons x xs))))

(intern 'ys.std 'lc clojure.string/lower-case)

(defn lines [S]
  (if (empty? S)
    []
    (let [S (if (= (last S) \newline)
              (subs S 0 (dec (count S)))
              S)]
      (str/split S #"\n" -1))))

(defn text [Ss]
  (if (empty? Ss)
    ""
    (str/join "\n"
      (concat Ss (list "")))))

(defn pretty [x]
  (str/trim-newline
    (with-out-str
      (pp/pprint x))))

(defn replace
  ([x] (clojure.core/replace x))
  ([x y] (clojure.core/replace x y))
  ([x y z] (clojure.string/replace x y z)))

(intern 'ys.std 'replace1 clojure.string/replace-first)

(defn split
  ([S] (if (empty? S)
         []
         (clojure.string/split S #"")))
  ([S R]
    (let [[S R] (if (= java.util.regex.Pattern (type S)) [R S] [S R])
          R (if (string? R) (re-pattern R) R)]
      (clojure.string/split S R))))

(intern 'ys.std 'trim clojure.string/trim)
(intern 'ys.std 'triml clojure.string/triml)
(intern 'ys.std 'trimr clojure.string/trimr)

(intern 'ys.std 'uc clojure.string/upper-case)
(intern 'ys.std 'uc1 clojure.string/capitalize)

(defn words [S]
  (clojure.string/split S #"\s+"))


;;------------------------------------------------------------------------------
;; Regex functions
;;------------------------------------------------------------------------------

;; See: `qr` function above

(defn =-- [S R]
  (re-find R S))

(defn !-- [S R]
  (not (re-find R S)))


;;------------------------------------------------------------------------------
;; Collection functions
;;------------------------------------------------------------------------------
(defn get+ [C K]
  (cond
    (map? C) (condp = (type K)
               String (get C K)
               clojure.lang.Keyword (get C K)
               clojure.lang.Symbol (or
                                     (get C (str K))
                                     (get C (keyword K))
                                     (get C K))
               (get C K))
    (nil? C) nil
    (string? C) nil
    (seqable? C) (cond
                   (number? K) (nth C K)
                   (nil? K) nil
                   :else (die "Can't (get+ " C " "
                           (if (nil? K) "nil" K) ")"))
    :else nil))

(defn grep [P C]
  (let [[P C] (if (seqable? C) [P C] [C P])
        _ (when-not (seqable? C) (die "No seqable arg passed to grep"))
        t (type P)]
    (cond
      (= t java.util.regex.Pattern) (filter #(re-find P %1) C)
      (fn? P) (filter P C)
      :else (filter #(= P %1) C))))

(defn has? [C x]
  (boolean
    (if (and (string? C) (string? x))
      (re-find (re-pattern x) C)
      (some (set C) [x]))))

(defn in? [x C]
  (boolean
    (if (and (string? C) (string? x))
      (re-find (re-pattern x) C)
      (some (set C) [x]))))

(defn omap [& xs]
  (apply flatland.ordered.map/ordered-map xs))

(defn reverse [x]
  (cond
    (string? x) (clojure.string/reverse x)
    (vector? x) (vec (clojure.core/reverse x))
    (seqable? x) (clojure.core/reverse x)
    :else (die "Can't reverse " x)))

(defn rng [x y]
  (let [[a b] (for [n [x y]] (if (char? n) (long n) n))]
    (cond
      (and (number? x) (number? y))
      (if (> b a)
        (range a (inc b))
        (range a (dec b) -1))
      (and (char? x) (char? y))
      (if (> b a)
        (map char (range a (inc b)))
        (map char (range a (dec b) -1)))
      :else
      (die "Can't rng(" (pr-str x) ", " (pr-str y) ")"))))


;;------------------------------------------------------------------------------
;; I/O functions
;;------------------------------------------------------------------------------
(defn out [& xs]
  (apply clojure.core/print xs)
  (flush))

(defn pp [x]
  (pp/pprint x))

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
(defn            fs-s [path] (not= 0 (fs/size path)))
(intern 'ys.std 'fs-w fs/writable?)
(intern 'ys.std 'fs-x fs/executable?)
(defn            fs-z [path] (= 0 (fs/size path)))

(defn fs-abs
  ([path] (str (fs/canonicalize path)))
  ([path file] (str (fs/canonicalize (fs/file path file)))))

(intern 'ys.std 'fs-abs? fs/absolute?)

(defn fs-dirname [path]
  (str (fs/parent (fs/canonicalize path))))

(defn fs-filename [path]
  (str (fs/file-name (fs/canonicalize path))))

(defn fs-glob
  ([path] (fs-glob "." path))
  ([dir path] (map str (fs/glob dir path))))

(defn fs-ls
  ([] (fs-ls ""))
  ([dir] (map str (fs/list-dir dir))))

(defn fs-mtime [file]
  (fs/file-time->millis
    (fs/last-modified-time file)))

(defn fs-rel
  ([path] (str (fs/relativize (fs/cwd) path)))
  ([dir path] (str (fs/relativize dir path))))

(intern 'ys.std 'fs-rel? fs/relative?)

(defn fs-which [name]
  (when-let [path (fs/which name)] (str path)))


;;------------------------------------------------------------------------------
;; Java interop functions
;;------------------------------------------------------------------------------
(defn new [class & xs]
  (clojure.lang.Reflector/invokeConstructor
    class (into-array Object xs)))


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

(defn shout [cmd & xs]
  (str/trim-newline
    (:out (apply process/sh cmd xs))))


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
