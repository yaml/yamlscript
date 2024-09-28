;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.global
  (:require
   [sci.core :as sci]))

(def $ (atom {}))
(def $# (atom 0))

(def sci-ctx (atom nil))
(def main-ns (sci/create-ns 'main))

(defonce build-vstr (atom nil))

(defonce env (into {} (System/getenv)))

(def ENV (sci/new-dynamic-var 'ENV env {:ns main-ns}))

(def FILE (sci/new-dynamic-var 'FILE nil))

(def opts (atom {}))

(def pods (atom []))

(def stream-anchors_ (atom {}))
(def doc-anchors_ (atom {}))

(def error-msg-prefix (atom ()))
(defn reset-error-msg-prefix!
  ([] (reset! error-msg-prefix "Error :"))
  ([prefix] (reset! error-msg-prefix prefix)))
(reset-error-msg-prefix!)

(comment
  )
