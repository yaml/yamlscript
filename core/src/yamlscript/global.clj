;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.global
  (:require
   [sci.core :as sci])
  (:refer-clojure :exclude [create-ns
                            intern]))

#_(defn WWW [& xs]
  (println (apply str ">>> " xs) " <<<")
  (last xs))

(def main-ns (sci/create-ns 'main))
(def sci-ctx (atom nil))

(def stream-anchors_ (atom {}))
(def doc-anchors_ (atom {}))
(def stream-values (atom []))
(def opts (atom {}))
(def pods (atom []))
(defonce build-xstr (atom nil))

(def ENV (sci/new-dynamic-var 'ENV nil {:ns main-ns}))

(def env {})

(defn create-ns [ns]
  (sci/create-ns ns))

(defn intern [ns vym val]
  (sci/intern @sci-ctx ns vym val))

(defn update-environ [m]
  (sci/alter-var-root ENV
    (fn [env]
      (if (empty? m)
        (reduce dissoc env (keys env))
        (reduce-kv
          (fn [env k v]
            (if v
              (assoc env k v)
              (dissoc env k)))
          env m)))))

#_{:clj-kondo/ignore [:var-same-name-except-case]}
(defn update-env [m]
  (let [m (reduce-kv
            (fn [m k v] (if v (assoc m k v) (dissoc m k)))
            env m)]
    (alter-var-root #'env (constantly m))))

(defn reset-env [m]
  (let [m (or m (into {} (System/getenv)))]
    (alter-var-root #'env (constantly m))))

(def FILE (sci/new-dynamic-var 'FILE nil))

(def error-msg-prefix (atom ()))
(defn reset-error-msg-prefix!
  ([] (reset! error-msg-prefix "Error :"))
  ([prefix] (reset! error-msg-prefix prefix)))
(reset-error-msg-prefix!)

(comment
  )
