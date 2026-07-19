;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The clojure.data.json backend resolves at call time so that Clojure
;; runtimes that can't load it (like jolt, currently) can still load
;; this namespace.

(ns ys.v0.json
  (:require
   [ys.v0.util :as util])
  (:refer-clojure :exclude [load]))

(defn load [str]
  ((util/backend 'clojure.data.json/read-str) str))

(defn dump [data]
  ((util/backend 'clojure.data.json/write-str) data))

(defn pretty [data]
  (with-out-str
    ((util/backend 'clojure.data.json/pprint) data)))

(comment
  )
