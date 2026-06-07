;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.util)

(defn die
  "Throw a YAMLScript exception with a normalized trailing newline."
  ([] (throw (Exception. "Died")))
  ([msg] (throw (Exception. (str msg "\n"))))
  ([x & xs] (die (apply str x xs))))

(defmacro condf
  "Like condp, but each clause predicate is called with the same value."
  [x & clauses]
  `(condp (fn [f# x#] (f# x#)) ~x ~@clauses))

(defmacro cond-lets
  "Try groups of let-style bindings and run the first successful body."
  {:style/indent [0]}
  [& clauses]
  (when clauses
    `(if-lets ~(first clauses)
       ~(if (next clauses)
          (second clauses)
          (die "Odd number of forms"))
       (cond-lets ~@(nnext clauses)))))

(defn eprint
  "Print values to stderr without adding a newline."
  [& xs]
  (binding [*out* *err*]
    (apply print xs)))

(defn eprintln
  "Print values to stderr with a newline."
  [& xs]
  (binding [*out* *err*]
    (apply println xs)))

(defmacro if-lets
  "Like if-let, but require every binding/test pair to succeed."
  ([bindings then]
   `(if-lets ~bindings ~then nil))
  ([bindings then else]
   (if (seq bindings)
     `(if-let [~(first bindings) ~(second bindings)]
        (if-lets ~(drop 2 bindings) ~then ~else)
        ~else)
     then)))

(defn macro?
  "Return true when a symbol resolves to a macro var."
  [x]
  (and
    (symbol? x)
    (when-let [x (resolve x)]
      (:macro (meta x)))))

(defn type-name
  "Return a readable type name for diagnostics."
  [x]
  (condf x
    map? "Map"
    set? "Set"
    vector? "Vector"
    list? "List"
    seq? "Seq"
    (type x)))

(defmacro when-lets
  "Like when-let, but require every binding/test pair to succeed."
  ([bindings & body]
   (if (seq bindings)
     `(when-let [~(first bindings) ~(second bindings)]
        (when-lets ~(drop 2 bindings) ~@body))
     `(do ~@body))))

(intern 'clojure.core 'die die)
(intern 'clojure.core 'eprint eprint)
(intern 'clojure.core 'eprintln eprintln)

(intern 'clojure.core
        (with-meta 'condf {:macro true})
        @#'condf)


(intern 'clojure.core
        (with-meta 'cond-lets {:macro true})
        @#'cond-lets)

(intern 'clojure.core
        (with-meta 'if-lets {:macro true})
        @#'if-lets)

(intern 'clojure.core
        (with-meta 'when-lets {:macro true})
        @#'when-lets)

(comment
  )
