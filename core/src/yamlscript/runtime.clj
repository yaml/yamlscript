;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.runtime
  (:require
   [yamlscript.debug :refer [www]]
   [clojure.java.io :as io]
   [clojure.pprint]
   [clojure.string :as str]
   [sci.core :as sci]
   [ys.std]
   [ys.json]
   [ys.yaml]
   [ys.ys]))

(sci/alter-var-root sci/out (constantly *out*))
(sci/alter-var-root sci/err (constantly *err*))
(sci/alter-var-root sci/in (constantly *in*))

(def ys-std (sci/create-ns 'std))
(def ys-std-vars (sci/copy-ns ys.std ys-std))

(def ys-json (sci/create-ns 'json))
(def ys-json-vars (sci/copy-ns ys.json ys-json))

(def ys-yaml (sci/create-ns 'yaml))
(def ys-yaml-vars (sci/copy-ns ys.yaml ys-yaml))

(def ys-ys (sci/create-ns 'ys))
(def ys-ys-vars (sci/copy-ns ys.ys ys-ys))


(declare ys-load)

(defn sci-ctx []
  {:namespaces
   {'std ys-std-vars
    'json ys-json-vars
    'yaml ys-yaml-vars
    'ys ys-ys-vars
    'clojure.core (clojure-core-vars)}})

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
    (eval-string "(say (inc 123))"))
