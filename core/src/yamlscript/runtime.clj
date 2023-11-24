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

; pp pprint *command-line-args*
; parse-boolean parse-long parse-double
; cast destructure dosync time unquote-splicing update-keys update-vals

(def argv (sci/new-dynamic-var 'ARGV nil))
(def env (sci/new-dynamic-var 'ENV nil))

(declare ys-load)

(def sci-version
  (sci/new-var '*sci-version*
    (->>
      (io/resource "SCI_VERSION")
      slurp
      str/trim-newline
      (#(str/split % #"\."))
      (map #(if (re-matches #"\d+" %) (parse-long %) %))
      (zipmap [:major :minor :incremental :qualifier]))))

(defn clojure-core-vars []
  (let [core {'ARGV argv
              'ENV env
              '*sci-version* sci-version
              'load (sci/copy-var ys-load nil)
              'pprint (sci/copy-var clojure.pprint/pprint nil)
              'slurp (sci/copy-var clojure.core/slurp nil)
              'spit (sci/copy-var clojure.core/spit nil)}
        std (ns-publics 'ys.std)
        std (update-vals std #(sci/copy-var* % nil))]
    (merge core std)))

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

(def no-file "NO_SOURCE_PATH")
(defn eval-string
  ([clj]
   (let [file (if @sci/file
                (.getAbsolutePath (io/file @sci/file))
                no-file)]
     (eval-string clj file)))

  ([clj file] (eval-string clj file []))

  ([clj file args]
   (let [clj (str/trim-newline clj)
         file (or file no-file)
         file (if (= file no-file)
                file
                (.getAbsolutePath (io/file file)))]
     (if (= "" clj)
       ""
       (sci/binding
        [sci/file file
         argv (vec args)
         env (into {} (System/getenv))]
         (sci/eval-string clj (sci-ctx)))))))

  (comment
    (eval-string "(say (inc 123))"))
