;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.global
  (:require
   [ys.v0.global :as v0])
  (:refer-clojure :exclude [create-ns intern ns-name resolve]))

(def main-ns 'main)
(def sci-ctx (atom nil))

(def stream-anchors_ v0/stream-anchors_)
(def doc-anchors_ v0/doc-anchors_)
(def stream-values v0/stream-values)
(def opts v0/opts)

(def pods (atom []))
(defonce build-xstr (atom nil))

(def PUN (atom nil))
(def FILE (atom nil))

(defn get-PUN [] @PUN)
(defn create-ns [ns] ns)
(defn ns-name [ns] ns)
(defn resolve [sym] (clojure.core/resolve sym))
(defn intern [ns sym val] (clojure.core/intern ns sym val))
(defn set-underscore [v]
  (alter-var-root #'v0/_ (constantly v)))
(defn update-environ [m]
  (alter-var-root #'v0/ENV (v0/make-environ-updater m)))

(reset! v0/underscore-hook set-underscore)
(reset! v0/environ-hook update-environ)

(defn update-env [m] (v0/update-env m))
(defn reset-env [m] (v0/reset-env m))

(def error-msg-prefix (atom ()))

(defn reset-error-msg-prefix!
  ([] (reset! error-msg-prefix "Error: "))
  ([prefix] (reset! error-msg-prefix prefix)))

(reset-error-msg-prefix!)
