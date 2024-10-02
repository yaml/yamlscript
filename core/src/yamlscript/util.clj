;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.util)

(declare die)

(defmacro cond-lets
  "if-lets but works like cond"
  {:style/indent [0]}
  [& clauses]
  (when clauses
    `(if-lets ~(first clauses)
       ~(if (next clauses)
          (second clauses)
          (die "Odd number of forms"))
       (cond-lets ~@(nnext clauses)))))

(defn die
  ([] (throw (Exception. "Died")))
  ([msg] (throw (Exception. (str msg "\n"))))
  ([x & xs] (die (apply str x xs))))

(defn eprint [& xs]
  (binding [*out* *err*]
    (apply print xs)))

(defn eprintln [& xs]
  (binding [*out* *err*]
    (apply println xs)))

(defmacro if-lets
  ([bindings then]
   `(if-lets ~bindings ~then nil))
  ([bindings then else]
   (if (seq bindings)
     `(if-let [~(first bindings) ~(second bindings)]
        (if-lets ~(drop 2 bindings) ~then ~else)
        ~else)
     then)))

(defn macro? [x]
  (and
    (symbol? x)
    (when-let [x (resolve x)]
      (:macro (meta x)))))

(defn type-name [x]
  (cond
    (map? x) "Map"
    (set? x) "Set"
    (vector? x) "Vector"
    (list? x) "List"
    (seq? x) "Seq"
    :else (type x)))

(defmacro when-lets
  ([bindings & body]
   (if (seq bindings)
     `(when-let [~(first bindings) ~(second bindings)]
        (when-lets ~(drop 2 bindings) ~@body))
     `(do ~@body))))

(intern 'clojure.core 'die die)
(intern 'clojure.core 'eprint eprint)
(intern 'clojure.core 'eprintln eprintln)

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
