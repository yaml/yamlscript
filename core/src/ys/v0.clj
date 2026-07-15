;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The ys.v0 namespace makes code compiled by `ys -c --v0` runnable by
;; plain Clojure runtimes like babashka and JVM Clojure. Compiled programs
;; start with:
;;
;;   (ns main (:require ys.v0))
;;   (ys.v0/init)
;;
;; init refers the full YS standard library into the calling namespace
;; (silently replacing any clojure.core mappings, so no replace warnings),
;; sets up the YS namespace aliases, and binds the YS runtime variables.
;;
;; Under the ys runtime this namespace is shadowed by a no-op SCI stub, so
;; the same compiled code runs identically there.

(ns ys.v0
  (:require
   [babashka.fs]
   [clojure.java.io]
   [clojure.math]
   [clojure.pprint]
   [clojure.set]
   [clojure.string :as str]
   [clojure.tools.cli]
   [clojure.walk]
   [ys.v0.clj]
   [ys.v0.common :as common]
   [ys.v0.csv]
   [ys.v0.debug]
   [ys.v0.dwim]
   [ys.v0.ext]
   [ys.v0.fs]
   [ys.v0.global :as global]
   [ys.v0.http]
   [ys.v0.json]
   [ys.v0.manifest :as manifest]
   [ys.v0.re :as re]
   [ys.v0.std]
   [ys.v0.taptest]
   [ys.v0.util :as util]
   [ys.v0.yaml]
   [ys.v0.ys]))

(def VERSION "0.2.28")

(defn- refer-exports
  "Refer every manifest export into the target namespace. Symbols are
  ns-unmapped first so clojure.core shadows replace silently."
  [target]
  (doseq [sym (manifest/exported-syms)]
    (ns-unmap target sym))
  (doseq [[ns-sym syms] manifest/refers]
    (if (= :all syms)
      (refer ns-sym)
      (refer ns-sym :only (vec syms))))
  ;; The YS runtime overrides clojure.core load and use with YS versions.
  ;; Here they refer to the ys.v0.ys bridge, which dies with a clear
  ;; message outside the ys runtime.
  (doseq [sym manifest/runtime-overrides]
    (ns-unmap target sym))
  (refer 'ys.v0.ys :only '[load-file use] :rename '{load-file load}))

(defn- refer-runtime-vars [target]
  (doseq [sym manifest/runtime-vars]
    (ns-unmap target sym))
  (refer 'ys.v0.global :only (vec manifest/runtime-vars)))

(defn- alias-namespaces [target]
  (doseq [[a ns-sym] manifest/aliases]
    (when-not (get (ns-aliases target) a)
      (alias a ns-sym))))

(defn- coerce-args
  "Coerce command line arguments the way the ys runtime does."
  [args]
  (vec
    (map #(cond
            (re-matches re/xnum %1)
            (read-string (str/replace %1 #"^([-+]?)0o"
                           (str "$1" "0")))
            ,
            (re-matches re/keyw %1)
            (keyword (subs %1 1))
            :else %1)
      args)))

(defn- normalize-os [os-name]
  (let [os-name (str/lower-case (or os-name ""))]
    (cond
      (str/includes? os-name "linux") "linux"
      (str/includes? os-name "mac") "macos"
      (str/includes? os-name "windows") "windows"
      :else "unknown")))

(defn- normalize-arch [arch]
  (let [arch (str/lower-case (or arch ""))]
    (case arch
      ("amd64" "x86_64") "x86_64"
      ("aarch64" "arm64") "aarch64"
      (if (seq arch) arch "unknown"))))

(defn- get-hostname []
  (or
    (try
      (not-empty (.getHostName (java.net.InetAddress/getLocalHost)))
      (catch Exception _ nil))
    (not-empty (System/getenv "HOSTNAME"))
    (not-empty (System/getenv "COMPUTERNAME"))))

(defn- get-runtime-info []
  {:args (try (common/get-cmd-args) (catch Exception _ []))
   :arch (normalize-arch (System/getProperty "os.arch"))
   :bin (try (common/get-cmd-bin) (catch Exception _ nil))
   :hostname (get-hostname)
   :os (normalize-os (System/getProperty "os.name"))
   :pid (try (common/get-cmd-pid) (catch Exception _ nil))
   :versions {:clojure (clojure-version)
              :yamlscript VERSION}
   :yspath (try (common/get-cmd-path) (catch Exception _ nil))})

(defn- set-var! [v value]
  (alter-var-root v (constantly value)))

(defn- bind-runtime-vars []
  (let [file (if (and *file* (not= *file* "NO_SOURCE_PATH"))
               (common/abspath *file*)
               (common/abspath "NO-NAME"))
        argv (vec (or *command-line-args* []))]
    (set-var! #'global/ARGS (coerce-args argv))
    (set-var! #'global/ARGV argv)
    (set-var! #'global/CWD (str (babashka.fs/cwd)))
    (set-var! #'global/DIR (common/dirname file))
    (set-var! #'global/ENV (into {} (System/getenv)))
    (set-var! #'global/FILE file)
    (set-var! #'global/INC (common/get-yspath file))
    (set-var! #'global/RUN (get-runtime-info))
    (set-var! #'global/VERSION VERSION)
    (global/reset-env nil)))

(defn init
  "Set up the calling namespace to run YS compiled code."
  ([] (init nil))
  ([opts]
   (let [target *ns*]
     (refer-exports target)
     (refer-runtime-vars target)
     (alias-namespaces target)
     (bind-runtime-vars)
     (when-let [v (:v opts)]
       (when (not= v VERSION)
         (binding [*out* *err*]
           (println (str "WARNING: code compiled by ys " v
                      " running with ys.v0 " VERSION)))))
     nil)))

(comment
  )
