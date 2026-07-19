;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The babashka.http-client backend resolves at call time so that Clojure
;; runtimes without it (like jolt) can still load this namespace.

(ns ys.v0.http
  (:require
   [ys.v0.util :as util])
  (:refer-clojure :exclude [get]))

(defn- backend [name]
  (util/backend (symbol "babashka.http-client" name)))

(defn delete [& args] (apply (backend "delete") args))
(defn get [& args] (apply (backend "get") args))
(defn head [& args] (apply (backend "head") args))
(defn patch [& args] (apply (backend "patch") args))
(defn post [& args] (apply (backend "post") args))
(defn put [& args] (apply (backend "put") args))

(comment
  )
