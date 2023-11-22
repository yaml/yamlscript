(ns yamlscript.runtime
  (:require
   [yamlscript.debug :refer [www]]
   ;; Small Clojure Interpreter runtime for YAMLScript evaluation
   [sci.core :as sci]
   [ys.std]
   [ys.ys]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.string :as str]
   ))

(sci/alter-var-root sci/out (constantly *out*))
(sci/alter-var-root sci/err (constantly *err*))
(sci/alter-var-root sci/in (constantly *in*))

(def ys-ys (sci/create-ns 'ys))
(def ys-ys-vars (sci/copy-ns ys.ys ys-ys))

(def ys-std (sci/create-ns 'std))
(def ys-std-vars (sci/copy-ns ys.std ys-std))

(declare ys-load sci-ctx)

(defn sci-ctx []
  {:namespaces
   {'ys ys-ys-vars
    'std ys-std-vars
    'clojure.core
    {'load (sci/copy-var ys-load nil)
     'pprint (sci/copy-var clojure.pprint/pprint nil)
     'slurp (sci/copy-var clojure.core/slurp nil)
     'spit (sci/copy-var clojure.core/spit nil)
     'say (sci/copy-var ys.std/say nil)}}})

(defn ys-load
  ([file]
   (let [path (.getAbsolutePath (io/file @sci/file))
         path (.getParent (io/file path))
         file (io/file path file)
         ys-code (slurp file)
         clj-code (ys.ys/compile ys-code)]
     (sci/with-bindings
       {sci/ns @sci/ns}
       (sci/eval-string clj-code (sci-ctx)))))

  ([file path]
   (let [data (ys-load file)
         path
         (reduce
           #(conj %1 (if (re-matches #"\d+" %2) (parse-long %2) %2))
           [] (str/split path #"\."))]
     (get-in data path))))

(defn eval-string [clj & [file]]
  (let [clj (str/trim-newline clj)
        file (if file
               (.getAbsolutePath (io/file file))
               (if @sci/file
                 (.getAbsolutePath (io/file @sci/file))
                 "NO_SOURCE_PATH"))]
    (if (= "" clj)
      ""
      (sci/binding
       [sci/file file]
        (sci/eval-string clj (sci-ctx))))))

(comment
  (eval-string "(say (inc 123))")
  )
