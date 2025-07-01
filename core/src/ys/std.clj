;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; This is the YS standard library.

(ns ys.std
  (:require
   [babashka.process :as process]
   [clojure.data :as data]
   [clojure.math :as math]
   [clojure.pprint :as pp]
   [clojure.set :as set]
   [clojure.string :as str]
   [flatland.ordered.map]
   [java-time.api :as jtime]
   [yamlscript.common :as common :refer
    [atom? re-find+ regex?]]
   [yamlscript.externals :as ext]
   [yamlscript.global :as global]
   [yamlscript.util :as util]
   [ys.http :as http]
   [ys.ys :as ys])
  (:import java.security.MessageDigest
           java.util.Base64)
  (:refer-clojure :exclude [atom
                            die
                            eval
                            print
                            read
                            replace
                            reverse
                            set]))


;; Guard against billion laughs style attacks
(def _max-alias-size (* 1024 1024))


;;------------------------------------------------------------------------------
;; String functions
;;------------------------------------------------------------------------------
(defn base64-decode [S]
  (String. (.decode (Base64/getDecoder) ^String S)))
(defn base64-encode [S]
  (.encodeToString (Base64/getEncoder) (.getBytes ^String S)))
(defn base64-points [S]
  (.encode (Base64/getEncoder) (.getBytes ^String S)))
