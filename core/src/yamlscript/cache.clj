;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.cache
  (:require
   [babashka.fs :as fs]
   [babashka.http-client :as http]
   [clj-commons.digest :as digest])
  (:refer-clojure :exclude [get set]))

(def ys-cache
  (or (System/getenv "YS_CACHE")
    "/tmp/ys-cache"))

(def ys-cache-dir
  (do
    (when (not (fs/exists? ys-cache))
      (fs/create-dir ys-cache))
    ys-cache))

(defn get [key]
  (let [sha1 (digest/sha1 key)
        cache (str ys-cache-dir "/" sha1)]
    (when (fs/exists? cache)
      (slurp cache))))

(defn set [key val]
  (let [sha1 (digest/sha1 key)
        cache (str ys-cache-dir "/" sha1)]
    (spit cache val)
    val))

(defn curl [url]
  (or (get url)
    (let [resp (http/get url)
          text (if-let [body (:body resp)]
                 (str body)
                 (die resp))]
      (set url text))))

(comment
  )
