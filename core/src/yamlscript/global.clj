;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.global namespace owns mutable runtime state shared across the
;; compiler and SCI evaluator.

(ns yamlscript.global
  (:require
   [sci.core :as sci])
  (:refer-clojure :exclude [create-ns
                            intern
                            ns-name
                            resolve]))

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

(def _ (sci/new-dynamic-var 'ARGS nil {:ns main-ns}))
(def ENV (sci/new-dynamic-var 'ENV nil {:ns main-ns}))
(def PUN (sci/new-dynamic-var 'PUN nil {:ns main-ns}))

(defn get-PUN
  "Return PUN for the current context."
  []
  (sci/eval-string+ @sci-ctx "(var-get (resolve 'PUN))"))

(def env {})

(defn create-ns
  "Create an SCI namespace."
  [ns]
  (sci/create-ns ns))

(defn ns-name
  "Return the symbol name for an SCI namespace."
  [ns]
  (sci/ns-name ns))

(defn resolve
  "Walk a composed YAML node tree and tag every node by YS rules."
  [sym]
  (sci/resolve @sci-ctx sym))

(defn intern
  "Intern a value into an SCI namespace."
  [ns sym val]
  (sci/intern @sci-ctx ns sym val))

(defn set-underscore
  "Set underscore in the current context."
  [v]
  (sci/alter-var-root _ (constantly v)))

(defn update-environ
  "Update environ in the current context."
  [m]
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

(def FILE (sci/new-dynamic-var 'FILE nil))

(def error-msg-prefix (atom ()))
(defn reset-error-msg-prefix!
  "Reset error msg prefix! to its initial state."
  ([] (reset! error-msg-prefix "Error: "))
  ([prefix] (reset! error-msg-prefix prefix)))
(reset-error-msg-prefix!)

(comment
  )
