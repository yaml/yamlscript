;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.runtime
  (:require
   [yamlscript.debug :refer [www]]
   [yamlscript.re :as re]
   [clojure.java.io :as io]
   [clojure.pprint]
   [clojure.string :as str]
   [babashka.fs]
   [babashka.http-client]
   [sci.core :as sci]
   [ys.std]
   [ys.json]
   [ys.yaml]
   [ys.ys]))

(def ys-version "0.1.36")

(sci/alter-var-root sci/out (constantly *out*))
(sci/alter-var-root sci/err (constantly *err*))
(sci/alter-var-root sci/in (constantly *in*))

(def ys-std (sci/create-ns 'std))
(def ys-std-ns (sci/copy-ns ys.std ys-std))

(def ys-json (sci/create-ns 'json))
(def ys-json-ns (sci/copy-ns ys.json ys-json))

(def ys-yaml (sci/create-ns 'yaml))
(def ys-yaml-ns (sci/copy-ns ys.yaml ys-yaml))

(def ys-ys (sci/create-ns 'ys))
(def ys-ys-ns (sci/copy-ns ys.ys ys-ys))

(def clj-str (sci/create-ns 'str))
(def clj-str-ns (sci/copy-ns clojure.string clj-str))

(def bb-http (sci/create-ns 'http))
(def bb-http-ns (sci/copy-ns babashka.http-client bb-http))

(def bb-fs (sci/create-ns 'fs))
(def bb-fs-ns (sci/copy-ns babashka.fs bb-fs))

(def ARGS (sci/new-dynamic-var 'ARGS nil))
(def ARGV (sci/new-dynamic-var 'ARGV nil))
(def CWD (sci/new-dynamic-var 'CWD nil))
(def ENV (sci/new-dynamic-var 'ENV nil))
(def FILE (sci/new-dynamic-var 'FILE nil))
(def VERSION (sci/new-dynamic-var 'VERSION nil))
(def VERSIONS (sci/new-dynamic-var 'VERSIONS nil))

(declare ys-load)

(defn clojure-core-ns []
  (let [core {'ARGS ARGS
              'ARGV ARGV
              'CWD CWD
              'ENV ENV
              'FILE FILE
              'VERSION VERSION
              'VERSIONS VERSIONS
              'load (sci/copy-var ys-load nil)
              'parse-double (sci/copy-var clojure.core/parse-double nil)
              'parse-long (sci/copy-var clojure.core/parse-long nil)
              'pprint (sci/copy-var clojure.pprint/pprint nil)
              'slurp (sci/copy-var clojure.core/slurp nil)
              'spit (sci/copy-var clojure.core/spit nil)}
        std (ns-publics 'ys.std)
        std (update-vals std #(sci/copy-var* %1 nil))]
    (merge core std)))

(defn sci-ctx []
  {:namespaces
   {'ys.std ys-std-ns
    'ys.json ys-json-ns
    'ys.yaml ys-yaml-ns
    'ys.ys ys-ys-ns
    'fs bb-fs-ns
    'http bb-http-ns
    'str clj-str-ns
    'clojure.core (clojure-core-ns)}
   :classes
   {'Boolean java.lang.Boolean
    'java.lang.Boolean java.lang.Boolean
    'Character java.lang.Character
    'java.lang.Character java.lang.Character
    'Long java.lang.Long
    'java.lang.Long java.lang.Long}})

(defn ys-load
  ([file]
   (let [path (if (or (= "/EVAL" file) (= "/STDIN" file))
                file
                (.getParent
                  (io/file
                    (.getAbsolutePath (io/file @sci/file)))))
         file (io/file path file)
         ys-code (slurp file)
         clj-code (ys.ys/compile ys-code)]
     (sci/with-bindings
       {sci/ns @sci/ns}
       (sci/eval-string clj-code (sci-ctx)))))

  ([file path]
   (let [data (ys-load file)
         path
         (map #(if (re-matches #"\d+" %1) (parse-long %1) %1)
           (str/split path #"\."))]
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
         ARGS (vec
                (map #(cond (re-matches re/inum %1) (parse-long %1)
                            (re-matches re/fnum %1) (parse-double %1)
                            :else %1)
                  args))
         ARGV args
         CWD (str (babashka.fs/cwd))
         ENV (into {} (System/getenv))
         FILE file
         VERSION ys-version
         VERSIONS {:clojure "1.11.1"
                   :sci (->>
                          (io/resource "SCI_VERSION")
                          slurp
                          str/trim-newline)
                   :yamlscript ys-version}]
         (sci/eval-string clj (sci-ctx)))))))

(comment
  www
  (eval-string "(say (inc 123))")
  )
