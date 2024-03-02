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
   [babashka.pods.sci]
   [sci.core :as sci]
   [ys.std]
   [ys.json]
   [ys.yaml]
   [ys.ys :as ys]
   [yamlscript.util
    :refer [abspath
            get-yspath]]))

(def ys-version "0.1.36")

(def ARGS (sci/new-dynamic-var 'ARGS))
(def ARGV (sci/new-dynamic-var 'ARGV))
(def INC (sci/new-dynamic-var 'INC))

;; Define the clojure.core namespace that is referenced into all namespaces
(defn clojure-core-ns []
  (let [core {;; Runtime variables
              'ARGS ARGS
              'ARGV ARGV
              'CWD nil
              'ENV nil
              'FILE ys/FILE
              'INC INC
              'VERSION nil
              'VERSIONS nil

              ;; clojure.core functions overridden by YS
              'load (sci/copy-var ys.ys/load-file nil)
              'use (sci/copy-var ys.ys/use nil)

              ;; clojure.core functions not added by SCI
              'parse-double (sci/copy-var clojure.core/parse-double nil)
              'parse-long (sci/copy-var clojure.core/parse-long nil)
              'pprint (sci/copy-var clojure.pprint/pprint nil)
              'slurp (sci/copy-var clojure.core/slurp nil)
              'spit (sci/copy-var clojure.core/spit nil)}
        std (ns-publics 'ys.std)
        std (update-vals std #(sci/copy-var* %1 nil))]
    (merge core std)))

(defmacro use-ns [ns-name from-ns]
  `(let [ns# (sci/create-ns ~ns-name)]
     (sci/copy-ns ~from-ns ns#)))

(reset! ys/sci-ctx
  (sci/init
    {:namespaces
     {'clojure.core (clojure-core-ns)

      'ys.ys (use-ns 'ys.ys ys.ys)

      'fs  (use-ns 'fs  babashka.fs)
      'babashka.pods (use-ns 'babashka.pods babashka.pods.sci)
      'std (use-ns 'std ys.std)
      'str (use-ns 'str clojure.string)
      'ys  (use-ns 'ys  ys.ys)

      'ys.http (use-ns 'http babashka.http-client)
      'ys.json (use-ns 'json ys.json)
      'ys.yaml (use-ns 'yaml ys.yaml)}

     :classes
     {'Boolean   java.lang.Boolean
      'Character java.lang.Character
      'Long      java.lang.Long
      'Thread    java.lang.Thread

      'java.lang.Boolean   java.lang.Boolean
      'java.lang.Character java.lang.Character
      'java.lang.Long      java.lang.Long
      'java.lang.Thread    java.lang.Thread}}))

(sci/intern @ys/sci-ctx 'clojure.core 'CWD (str (babashka.fs/cwd)))
(sci/intern @ys/sci-ctx 'clojure.core 'ENV (into {} (System/getenv)))
(sci/intern @ys/sci-ctx 'clojure.core 'VERSION ys-version)
(sci/intern @ys/sci-ctx 'clojure.core 'VERSIONS
  {:clojure "1.11.1"
   :sci (->>
          (io/resource "SCI_VERSION")
          slurp
          str/trim-newline)
   :yamlscript ys-version})

(sci/alter-var-root sci/out (constantly *out*))
(sci/alter-var-root sci/err (constantly *err*))
(sci/alter-var-root sci/in (constantly *in*))

(defn eval-string
  ([clj] (eval-string clj @sci/file))

  ([clj file] (eval-string clj file []))

  ([clj file args]
   (let [clj (str/trim-newline clj)
         file (abspath (or file "NO-NAME"))]
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
         ys/FILE file
         INC (get-yspath file)]
         (sci/eval-string* @ys/sci-ctx clj))))))

(sci/intern @ys/sci-ctx 'clojure.core 'eval-string eval-string)

(comment
  www
  )