(defn base64 [S] (base64-encode S))

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
   (let [S (str S)]
     (if (empty? S)
       []
       (clojure.string/split S #""))))
  ([S R]
   (let [[S R] (if (regex? S) [R S] [S R])
         S (str S)
         R (if (string? R) (re-pattern R) R)]
     (clojure.string/split S R))))

(intern 'ys.std 'starts? clojure.string/starts-with?)

(defn substr
  ([str off] (substr str off (- (count str) off)))
  ([str off len]
   (let [slen (count str)
         off (if (neg? off) (+ slen off) off)
         [len slen] (if (neg? len)
                      (let [len (max 0 (+ slen len))]
                        [len len])
                      [len slen])]
     (condp apply [off slen]
       < (when (>= (+ off len) 0)
           (let [[off len] (if (neg? off)
                             [0 (+ off len)]
                             [off len])
                 len (if (> (+ off len) slen) (- slen off) len)]
             (subs str off (+ off len))))
       = ""
       > nil))))

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
;; Collection functions
;;------------------------------------------------------------------------------
(defn get+ [C K]
  (condf C
    map? (if (symbol? K)
           (or
             (get C K)
             (get C (str K))
             (get C (keyword K)))
           (or
             (get C K)
             (get C (str K))))
    nil? nil
    seqable? (condf K
               number? (nth C K nil)
               nil? nil
               nil)
    nil))

(defn diff [a b] (data/diff a b))

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

(defn has?
  ([C] #(has? C %1))
  ([C x]
   (boolean
     (cond
       (and (string? C) (string? x)) (str/includes? C x)
       (map? C) (get+ C (symbol x))
       :else (some #(= %1 x) C)))))

#_{:clj-kondo/ignore [:syntax]}
(defn in?
  ([C] #(in? C %1))
  ([x C] (has? C x)))

(defn +merge [M]
  (if (:-<< M)
    (let [m (dissoc M :-<<)
          q (get M :-<<)
          v (if (map? q)
              (vector q)
              (if (seqable? q)
                (vec q)
                (util/die "Can't merge " q)))
          M (apply merge-with (fn [x _] x) m v)]
      (+merge M))
    M))

(defn omap [& xs]
  (apply flatland.ordered.map/ordered-map xs))

(intern 'ys.std '% omap)

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
;; Math functions
;;------------------------------------------------------------------------------
(declare to-list to-num to-map to-set to-vec)

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

(defn digits [n]
  (let [n (str n)]
    (when (re-matches #"[0-9]+" n)
      (for [d n]
        (- (byte d) 48)))))

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

(defn- op-error
  ([op x]
   (util/die "Cannot " op "(" (pr-str x) ")"))
  ([op x y]
   (util/die "Cannot " op "(" (pr-str x) " " (pr-str y) ")")))

(defn inc+ [x]
  (condf x
    number? (inc x)
    char? (char (inc (long x)))
    (let [n (to-num x 0)]
      (condf n
        number? (inc n)
        (op-error "inc+" x)))))

(defn dec+ [x]
  (condf x
    number? (dec x)
    char? (char (dec (long x)))
    (let [n (to-num x 0)]
      (condf n
        number? (dec n)
        (op-error "dec+" x)))))

(defn add+
  ([x y]
   (condf (if (nil? x) y x)
     number? (+ (to-num x 0) (to-num y 0))
     string? (str x y)
     map? (merge (to-map x) (to-map y))
     set? (set/union (to-set x) (to-set y))
     vector? (vec (concat (to-vec x)
                (if (sequential? y)
                  (to-list y)
                  (list y))))
     seqable? (concat (to-list x)
                (if (sequential? y)
                  (to-list y)
                  (list y)))
     char? (if (number? y)
             (char (+ (int x) y))
             (str x y))
     fn? (cond
           (fn? y) (comp y x)
           (sequential? y) (apply partial x y)
           :else (partial x y))
     (+ (to-num x 0) (to-num y 0))))

  ([x y & xs]
   (when (not (or
                (apply = (type x) (type y) (map type xs))
                (every? map? (conj xs x y))))
     (util/die "Cannot add+ multiple types when more than 2 arguments"))
   (reduce add+ (add+ x y) xs)))

(defn div+ [& xs] (apply div (map to-num xs)))

(defn mul+
  ([x y]
   (cond
     (and (string? x) (number? y)) (apply str (repeat y x))
     (and (number? x) (string? y)) (apply str (repeat x y))
     (and (vector? x) (number? y)) (vec (apply concat (repeat y x)))
     (and (number? x) (vector? y)) (vec (apply concat (repeat x y)))
     (and (sequential? x) (number? y)) (apply concat (repeat y x))
     (and (number? x) (sequential? y)) (apply concat (repeat x y))
     :else  (* (to-num x 1) (to-num y 1))))

  ([x y & xs]
   (reduce mul+ (mul+ x y) xs)))

(defn sub+
  ([x y]
   (condf x
     string? (str/replace x (str y) "")
     map? (dissoc x y)
     set? (disj x y)
     vector? (vec (remove #(= y %1) x))
     seqable? (remove #(= y %1) x)
     number? (- x (to-num y 0))
     char? (condf y
             number? (char (- (long x) y))
             char? (- (long x) (long y))
             (op-error "sub" x y))
     (+ (to-num x 0) (to-num y 0))))

  ([x y & xs]
   (when (apply not= (type x) (type y) (map type xs))
     (util/die "Cannot sub+ multiple types when more than 2 arguments"))
   (reduce sub+ (sub+ x y) xs)))


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
  (ys/eval (str "!YS-v0\n" S)))

(defn exit
  ([] (exit 0))
  ([I] (System/exit I)))

;; `if` is a special form in Clojure, but we can make resolve with this for use
;; in dot chaining.
(defn if [cond then else] (if cond then else))

(defn sleep [I]
  (Thread/sleep (int (* 1000 I))))

(defn throw [e] (throw e))

(defmacro when+ [test & body]
  (list 'when-let ['_ test] (cons 'do body)))


;;------------------------------------------------------------------------------
;; Function functions
;;------------------------------------------------------------------------------

(defn flip [f]
  (fn
    ([] (f))
    ([a] (f a))
    ([a b] (f b a))
    ([a b c] (f c b a))
    ([a b c d] (f d c b a))
    ([a b c d & rest]
     (->> rest
       (concat [a b c d])
       clojure.core/reverse
       (apply f)))))


;;------------------------------------------------------------------------------
;; Regex functions
;;------------------------------------------------------------------------------

;; See: `qr` function above

(defn =-- [S R]
  (re-find+ R S))

(defn !-- [S R]
  (not (=-- S R)))

(defn =--- [S R]
  (re-matches R (str S)))

(defn !--- [S R]
  (not (=--- S R)))


;;------------------------------------------------------------------------------
;; I/O functions
;;------------------------------------------------------------------------------
(intern 'ys.std 'read clojure.core/slurp)
(intern 'ys.std 'write clojure.core/spit)

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

(defmacro ql [& xs] `(list ~@xs))
(defmacro qm [& xs] `(hash-map ~@xs))
(defmacro qo [& xs] `(omap ~@xs))
(defmacro qv [& xs] `(vector ~@xs))

(defn qr [S] (re-pattern S))

(defmacro qw [& xs]
  (let [xs# (map #(if (nil? %1) "nil" (str %1)) xs)]
    `[~@xs#]))


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
              (util/die "Can't convert multi-char string to char"))
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
    nil? '()
    (util/die "Can't convert " (to-type x) " to list")))

(defn to-map [x]
  (condf x
    map? x
    set? (zipmap (seq x) (repeat nil))
    sequential? (apply hash-map (seq x))
    string? (apply hash-map (seq x))
    nil? {}
    (util/die "Can't convert " (to-type x) " to map")))

(defn to-num
  ([x] (to-num x nil))
  ([x default]
   (condf x
     ratio? (double x)
     number? x
     string? (if (re-find #"\." x)
               (parse-double x)
               (parse-long x))
     nil? (util/die "Can't convert a nil value to a number")
     seqable? (count x)
     char? (int x)
     boolean? (if x 1 0)
     (util/die (str "Can't convert a value of type '"
                 (to-type x) "' to a number")))))

(defn to-omap [x]
  (condf x
    sequential? (apply omap x)
    map? (apply omap (into [] cat x))
    nil? (omap)
    (util/die "Can't convert " (to-type x) " to omap")))

(defn to-set [x]
  (condf x
    map? (clojure.core/set (keys x))
    seqable? (clojure.core/set (seq x))
    nil? (clojure.core/set nil)
    (util/die "Can't convert " (to-type x) " to set")))

(defn set
  ([] #{})
  ([x] (to-set x)))

(defn to-str [x]
  (condf x
    string? x
    nil? "nil"
    (str x)))

(defn to-type [x]
  (condf x
    atom? "atom"
    boolean? "bool"
    char? "char"
    class? "class"
    float? "float"
    fn? "fun"
    int? "int"
    keyword? "keyw"
    list? "list"
    map? "map"
    nil? "nil"
    number? "num"
    regex? "rgx"
    seq? "seq"
    set? "set"
    string? "str"
    symbol? "sym"
    var? "var"
    vector? "vec"
    (util/die "Can't determine type of '" (type x) "' value")))

(defmacro to-vars
  ([m] `(ys/+def-vars *ns* ~m))
  ([m force]
   (when (and force (not= force :force))
     (util/die "to-vars() force argument must be :force"))
  `(ys/+def-vars *ns* ~m ~force)))

(defn to-vec [x]
  (condf x
    map? (reduce-kv (fn [acc k v] (conj acc k v)) [] x)
    sequential? (vec x)
    string? (vec x)
    nil? []
    (util/die "Can't convert " (or (type x) "nil") " to vector")))

(intern 'ys.std 'B to-bool)
(intern 'ys.std 'C to-char)
(intern 'ys.std 'F to-float)
(intern 'ys.std 'I to-int)
(intern 'ys.std 'K to-keyw)
(intern 'ys.std 'L to-list)
(intern 'ys.std 'M to-map)
(intern 'ys.std 'N to-num)
(intern 'ys.std 'O to-omap)
(intern 'ys.std 'S to-str)
(intern 'ys.std 'T to-type)
(intern 'ys.std 'V to-vec)

(intern 'ys.std 'L+ list)
(intern 'ys.std 'M+ hash-map)
(intern 'ys.std 'O+ omap)
(intern 'ys.std 'V+ vector)


;;------------------------------------------------------------------------------
;; Alternate truth functions
;;------------------------------------------------------------------------------

(defn falsey? [x]
  (condf x
    number? (zero? x)
    seqable? (empty? x)
    identity false
    true))

(defmacro F? [x] `(falsey? ~x))

(defn truey? [x]
  (if (falsey? x) nil x))

(defmacro T? [x] `(truey? ~x))

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
;; Date/Time functions
;;------------------------------------------------------------------------------
(defn now
  ([] (jtime/instant))
  ([f] (condp eq f
         :local (jtime/local-date-time)
         :zoned (jtime/zoned-date-time)
         :utc (jtime/instant)
         (util/die "Unknown time format: '" f "'"))))

(defn instant [x] (jtime/instant x))


;;------------------------------------------------------------------------------
;; YAML anchor and alias functions
;;------------------------------------------------------------------------------
(defn _& [sym val]
  (when (> (count (str val)) _max-alias-size)
    (util/die "Anchored node &" sym " exceeds max size of " _max-alias-size))
  (swap! global/stream-anchors_ assoc sym val)
  (swap! global/doc-anchors_ assoc sym val)
  val)

(defn _* [sym]
  (or
    (+merge (get @global/doc-anchors_ sym))
    (util/die "1 Anchor not found: &" sym)))

(defn _** [sym]
  (or
    (+merge (get @global/stream-anchors_ sym))
    (util/die "2 Anchor not found: &" sym)))


;;------------------------------------------------------------------------------
;; Java interop functions
;;------------------------------------------------------------------------------
(defn new [class & xs]
  (clojure.lang.Reflector/invokeConstructor
    class (into-array Object xs)))


;;------------------------------------------------------------------------------
;; Security functions
;;------------------------------------------------------------------------------
(defn md5 [^String string]
  (let [digest (.digest (MessageDigest/getInstance "MD5")
                 (.getBytes string "UTF-8"))]
    (apply str (map (partial format "%02x") digest))))

(defn sha1 [^String string]
  (let [digest (.digest (MessageDigest/getInstance "SHA-1")
                 (.getBytes string "UTF-8"))]
    (apply str (map (partial format "%02x") digest))))

(defn sha256 [^String string]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                 (.getBytes string "UTF-8"))]
    (apply str (map (partial format "%02x") digest))))


;;------------------------------------------------------------------------------
;; IPC functions
;;------------------------------------------------------------------------------
(defn- process-opts [[opts & xs]]
  (let [opts (if (map? opts)
               (let [env (or (:env opts) global/env)
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
  (let [ret (apply sh xs)]
    (when (not= 0 (:exit ret))
      (util/die (:err ret)))
    (str/trim-newline
      (:out ret))))

(defn bash [& xs]
  (let [cmd (str/join " " xs)]
    (sh "bash -c" cmd)))

(defn bash-out [& xs]
  (let [cmd (str/join " " xs)]
    (sh-out "bash -c" cmd)))


;;------------------------------------------------------------------------------
;; External library functions
;;------------------------------------------------------------------------------
(defn use-pod [pod-name version]
  (ys/load-pod pod-name version))


;;------------------------------------------------------------------------------
;; HTTP functions
;;------------------------------------------------------------------------------

(defn get-url [url]
  (ext/convert-url url))

(defn load-url [url]
  (ext/load-url nil url))

(defn curl [url]
  (let [url (get-url url)
        resp (http/get url)]
    (if-let [body (:body resp)]
      (str body)
      (util/die resp))))


;;------------------------------------------------------------------------------
;; YS document result stashing functions
;;------------------------------------------------------------------------------
(defn +++* [value]
  (reset! global/doc-anchors_ {})
  (when ((some-fn map? seqable? number? string?) value)
    (global/set-underscore value)
    (swap! global/stream-values conj value))
  value)

(defmacro +++ [& xs]
  `(do
     (intern '~'main '~'+value (+++* (do ~@xs)))
     (~'in-ns '~'main)
     ~'main/+value))

(defn stream
  ([] @global/stream-values)
  ([values] (reset! global/stream-values values)
            nil))


;;------------------------------------------------------------------------------
;; Atom functions
;;------------------------------------------------------------------------------

(defn atom
  ([] (atom nil))
  ([x] (clojure.core/atom x)))

(defn reset
  ([x y] (clojure.core/reset! x y)))

(defn swap
  ([f & xs] (apply clojure.core/swap! f xs)))


;;------------------------------------------------------------------------------
;; Special functions
;;------------------------------------------------------------------------------

;; Used to run a YS file as a Bash script:
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
(comment
  )
