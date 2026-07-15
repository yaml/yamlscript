;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.runtime namespace builds the SCI context that evaluates the
;; Clojure emitted by the YAMLScript compiler.

(ns yamlscript.runtime
  (:require
   [babashka.pods]
   [babashka.pods.sci]
   [clojure.java.io :as io]
   [clojure.math]
   [clojure.pprint]
   [clojure.set]
   [clojure.string :as str]
   [clojure.tools.cli]
   [clojure.walk]
   ; [clojure.zip]
   [java-time.api]
   [sci.core :as sci]
   [ys.v0]
   [ys.v0.common :as common]
   [ys.v0.debug]
   [yamlscript.global :as global]
   [ys.v0.manifest :as manifest]
   [ys.v0.re :as re]
   [ys.v0.util]
   [ys.v0.clj]
   [ys.v0.csv]
   [ys.v0.dwim]
   [ys.v0.ext]
   [ys.v0.fs]
   [ys.v0.http]
   [ys.v0.json]
   [ys.v0.std]
   [ys.v0.taptest]
   [ys.v0.yaml]
   [ys.ys :as ys])
  (:import
   [java.net InetAddress UnknownHostException]))

(def ys-version ys.v0/VERSION)

(def ARGS (sci/new-dynamic-var 'ARGS [] {:ns global/main-ns}))
(def ARGV (sci/new-dynamic-var 'ARGV [] {:ns global/main-ns}))
(def CWD (sci/new-dynamic-var 'CWD nil {:ns global/main-ns}))
(def DIR (sci/new-dynamic-var 'DIR nil {:ns global/main-ns}))
(def INC (sci/new-dynamic-var 'INC [] {:ns global/main-ns}))
(def RUN (sci/new-dynamic-var 'RUN {} {:ns global/main-ns}))

;; Define the clojure.core namespace that is referenced into all namespaces.
;; The exported vars come from the ys.v0.manifest so the SCI runtime and
;; ys.v0/init (babashka, JVM Clojure) always expose the same set.
(def clojure-core-ns
  (let [core {;; Runtime variables
              '_ global/_
              'ARGS ARGS
              'ARGV ARGV
              'CWD CWD
              'DIR DIR
              'ENV global/ENV
              'FILE global/FILE
              'INC INC
              'PUN global/PUN
              'RUN RUN
              'VERSION ys-version

              ;; clojure.core functions overridden by YS
              'load (sci/copy-var ys.ys/load-file nil)
              'use (sci/copy-var ys.ys/use nil)}

        ;; clojure.core functions not added by SCI
        extras (-> (ns-publics 'clojure.core)
                 (select-keys manifest/sci-core-extras)
                 (update-vals #(sci/copy-var* %1 nil)))

        ;; The manifest exports (ys.v0.std, ys.v0.dwim, condf, debug fns...)
        refers (reduce
                 (fn [m [ns-sym syms]]
                   (let [publics (ns-publics ns-sym)
                         publics (if (= :all syms)
                                   publics
                                   (select-keys publics syms))]
                     (merge m (update-vals publics
                                #(sci/copy-var* %1 nil)))))
                 {} manifest/refers)]
    (merge core extras refers)))

(def pods-namespace
  {'load-pod (sci/copy-var ys/load-pod nil)
   'unload-pod (sci/copy-var babashka.pods.sci/unload-pod nil)})
(def babashka-pods-ns
  (sci/copy-ns babashka.pods (sci/create-ns 'babashka.pods)))
(def babashka-pods-sci-ns
  (sci/copy-ns babashka.pods.sci (sci/create-ns 'babashka.pods.sci)))

(def cli-namespace
  (sci/copy-ns clojure.tools.cli (sci/create-ns 'cli)))
(def clj-namespace
  (sci/copy-ns ys.v0.clj (sci/create-ns 'clj)))
(def csv-namespace
  (sci/copy-ns ys.v0.csv (sci/create-ns 'csv)))
(def debug-namespace
  (sci/copy-ns ys.v0.debug (sci/create-ns 'ys.v0.debug)))
(def fs-namespace
  (sci/copy-ns ys.v0.fs (sci/create-ns 'fs)))
(def java-time-namespace
  (sci/copy-ns java-time.api (sci/create-ns 'java-time.api)))
(def http-namespace
  (sci/copy-ns ys.v0.http (sci/create-ns 'http)))
(def io-namespace
  (sci/copy-ns clojure.java.io (sci/create-ns 'io)))
(def json-namespace
  (sci/copy-ns ys.v0.json (sci/create-ns 'json)))
(def math-namespace
  (sci/copy-ns clojure.math (sci/create-ns 'math)))
(def set-namespace
  (sci/copy-ns clojure.set (sci/create-ns 'set)))
(def ext-namespace
  (sci/copy-ns ys.v0.ext (sci/create-ns 'ys.ext)))
(def std-namespace
  (sci/copy-ns ys.v0.std (sci/create-ns 'std)))
(def str-namespace
  (sci/copy-ns clojure.string (sci/create-ns 'str)))
(def taptest-namespace
  (sci/copy-ns ys.v0.taptest (sci/create-ns 'ys.taptest)))
(def util-namespace
  (sci/copy-ns ys.v0.util (sci/create-ns 'ys.v0.util)))
(def walk-namespace
  (sci/copy-ns clojure.walk (sci/create-ns 'walk)))
(def yaml-namespace
  (sci/copy-ns ys.v0.yaml (sci/create-ns 'yaml)))
(def ys-namespace
  (sci/copy-ns ys.ys (sci/create-ns 'ys)))
(def v0-ys-namespace
  (sci/copy-ns ys.v0.ys (sci/create-ns 'ys.v0.ys)))

;; The SCI copies of the host namespaces that user code can reach.
(def host-namespaces
  {'ys.v0.std std-namespace
   'ys.v0.clj clj-namespace
   'ys.v0.csv csv-namespace
   'ys.v0.ext ext-namespace
   'ys.v0.fs fs-namespace
   'ys.v0.http http-namespace
   'ys.v0.json json-namespace
   'ys.v0.taptest taptest-namespace
   'ys.v0.yaml yaml-namespace
   'ys.v0.ys v0-ys-namespace
   'clojure.tools.cli cli-namespace
   'clojure.java.io io-namespace
   'clojure.math math-namespace
   'clojure.set set-namespace
   'clojure.string str-namespace
   'clojure.walk walk-namespace})

;; A no-op ys.v0 namespace so that code compiled with `ys -c --v0` (which
;; starts with `(ns main (:require ys.v0))` and `(ys.v0/init)`) also runs
;; under the ys runtime, where everything is already set up.
(def v0-namespace
  {'init (fn [& _] nil)
   'VERSION ys-version})

(def namespaces
  (merge
    {'main {}
     'clojure.core clojure-core-ns 'core clojure-core-ns}

    ;; The user-visible aliases (str, json, ys, std...) from the manifest
    (update-vals manifest/aliases host-namespaces)

    ;; ys.v0.* host namespace names; macro expansions of stdlib macros
    ;; resolve these directly
    (select-keys host-namespaces
      (filter #(str/starts-with? (str %1) "ys.v0.")
        (keys host-namespaces)))

    ;; SCI-runtime-only namespaces
    {'ys.v0 v0-namespace
     'pods    pods-namespace 'ys.pods    pods-namespace
     'babashka.pods     babashka-pods-ns
     'babashka.pods.sci babashka-pods-sci-ns
     'java-time java-time-namespace
     'ys.v0.debug debug-namespace 'yamlscript.debug debug-namespace
     'ys.v0.util util-namespace   'yamlscript.util util-namespace}))

(defn classes-map
  "Build SCI class lookup entries from fully qualified class symbols."
  [class-symbols]
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

      java.security.MessageDigest

      java.util.regex.Pattern]))

(reset! global/sci-ctx
  (sci/init
    {:namespaces namespaces
     :classes classes}))

(defn- normalize-os
  "Return the stable YS name for the host operating system."
  [os-name]
  (let [os-name (str/lower-case (or os-name ""))]
    (cond
      (str/includes? os-name "linux") "linux"
      (str/includes? os-name "mac") "macos"
      (str/includes? os-name "windows") "windows"
      :else "unknown")))

(defn- normalize-arch
  "Return the stable YS name for the host architecture."
  [arch]
  (let [arch (str/lower-case (or arch ""))]
    (case arch
      ("amd64" "x86_64") "x86_64"
      ("aarch64" "arm64") "aarch64"
      (if (seq arch) arch "unknown"))))

(defn- get-hostname
  "Return the system hostname when it can be determined."
  []
  (or
    (try
      (not-empty (.getHostName (InetAddress/getLocalHost)))
      (catch UnknownHostException _ nil)
      (catch SecurityException _ nil))
    (not-empty (System/getenv "HOSTNAME"))
    (not-empty (System/getenv "COMPUTERNAME"))))

(defn get-runtime-info
  "Return runtime version and platform information for YS code."
  []
  {:args (common/get-cmd-args)
   :arch (normalize-arch (System/getProperty "os.arch"))
   :bin (common/get-cmd-bin)
   :hostname (get-hostname)
   :os (normalize-os (System/getProperty "os.name"))
   :pid (common/get-cmd-pid)
   :versions {:clojure "1.12.0"
              ;; TODO Add graalvm and other versions
              :sci (->>
                     (io/resource "SCI_VERSION")
                     slurp
                     str/trim-newline)
              :yamlscript ys-version}
   :yspath (common/get-cmd-path)})

(defn eval-string
  "Evaluate generated Clojure code in the YAMLScript SCI context."
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
                (map #(cond
                        (re-matches re/xnum %1)
                        (read-string (str/replace %1 #"^([-+]?)0o"
                                       (str "$1" "0")))
                        ,
                        (re-matches re/keyw %1)
                        (keyword (subs %1 1))
                        :else %1)
                  args))
         ARGV args
         RUN (get-runtime-info)
         CWD (str (ys.v0.fs/cwd))
         DIR (common/dirname file)
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
