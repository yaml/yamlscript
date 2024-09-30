;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.runtime
  (:require
   ; [babashka.deps]
   [babashka.fs]
   [babashka.http-client]
   [babashka.pods.sci]
   [babashka.process]
   [clojure.java.io :as io]
   [clojure.math]
   [clojure.pprint]
   [clojure.set]
   [clojure.string :as str]
   [clojure.tools.cli]
   [clojure.walk]
   ; [clojure.zip]
   [sci.core :as sci]
   [yamlscript.common :as common]
   [yamlscript.debug]
   [yamlscript.global :as global]
   [yamlscript.re :as re]
   [yamlscript.util]
   [ys.clj]
   [ys.dwim]
   [ys.json]
   [ys.std]
   [ys.taptest]
   [ys.yaml]
   [ys.ys :as ys]))

(def ys-version "0.1.79")

(def ARGS (sci/new-dynamic-var 'ARGS [] {:ns global/main-ns}))
(def ARGV (sci/new-dynamic-var 'ARGV [] {:ns global/main-ns}))
(def CWD (sci/new-dynamic-var 'CWD nil {:ns global/main-ns}))
(def INC (sci/new-dynamic-var 'INC [] {:ns global/main-ns}))
(def RUN (sci/new-dynamic-var 'RUN {} {:ns global/main-ns}))

;; Define the clojure.core namespace that is referenced into all namespaces
(def clojure-core-ns
  (let [core {;; Runtime variables
              'ARGS ARGS
              'ARGV ARGV
              'CWD CWD
              'ENV global/ENV
              'FILE global/FILE
              'INC INC
              'RUN RUN
              'VERSION ys-version
              '$ global/$
              '$# global/$#

              ;; clojure.core functions overridden by YS
              'load (sci/copy-var ys.ys/load-file nil)
              'use (sci/copy-var ys.ys/use nil)

              ;; clojure.core functions not added by SCI
              'abs (sci/copy-var clojure.core/abs nil)
              'file-seq (sci/copy-var clojure.core/file-seq nil)
              'infinite? (sci/copy-var clojure.core/infinite? nil)
              'parse-double (sci/copy-var clojure.core/parse-double nil)
              'parse-long (sci/copy-var clojure.core/parse-long nil)
              'parse-uuid (sci/copy-var clojure.core/parse-uuid nil)
              'pprint (sci/copy-var clojure.pprint/pprint nil)
              'random-uuid (sci/copy-var clojure.core/random-uuid nil)
              'slurp (sci/copy-var clojure.core/slurp nil)
              'spit (sci/copy-var clojure.core/spit nil)
              'NaN? (sci/copy-var clojure.core/NaN? nil)

              ;; YAMLScript debugging functions
              'DBG (sci/copy-var yamlscript.debug/DBG nil)
              'PPP (sci/copy-var yamlscript.debug/PPP nil)
              'TTT (sci/copy-var yamlscript.debug/TTT nil)
              'WWW (sci/copy-var yamlscript.debug/WWW nil)
              'XXX (sci/copy-var yamlscript.debug/XXX nil)
              'YYY (sci/copy-var yamlscript.debug/YYY nil)
              'ZZZ (sci/copy-var yamlscript.debug/ZZZ nil)}
        std (ns-publics 'ys.std)
        std (update-vals std #(sci/copy-var* %1 nil))
        dwim (ns-publics 'ys.dwim)
        dwim (update-vals dwim #(sci/copy-var* %1 nil))]
    (merge core std dwim)))

(def babashka-pods-ns
  {'load-pod (sci/copy-var ys/load-pod nil)
   'unload-pod (sci/copy-var babashka.pods.sci/unload-pod nil)})

(defmacro use-ns [ns-name from-ns]
  `(sci/copy-ns ~from-ns (sci/create-ns ~ns-name)))

(def cli-namespace (use-ns 'cli clojure.tools.cli))
(def clj-namespace (use-ns 'clj ys.clj))
(def debug-namespace (use-ns 'yamlscript.debug yamlscript.debug))
(def fs-namespace (use-ns 'fs babashka.fs))
(def http-namespace (use-ns 'http babashka.http-client))
(def io-namespace (use-ns 'io clojure.java.io))
(def json-namespace (use-ns 'json ys.json))
(def math-namespace (use-ns 'math clojure.math))
(def process-namespace (use-ns 'process babashka.process))
(def set-namespace (use-ns 'set clojure.set))
(def std-namespace (use-ns 'std ys.std))
(def str-namespace (use-ns 'str clojure.string))
(def taptest-namespace (use-ns 'ys.taptest ys.taptest))
(def util-namespace (use-ns 'yamlscript.util yamlscript.util))
(def walk-namespace (use-ns 'walk clojure.walk))
(def yaml-namespace (use-ns 'yaml ys.yaml))
(def ys-namespace (use-ns 'ys ys.ys))

(def namespaces
  {'main {}

   ;; These need to be first:
   'clojure.core clojure-core-ns 'core clojure-core-ns
   'ys      ys-namespace      'ys.ys      ys-namespace
   'std     std-namespace     'ys.std     std-namespace
   'clj     clj-namespace     'ys.clj     clj-namespace

   'cli     cli-namespace     'ys.cli     cli-namespace
   'fs      fs-namespace      'ys.fs      fs-namespace
   'http    http-namespace    'ys.http    http-namespace
   'io      io-namespace      'ys.io      io-namespace
   'json    json-namespace    'ys.json    json-namespace
   'math    math-namespace    'ys.math    math-namespace
   'pods    babashka-pods-ns  'ys.pods    babashka-pods-ns
   'process process-namespace 'ys.process process-namespace
   'set     set-namespace     'ys.set     set-namespace
   'str     str-namespace     'ys.str     str-namespace
   'walk    walk-namespace    'ys.walk    walk-namespace
   'yaml    yaml-namespace    'ys.yaml    yaml-namespace

   'ys.taptest taptest-namespace
   'yamlscript.debug debug-namespace
   'yamlscript.util util-namespace})

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

(def classes
  (classes-map
    '[clojure.lang.Atom
      clojure.lang.Fn
      clojure.lang.Keyword
      clojure.lang.Numbers
      clojure.lang.Range
      clojure.lang.Seqable
      clojure.lang.Sequential
      clojure.lang.Symbol

      java.io.File

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

      java.math.BigDecimal
      java.math.BigInteger

      java.util.regex.Pattern]))

(reset! global/sci-ctx
  (sci/init
    {:namespaces namespaces
     :classes classes}))

(defn get-runtime-info []
  {:args (common/get-cmd-args)
   :bin (common/get-cmd-bin)
   :pid (common/get-cmd-pid)
   :versions {:clojure "1.12.0"
              ;; TODO Add graalvm and other versions
              :sci (->>
                     (io/resource "SCI_VERSION")
                     slurp
                     str/trim-newline)
              :yamlscript ys-version}
   :yspath (common/get-cmd-path)
   })

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
         file (common/abspath (or file "NO-NAME"))]
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
         RUN (get-runtime-info)
         CWD (str (babashka.fs/cwd))
         global/ENV (into {} (System/getenv))
         global/FILE file
         INC (common/get-yspath file)]
         (let [resp (sci/eval-string+
                      @global/sci-ctx
                      clj
                      {:ns global/main-ns})]
           (ys/unload-pods)
           (shutdown-agents)
           (:val resp)))))))

(sci/intern @global/sci-ctx 'clojure.core 'eval-string eval-string)

(comment
  )
