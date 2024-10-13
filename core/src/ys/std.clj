;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; This is the YAMLScript standard library.

(ns ys.std
  (:require
   [babashka.fs :as fs]
   [babashka.http-client :as http]
   [babashka.process :as process]
   [clojure.math :as math]
   [clojure.pprint :as pp]
   [clojure.set :as set]
   [clojure.string :as str]
   [flatland.ordered.map]
   [yamlscript.common :as common :refer [atom? regex?]]
   [yamlscript.global :as global]
   [yamlscript.util :as util]
   [ys.ys :as ys])
  (:refer-clojure :exclude [die
                            eval
                            print
                            reverse
                            replace]))


;; Guard against billion laughs style attacks
(def _max-alias-size (* 1024 1024))


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
       ~@(condf x
           vector? (destructure-vector x root)
           list? (destructure-vector x root)
           map? (destructure-map x root)
           []))))

(defn- +def-defn [x y]
  (if (symbol? x)
    `(def ~x ~y)
    (destructure-idx x y)))

(defmacro +def [x y]
  (+def-defn x y))

(defn env-update
  ([m]
   (let
    [m (reduce-kv
         (fn [env k v]
           (when-not (string? k)
             (util/die "env-update() keys must be strings"))
           (let [v (condf v
                     string? v
                     number? (str v)
                     boolean? (str v)
                     nil? nil
                     (util/die "env-update() values must be scalars"))]
             (assoc env k v))) {} m)]
     (global/update-env m)
     (global/update-environ m)))
  ([k v & xs] (env-update (apply hash-map k v xs))))


;;------------------------------------------------------------------------------
;; Shorter named alias functions
;;------------------------------------------------------------------------------

(intern 'ys.std 'a clojure.core/identity)

(intern 'ys.std 'len clojure.core/count)


;;------------------------------------------------------------------------------
;; Quoting functions
;;------------------------------------------------------------------------------

(defmacro q
  ([x] `(quote ~x))
  ([x & xs] `(quote [~x ~@xs])))

(defn qr [S] (re-pattern S))

(defmacro qw [& xs]
  (let [xs# (map #(if (nil? %1) "nil" (str %1)) xs)]
    `[~@xs#]))


;;------------------------------------------------------------------------------
;; Alternate truth functions
;;------------------------------------------------------------------------------

(defn falsey? [x]
  (condf x
    number? (zero? x)
    seqable? (empty? x)
    identity false
    true))

(defn truey? [x]
  (if (falsey? x) nil x))

(defmacro or?
  ([] nil)
  ([x]
   `(truey? ~x))
  ([x & xs]
   `(or (truey? ~x) (or? ~@xs))))

(defmacro ||| [x & xs] `(or? ~x ~@xs))

(defmacro and?
  ([] true)
  ([x]
   `(truey? ~x))
  ([x & xs]
   `(and (truey? ~x) (and? ~@xs))))

(defmacro &&& [x & xs] `(and? ~x ~@xs))


;;------------------------------------------------------------------------------
;; Named function aliases for infix operators
;;------------------------------------------------------------------------------
(defn eq
  ([x] #(= %1 x))
  ([x y] (= x y))
  ([x y & xs] (apply = x y xs)))

(defn ne
  ([x] #(not= %1 x))
  ([x y] (not= x y))
  ([x y & xs] (apply not= x y xs)))

(defn gt
  ([x] #(> %1 x))
  ([x y] (> x y))
  ([x y & xs] (apply > x y xs)))

(defn ge
  ([x] #(>= %1 x))
  ([x y] (>= x y))
  ([x y & xs] (apply >= x y xs)))

(defn lt
  ([x] #(< %1 x))
  ([x y] (< x y))
  ([x y & xs] (apply < x y xs)))

(defn le
  ([x] #(<= %1 x))
  ([x y] (<= x y))
  ([x y & xs] (apply <= x y xs)))


;;------------------------------------------------------------------------------
;; Common type casting functions
;;------------------------------------------------------------------------------
(declare to-num to-type)

(defn to-bool [x] (boolean x))

(defn to-char [x]
  (condf x
    char? x
    string? (if (= 1 (count x))
              (first x)
              (util/die "Can't convert string to char"))
    number? (char x)
    (util/die "Can't convert " (to-type x) " to char")))

(defn to-float [x] (double (to-num x)))

(defn to-int [x] (long (to-num x)))

(defn to-keyw [x] (keyword x))

(defn to-list [x]
  (condf x
    map? (reduce-kv (fn [acc k v] (conj acc v k)) '() x)
    sequential? (if (empty? x) '() (seq x))
    string? (if (empty? x) '() (seq x))
    (util/die "Can't convert " (to-type x) " to list")))

(defn to-map [x]
  (condf x
    map? x
    set? (zipmap (seq x) (repeat nil))
    sequential? (apply hash-map (seq x))
    string? (apply hash-map (seq x))
    (util/die "Can't convert " (to-type x) " to map")))

(defn to-num [x]
  (condf x
    ratio? (double x)
    number? x
    string? (if (re-find #"\." x)
              (parse-double x)
              (parse-long x))
    nil? (util/die "Can't convert nil to number")
    seqable? (count x)
    char? (int x)
    boolean? (if x 1 0)
    (util/die (str "Can't convert " (to-type x) " to number"))))

(defn to-set [x]
  (condf x
    map? (set (keys x))
    seqable? (set (seq x))
    (util/die "Can't convert " (to-type x) " to set")))

(defn to-str [x]
  (condf x
    string? x
    nil? "nil"
    (str x)))

(defn to-type [x]
  (condf x
    nil? "nil"
    string? "str"
    int? "int"
    float? "float"
    number? "num"
    boolean? "bool"
    char? "char"
    keyword? "keyw"
    regex? "rgx"
    map? "map"
    set? "set"
    vector? "vec"
    list? "list"
    seq? "seq"
    fn? "fun"
    atom? "atom"
    class? "class"
    var? "var"
    symbol? "sym"
    (util/die "Can't determine type of '" (type x) "' value")))

(defn to-vec [x]
  (condf x
    map? (reduce-kv (fn [acc k v] (conj acc k v)) [] x)
    sequential? (vec x)
    string? (vec x)
    (util/die "Can't convert " (or (type x) "nil") " to vector")))

(intern 'ys.std 'A atom)
(intern 'ys.std 'B to-bool)
(intern 'ys.std 'C to-char)
(intern 'ys.std 'D deref)
(intern 'ys.std 'F to-float)
;; (intern 'ys.std 'G to-set)  ;; G for group; XXX not convinced this is useful
(intern 'ys.std 'I to-int)
(intern 'ys.std 'K to-keyw)
(intern 'ys.std 'L to-list)
(intern 'ys.std 'M to-map)
(intern 'ys.std 'N to-num)
(intern 'ys.std 'S to-str)
(intern 'ys.std 'T to-type)
(intern 'ys.std 'V to-vec)


;;------------------------------------------------------------------------------
;; Math functions
;;------------------------------------------------------------------------------

(defn add
  ([] 0)
  ([x] #(+ %1 x))
  ([x y] (+ x y))
  ([x y & xs] (apply + x y xs)))

(defn sub
  ([x] #(- %1 x))
  ([x y] (- x y))
  ([x y & xs] (apply - x y xs)))

(defn mul
  ([] 1)
  ([x] #(* %1 x))
  ([x y] (* x y))
  ([x y & xs] (apply * x y xs)))

(defn div
  ([x] #(div %1 x))
  ([x y]
   (let [a (/ x y)]
     (if (ratio? a)
       (double a)
       a)))
  ([x y & xs]
   (reduce div (div x y) xs)))

(intern 'ys.std 'floor math/floor)

(defn pow
  ([x] #(pow %1 x))
  ([x y]
   (if (and (integer? x) (integer? y) (>= y 0))
     (let [a (math/pow x y)]
       (if (<= a Long/MAX_VALUE)
         (long a)
         a))
     (math/pow x y)))
  ([x y & xs]
    (let [[& xs] (clojure.core/reverse (conj xs y x))]
      (reduce #(pow %2 %1) 1 xs))))

(intern 'ys.std 'round math/round)

(defn sum [xs]
  (reduce + 0 (filter identity xs)))

(defn sqr  [N] (pow N 2))
(defn cube [N] (pow N 3))
(intern 'ys.std 'sqrt math/sqrt)

(defn digits [n]
  (let [n (str n)]
    (when (re-matches #"[0-9]+" n)
      (for [d n]
        (- (byte d) 48)))))

(defn- op-error
  ([op x]
     (util/die "Cannot " op "(" (pr-str x) ")"))
  ([op x y]
     (util/die "Cannot " op "(" (pr-str x) " " (pr-str y) ")")))

(defn inc+ [x]
  (condf x
    number? (inc x)
    char? (char (inc (long x)))
    (let [n (to-num x)]
      (condf n
        number? (inc n)
        (op-error "inc+" x)))))

(defn dec+ [x]
  (condf x
    number? (dec x)
    char? (char (dec (long x)))
    (let [n (to-num x)]
      (condf n
        number? (dec n)
        (op-error "dec+" x)))))

(defn add+
  ([x y]
   (when (some nil? [x y])
     (util/die "Cannot add with a nil value"))
   (condf x
     number? (+ x (to-num y))
     string? (str x y)
     map? (merge x (to-map y))
     set? (set/union x (to-set y))
     seqable? (concat x (to-vec y))
     char? (if (number? y)
             (char (+ (int x) y))
             (str x y))
     (+ (to-num x) (to-num y))))
  ([x y & xs]
   (when (not (or
                (apply = (type x) (type y) (map type xs))
                (every? map? (conj xs x y))))
     (util/die "Cannot add+ multiple types when more than 2 arguments"))
   (reduce add+ (add+ x y) xs)))

(defn div+ [& xs] (apply div xs))

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

(defn sub+
  ([x y]
   (when (some nil? [x y])
     (util/die "Cannot subtract with a nil value"))
   (condf x
     string? (str/replace x (str y) "")
     map? (dissoc x y)
     set? (disj x y)
     seqable? (remove #(= y %1) x)
     number? (- x (to-num y))
     char? (condf y
             number? (char (- (long x) y))
             char? (- (long x) (long y))
             (op-error "sub" x y))
     (+ (to-num x) (to-num y))))
  ([x y & xs]
   (when (apply not= (type x) (type y) (map type xs))
     (util/die "Cannot sub+ multiple types when more than 2 arguments"))
   (reduce sub+ (sub+ x y) xs)))


;;------------------------------------------------------------------------------
;; YAML Anchor and alias functions
;;------------------------------------------------------------------------------
(defn _& [sym val]
  (when (> (count (str val)) _max-alias-size)
    (util/die "Anchored node &" sym " exceeds max size of " _max-alias-size))
  (swap! global/stream-anchors_ assoc sym val)
  (swap! global/doc-anchors_ assoc sym val)
  val)

(defn _* [sym]
  (or
    (get @global/doc-anchors_ sym)
    (util/die "Anchor not found: &" sym)))

(defn _** [sym]
  (or
    (get @global/stream-anchors_ sym)
    (util/die "Anchor not found: &" sym)))


;;------------------------------------------------------------------------------
;; YAMLScript document result stashing functions
;;------------------------------------------------------------------------------
(defn +++* [val]
  (let [idx (keyword (str (swap! global/$# inc)))]
    (reset! global/doc-anchors_ {})
    (swap! global/$ assoc idx val)
    val))

(defmacro +++ [& xs]
  `(~'+++* (do ~@xs)))

(defn $$ [] (->> @global/$# str keyword (get @global/$)))


;;------------------------------------------------------------------------------
;; Control functions
;;------------------------------------------------------------------------------

(defmacro value [x]
  `(let [var# (condp #(%1 %2) ~x
                string? (ns-resolve *ns* (symbol ~x))
                symbol? (ns-resolve *ns* ~x)
                var? ~x
                nil)]
     (when var# (var-get var#))))

(defmacro call [x & xs]
  `(let [f# (or (value ~x) ~x)]
     (when-not (fn? f#) (util/die "Can't call(" (pr-str f#) ")"))
     (f# ~@xs)))

(intern 'ys.std 'die yamlscript.util/die)

(defmacro each [bindings & body]
  `(doall (for ~bindings (do ~@body))))

(defn err [& xs]
  (binding [*out* *err*]
    (apply clojure.core/print xs)
    (flush)))

(defn eval [S]
  (->> S
    (str "!yamlscript/v0\n")
    ys/eval))

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
(intern 'ys.std 'blank? clojure.string/blank?)
(intern 'ys.std 'chomp clojure.string/trim-newline)
(intern 'ys.std 'chop common/chop)
(intern 'ys.std 'ends? clojure.string/ends-with?)
(intern 'ys.std 'escape clojure.string/escape)
(intern 'ys.std 'index clojure.string/index-of)

(defn index [C x]
  (condf C
    string? (clojure.string/index-of C x)
    sequential? (let [i (.indexOf ^java.util.List C x)]
                     (if (>= i 0) i nil))
    (util/die "Can't index a " (type C))))

(defn join
  ([Ss] (join "" Ss))
  ([S Ss]
   (if (string? S)
     (str/join S Ss)
     (str/join Ss S)))
  ([S x & xs]
   (str/join S (cons x xs))))

(defn joins [Ss] (join " " Ss))

(intern 'ys.std 'lc clojure.string/lower-case)

(defn lines [S]
  (if (empty? S)
    []
    (let [S (if (= (last S) \newline)
              (subs S 0 (dec (count S)))
              S)]
      (str/split S #"\n" -1))))

(defn pretty [x]
  (str/trim-newline
    (with-out-str
      (pp/pprint x))))

(defn replace
  ([x] (clojure.core/replace x))
  ([x y] (if (or (regex? y) (string? y))
           (clojure.string/replace x y "")
           (clojure.core/replace x y)))
  ([x y z] (clojure.string/replace x y z)))

(intern 'ys.std 'replace1 clojure.string/replace-first)
(intern 'ys.std 'rindex clojure.string/last-index-of)

(defn split
  ([S]
   (if (empty? S)
     []
     (clojure.string/split S #"")))
  ([S R]
   (let [[S R] (if (regex? S) [R S] [S R])
         R (if (string? R) (re-pattern R) R)]
     (clojure.string/split S R))))

(intern 'ys.std 'starts? clojure.string/starts-with?)

(defn text [Ss]
  (if (empty? Ss)
    ""
    (str/join "\n"
      (concat Ss (list "")))))

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
  (condf C
    map? (condp = (type K)
           String (get C K)
           clojure.lang.Keyword (get C K)
           clojure.lang.Symbol (or
                                 (get C K)
                                 (get C (str K))
                                 (get C (keyword K)))
           (get C K))
    nil? nil
    seqable? (condf K
               number? (nth C K nil)
               nil? nil
               nil)
    nil))

(defn flat [C]
  (mapcat
    (fn [x] (if (seqable? x) x [x]))
    C))

(defn grep [P C]
  (let [[P C] (if (seqable? C) [P C] [C P])
        _ (when-not (seqable? C)
            (util/die "No seqable arg passed to grep"))]
    (condf P
      regex? (filter #(re-find P %1) C)
      fn? (filter P C)
      (filter #(= P %1) C))))

(defn has? [C x]
  (boolean
    (if (and (string? C) (string? x))
      (str/includes? C x)
      (some (set C) [x]))))

(defn in? [x C] (has? C x))

(defn omap [& xs]
  (apply flatland.ordered.map/ordered-map xs))

(defn reverse [x]
  (condf x
    string? (clojure.string/reverse x)
    vector? (vec (clojure.core/reverse x))
    seqable? (clojure.core/reverse x)
    (util/die "Can't reverse " x)))

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
      (util/die "Can't rng(" (pr-str x) ", " (pr-str y) ")"))))

(defn slice [C & ks]
  (let [ks (flatten ks)]
    (vec (map (fn [k] (get+ C k)) ks))))


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
(defn- process-opts [[opts & xs]]
  (let [opts (if (map? opts)
               (let [env (or (:env opts) global/env)
                     #_#_ dir (or (:dir opts) CWD)
                     opts (assoc opts :env env)]
                 [opts])
               [{:env global/env} opts])]
    (vec (concat opts xs))))

(defn exec [& xs]
  (apply process/exec (process-opts xs)))

(defn process [& xs]
  (apply process/process (process-opts xs)))

(defn sh [& xs]
  (apply process/sh (process-opts xs)))

(defn shell [& xs]
  (apply process/shell (process-opts xs)))

(defn sh-out [& xs]
  (str/trim-newline
    (:out (apply sh xs))))


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
      (util/die resp))))


;;------------------------------------------------------------------------------
(comment
  )
