;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; Helper utility functions.

(ns yamlscript.util
  (:require
   [babashka.fs
    :refer [cwd]]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defonce build-vstr (atom nil))

(defn abspath
  ([path] (abspath path (str (cwd))))
  ([path base]
   (if (-> path io/file .isAbsolute)
     path
     (.getAbsolutePath (io/file (abspath base) path)))))

(defn die
  ([msg] (throw (Exception. (str msg "\n"))))
  ([x & xs] (die (apply str x xs))))

(defn dirname [path]
  (->
    path
    io/file
    .getParent
    (or ".")))

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

(defn get-yspath [base]
  (let [yspath (or
                 (get (System/getenv) "YSPATH")
                 (when (re-matches #"/NO-NAME$" base) (str (cwd)))
                 (->
                   base
                   dirname
                   abspath))
        _ (when-not yspath
            (die "YSPATH environment variable not set"))]
    (map abspath (str/split yspath #":"))))

(defmacro if-lets
  ([bindings then]
   `(if-lets ~bindings ~then nil))
  ([bindings then else]
   (if (seq bindings)
     `(if-let [~(first bindings) ~(second bindings)]
        (if-lets ~(drop 2 bindings) ~then ~else)
        ~else)
     then)))

(defmacro when-lets
  ([bindings & body]
   (if (seq bindings)
     `(when-let [~(first bindings) ~(second bindings)]
        (when-lets ~(drop 2 bindings) ~@body))
     `(do ~@body))))

(comment
  )
