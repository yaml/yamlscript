;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.runtime
  (:require
   [yamlscript.debug]
   [yamlscript.re :as re]
   [clojure.java.io :as io]
   [clojure.math]
   [clojure.pprint]
   [clojure.set]
   [clojure.string :as str]
   [clojure.tools.cli]
   [clojure.walk]
   ; [clojure.zip]
   ; [babashka.deps]
   [babashka.fs]
   [babashka.http-client]
   [babashka.pods.sci]
   [babashka.process]
   [sci.core :as sci]
   [ys.clj]
   [ys.std]
   [ys.poly]
   [ys.json]
   [ys.yaml]
   [ys.ys :as ys]
   [ys.taptest]
   [yamlscript.common :as common]
   [yamlscript.util
    :refer [abspath
            get-yspath]]))

(def ys-version "0.1.66")

(def ARGS (sci/new-dynamic-var 'ARGS))
(def ARGV (sci/new-dynamic-var 'ARGV))
(def CWD (sci/new-dynamic-var 'CWD))
(def ENV (sci/new-dynamic-var 'ENV))
(def INC (sci/new-dynamic-var 'INC))

;; Define the clojure.core namespace that is referenced into all namespaces
(defn clojure-core-ns []
  (let [core {;; Runtime variables
              'ARGS ARGS
              'ARGV ARGV
              'CWD CWD
              'ENV ENV
              'FILE ys/FILE
              'INC INC
              'VERSION nil
              'VERSIONS nil
              '$ common/$
              '$# common/$#

              ;; clojure.core functions overridden by YS
              'load (sci/copy-var ys.ys/load-file nil)
              'use (sci/copy-var ys.ys/use nil)

              ;; clojure.core functions not added by SCI
              'file-seq (sci/copy-var clojure.core/file-seq nil)
              'parse-double (sci/copy-var clojure.core/parse-double nil)
              'parse-long (sci/copy-var clojure.core/parse-long nil)
              'pprint (sci/copy-var clojure.pprint/pprint nil)
              'slurp (sci/copy-var clojure.core/slurp nil)
              'spit (sci/copy-var clojure.core/spit nil)

              ;; YAMLScript debugging functions
              '_DBG (sci/copy-var clojure.core/_DBG nil)
              'PPP (sci/copy-var yamlscript.debug/PPP nil)
              'WWW (sci/copy-var yamlscript.debug/WWW nil)
              'XXX (sci/copy-var yamlscript.debug/XXX nil)
              'YYY (sci/copy-var yamlscript.debug/YYY nil)
              'ZZZ (sci/copy-var yamlscript.debug/ZZZ nil)
              }
        std (ns-publics 'ys.std)
        std (update-vals std #(sci/copy-var* %1 nil))
        poly (ns-publics 'ys.poly)
        poly (update-vals poly #(sci/copy-var* %1 nil))]
    (merge core std poly)))

(defn babashka-pods-ns []
  {'load-pod (sci/copy-var ys/load-pod nil)
   'unload-pod (sci/copy-var babashka.pods.sci/unload-pod nil)})

(defmacro use-ns [ns-name from-ns]
  `(let [ns# (sci/create-ns ~ns-name)]
     (sci/copy-ns ~from-ns ns#)))

(defn classes-map [class-symbols]
  (loop [[class-symbol & class-symbols] class-symbols
         m '{}]
    (if class-symbol
      (let [symbol (-> class-symbol
                     str
                     (str/replace #".*\." "")
                     symbol)
            class (eval class-symbol)]
        (recur class-symbols (assoc m
                               symbol class
                               class-symbol class)))
      m)))



(reset! ys/sci-ctx
  (sci/init
    {:namespaces
     {'main {}

      ;; These 2 need to be first:
      'clojure.core (clojure-core-ns)
      'ys.ys   (use-ns 'ys.ys ys.ys)

      'cli     (use-ns 'cli clojure.tools.cli)
      'clj     (use-ns 'clj ys.clj)
      'fs      (use-ns 'fs babashka.fs)
      'http    (use-ns 'http babashka.http-client)
      'io      (use-ns 'io clojure.java.io)
      'math    (use-ns 'math clojure.math)
      'pods    (babashka-pods-ns)
      'process (use-ns 'process babashka.process)
      'set     (use-ns 'set clojure.set)
      'str     (use-ns 'str clojure.string)
      'walk    (use-ns 'walk clojure.walk)

      'std     (use-ns 'std ys.std)
      'ys      (use-ns 'ys ys.ys)
      'json    (use-ns 'json ys.json)
      'yaml    (use-ns 'yaml ys.yaml)

      'ys.taptest (use-ns 'ys.taptest ys.taptest)}


     :classes (classes-map
                '[clojure.lang.Atom
                  clojure.lang.Fn
                  clojure.lang.Keyword
                  clojure.lang.Range
                  clojure.lang.Seqable
                  clojure.lang.Sequential
                  clojure.lang.Symbol

                  java.lang.Boolean
                  java.lang.Byte
                  java.lang.Character
                  java.lang.Class
                  java.lang.Double
                  java.lang.Error
                  java.lang.Exception
                  java.lang.Float
                  java.lang.Integer
                  java.lang.Long
                  java.lang.Math
                  java.lang.Number
                  java.lang.Object
                  java.lang.Process
                  java.lang.Runtime
                  java.lang.String
                  java.lang.System
                  java.lang.Thread
                  java.lang.Throwable

                  java.io.File])}))

(sci/intern @ys/sci-ctx 'clojure.core 'VERSION ys-version)
(sci/intern @ys/sci-ctx 'clojure.core 'VERSIONS
  {:clojure "1.11.1"
   :sci (->>
          (io/resource "SCI_VERSION")
          slurp
          str/trim-newline)
   :yamlscript ys-version})

(defn eval-string
  ([clj]
   (eval-string clj @sci/file))

  ([clj file]
   (eval-string clj file []))

  ([clj file args]
   (sci/alter-var-root sci/out (constantly *out*))
   (sci/alter-var-root sci/err (constantly *err*))
   (sci/alter-var-root sci/in (constantly *in*))

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
         CWD (str (babashka.fs/cwd))
         ENV (into {} (System/getenv))
         ys/FILE file
         INC (get-yspath file)]
         (let [resp (sci/eval-string+
                      @ys/sci-ctx
                      clj
                      {:ns (sci/create-ns 'main)})]
           (ys/unload-pods)
           (shutdown-agents)
           (:val resp)))))))

(sci/intern @ys/sci-ctx 'clojure.core 'eval-string eval-string)

(comment
  )
