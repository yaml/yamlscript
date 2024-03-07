;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.runtime
  (:require
   [yamlscript.debug :refer [www]]
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
   [ys.std]
   [ys.json]
   [ys.yaml]
   [ys.ys :as ys]
   [yamlscript.util
    :refer [abspath
            get-yspath]]))

(def ys-version "0.1.40")

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

(defn babashka-pods-ns []
  {'load-pod (sci/copy-var ys/load-pod nil)
   'unload-pod (sci/copy-var babashka.pods.sci/unload-pod nil)})

(defmacro use-ns [ns-name from-ns]
  `(let [ns# (sci/create-ns ~ns-name)]
     (sci/copy-ns ~from-ns ns#)))

(reset! ys/sci-ctx
  (sci/init
    {:namespaces
     {'main {}

      'clojure.core (clojure-core-ns)

;; This needs to be first
      'ys.ys   (use-ns 'ys.ys ys.ys)

      'cli     (use-ns 'cli clojure.tools.cli)
      'fs      (use-ns 'fs babashka.fs)
      'http    (use-ns 'http babashka.http-client)
      'io      (use-ns 'io clojure.java.io)
      'math    (use-ns 'math clojure.math)
      'pods    (babashka-pods-ns)
      'process (use-ns 'process babashka.process)
      'set     (use-ns 'set clojure.set)
      'str     (use-ns 'str clojure.string)
      'walk    (use-ns 'str clojure.walk)

      'std     (use-ns 'std ys.std)
      'ys      (use-ns 'ys ys.ys)
      'json    (use-ns 'json ys.json)
      'yaml    (use-ns 'yaml ys.yaml)}

     :classes
     {'Boolean   java.lang.Boolean
      'Character java.lang.Character
      'Long      java.lang.Long
      'Math      java.lang.Math
      'System    java.lang.System
      'Thread    java.lang.Thread}}))

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
  www
  )
