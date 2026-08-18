;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.v0.util)

(defn die
  "Throw a YAMLScript exception with a normalized trailing newline.
  Thrown as ex-info because every Clojure runtime YS targets supports
  it (there is no Exception class outside the JVM family)."
  ([] (die "Died"))
  ([msg] (throw (ex-info (str msg "\n") {})))
  ([x & xs] (die (apply str x xs))))

(defmacro catching
  "Evaluate expr, returning fallback if anything is thrown. The caught
  type is chosen when THIS file loads (via reader conditional), so
  consumers stay portable across runtimes with different catch types
  (glojure catches go/any; the JVM family catches Exception)."
  [expr fallback]
  `(try
     ~expr
     (catch #?(:glj go/any :default Exception) e# ~fallback)))

(defn backend
  "Resolve a backend library var at call time, so that Clojure runtimes
  without the library (like jolt) can still load the ys.v0 namespaces.
  Dies with a clear message when the library is not available.
  Requires then resolves (rather than requiring-resolve) because some
  runtimes only resolve reliably in already loaded namespaces."
  [sym]
  (or
    (catching
      (do
        (require (symbol (namespace sym)))
        (resolve sym))
      nil)
    (die (str "The '" (namespace sym) "' library is not available"
           " in this Clojure runtime"))))

(defn- resolve-assignment-key
  "Resolve one dotted-assignment key against its current container."
  [container [kind value]]
  (case kind
    :bare
    (if (map? container)
      (cond
        (contains? container value) value
        (contains? container (str value)) (str value)
        (contains? container (keyword value)) (keyword value)
        :else (str value))
      (str value))

    :call (value container)
    :value value))

(defn- resolve-assignment-path
  "Resolve dotted-assignment step descriptors into an exact key path."
  [root steps]
  (loop [container root
         steps (seq steps)
         path []]
    (if-let [step (first steps)]
      (let [key (resolve-assignment-key container step)]
        (recur (get container key) (next steps) (conj path key)))
      path)))

(defn assoc-assignment-path
  "Associate a value at a dotted-assignment path."
  [root steps value]
  (assoc-in root (resolve-assignment-path root steps) value))

(defn update-assignment-path
  "Apply an update function at a dotted-assignment path."
  [root steps f & args]
  (apply update-in root (resolve-assignment-path root steps) f args))

(defn pprint*
  "Pretty print x via clojure.pprint when available, else plain prn."
  [x]
  (if-let [f (catching (backend 'clojure.pprint/pprint) nil)]
    (f x)
    (prn x)))

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
