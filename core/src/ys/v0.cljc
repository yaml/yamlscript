;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The ys.v0 namespace makes code compiled by `ys -T clj` (and the other
;; code targets) runnable by plain Clojure runtimes like babashka, JVM
;; Clojure, jolt and glojure. Compiled programs start with:
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
;;
;; Namespaces some runtimes lack (see manifest/optional-nses) load via
;; guarded requires; their aliases are only created when they loaded.

(ns ys.v0
  (:require
   [clojure.string :as str]
   [clojure.walk]
   [ys.v0.clj]
   [ys.v0.common :as common]
   [ys.v0.csv]
   [ys.v0.debug]
   [ys.v0.dwim]
   [ys.v0.ext]
   [ys.v0.global :as global]
   [ys.v0.http]
   [ys.v0.json]
   [ys.v0.manifest :as manifest]
   [ys.v0.re :as re]
   [ys.v0.std]
   [ys.v0.util :as util]
   [ys.v0.yaml]
   [ys.v0.ys]))

(def VERSION "0.2.29")

;; Load what this runtime can provide of the optional namespaces:
(doseq [ns-sym manifest/optional-nses]
  (util/catching (require ns-sym) nil))

(defn- refer-exports
  "Refer every manifest export into the target namespace. Symbols are
  ns-unmapped first so clojure.core shadows replace silently. (Needs
  glojure newer than 0.6.8, where ns-unmap was broken.)"
  [target]
  (doseq [sym (manifest/exported-syms)]
    (ns-unmap target sym))
  (doseq [[ns-sym syms] manifest/refers]
    (when (find-ns ns-sym)
      (if (= :all syms)
        (refer ns-sym)
        (refer ns-sym :only (vec syms)))))
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

(defn- alias-namespaces [_target]
  ;; catching instead of an ns-aliases lookup makes repeat init calls
  ;; harmless everywhere (and glojure's ns-aliases is broken anyway):
  (doseq [[a ns-sym] manifest/aliases]
    (when (find-ns ns-sym)
      (util/catching (alias a ns-sym) nil))))

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
    #?(:glj nil
       :default
       (util/catching
         (not-empty (.getHostName (java.net.InetAddress/getLocalHost)))
         nil))
    (not-empty (System/getenv "HOSTNAME"))
    (not-empty (System/getenv "COMPUTERNAME"))))

(defn- get-runtime-info []
  {:args (util/catching (common/get-cmd-args) [])
   :arch (normalize-arch (System/getProperty "os.arch"))
   :bin (util/catching (common/get-cmd-bin) nil)
   :hostname (get-hostname)
   :os (normalize-os (System/getProperty "os.name"))
   :pid (util/catching (common/get-cmd-pid) nil)
   :versions {:clojure #?(:glj (str "glojure " *glojure-version*)
                          :default (clojure-version))
              :yamlscript VERSION}
   :yspath (util/catching (common/get-cmd-path) nil)})

(defn- set-var! [v value]
  (alter-var-root v (constantly value)))

(defn- bind-runtime-vars []
  (let [file (if (and *file* (not= *file* "NO_SOURCE_PATH"))
               (common/abspath *file*)
               (common/abspath "NO-NAME"))
        argv (vec (or *command-line-args* []))]
    (set-var! #'global/ARGS (coerce-args argv))
    (set-var! #'global/ARGV argv)
    (set-var! #'global/CWD (common/cwd))
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
