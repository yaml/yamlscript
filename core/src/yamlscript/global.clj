;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.global namespace owns the SCI-coupled runtime state shared
;; across the compiler and SCI evaluator. The portable state lives in
;; ys.v0.global; this namespace re-exports it for compiler code and installs
;; the SCI-aware hooks.

(ns yamlscript.global
  (:require
   [sci.core :as sci]
   [ys.v0.global :as v0])
  (:refer-clojure :exclude [create-ns
                            intern
                            ns-name
                            resolve]))

(def main-ns (sci/create-ns 'main))
(def sci-ctx (atom nil))

;; Portable state re-exports (same atom objects as ys.v0.global)
(def stream-anchors_ v0/stream-anchors_)
(def doc-anchors_ v0/doc-anchors_)
(def stream-values v0/stream-values)
(def opts v0/opts)

(def pods (atom []))
(defonce build-xstr (atom nil))

(def _ (sci/new-dynamic-var 'ARGS nil {:ns main-ns}))
(def ENV (sci/new-dynamic-var 'ENV nil {:ns main-ns}))
(def PUN (sci/new-dynamic-var 'PUN nil {:ns main-ns}))

(defn get-PUN
  "Return PUN for the current context."
  []
  (sci/eval-string+ @sci-ctx "(var-get (resolve 'PUN))"))

(defn create-ns
  "Create an SCI namespace."
  [ns]
  (sci/create-ns ns))

(defn ns-name
  "Return the symbol name for an SCI namespace."
  [ns]
  (sci/ns-name ns))

(defn resolve
  "Resolve a symbol in the SCI context."
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
  (sci/alter-var-root ENV (v0/make-environ-updater m)))

;; Route the stdlib's portable hooks at the SCI implementations
(reset! v0/underscore-hook set-underscore)
(reset! v0/environ-hook update-environ)

(defn update-env
  "Update env in the current context."
  [m]
  (v0/update-env m))

(defn reset-env
  "Reset env to its initial state."
  [m]
  (v0/reset-env m))

(def FILE (sci/new-dynamic-var 'FILE nil))

(def error-msg-prefix (atom ()))
(defn reset-error-msg-prefix!
  "Reset error msg prefix! to its initial state."
  ([] (reset! error-msg-prefix "Error: "))
  ([prefix] (reset! error-msg-prefix prefix)))
(reset-error-msg-prefix!)

(comment
  )
