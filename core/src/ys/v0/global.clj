;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The ys.v0.global namespace owns the mutable runtime state used by the YS
;; standard library. It is portable Clojure with no compiler or SCI
;; dependencies, so it works under the ys runtime, babashka, and JVM Clojure.
;; The ys runtime redirects the hook atoms below at its SCI counterparts.

(ns ys.v0.global)

(def stream-anchors_ (atom {}))
(def doc-anchors_ (atom {}))
(def stream-values (atom []))
(def opts (atom {}))

;; Runtime variables. Under the ys runtime these are shadowed by SCI dynamic
;; vars of the same names; under plain Clojure runtimes ys.v0/init binds them.
(def ^:dynamic _ nil)
(def ^:dynamic ARGS [])
(def ^:dynamic ARGV [])
(def ^:dynamic CWD nil)
(def ^:dynamic DIR nil)
(def ^:dynamic ENV nil)
(def ^:dynamic FILE nil)
(def ^:dynamic INC [])
(def ^:dynamic PUN nil)
(def ^:dynamic RUN {})
(def ^:dynamic VERSION nil)

(def env {})

(defn update-env
  "Update env in the current context."
  [m]
  (let [m (reduce-kv
            (fn [m k v] (if v (assoc m k v) (dissoc m k)))
            env m)]
    (alter-var-root #'env (constantly m))))

(defn reset-env
  "Reset env to its initial state."
  [m]
  (let [m (or m (into {} (System/getenv)))]
    (alter-var-root #'env (constantly m))))

(defn- default-set-underscore [v]
  (alter-var-root #'_ (constantly v)))

(defn- environ-updater [m]
  (fn [env]
    (if (empty? m)
      (reduce dissoc env (keys env))
      (reduce-kv
        (fn [env k v]
          (if v
            (assoc env k v)
            (dissoc env k)))
        env m))))

(defn- default-update-environ [m]
  (alter-var-root #'ENV (environ-updater m)))

;; The ys runtime resets these hooks to SCI-aware implementations.
(def underscore-hook (atom default-set-underscore))
(def environ-hook (atom default-update-environ))

(defn set-underscore
  "Set underscore in the current context."
  [v]
  (@underscore-hook v))

(defn update-environ
  "Update environ in the current context."
  [m]
  (@environ-hook m))

(defn make-environ-updater
  "Build the ENV update fn for a given map. Used by runtime hooks."
  [m]
  (environ-updater m))

(comment
  )
