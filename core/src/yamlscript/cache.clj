;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.cache namespace stores remote module content under a stable
;; key so repeated `use :url ...` loads do not refetch the same source.

(ns yamlscript.cache
  (:require
   [babashka.fs :as fs]
   [babashka.http-client :as http]
   [clj-commons.digest :as digest])
  (:refer-clojure :exclude [get set]))

(defn ys-cache
  "Return the root cache directory for YAMLScript downloads."
  []
  (or (System/getenv "YS_CACHE")
    "/tmp/ys-cache"))

(defn ys-cache-dir
  "Ensure and return the cache directory used for remote content."
  []
  (let [ys-cache (ys-cache)]
    (when-not (fs/exists? ys-cache)
      (fs/create-dir ys-cache))
    ys-cache))

(defn get
  "Read a cached value by key when the cache file exists."
  [key]
  (let [sha1 (digest/sha1 key)
        ys-cache-dir (ys-cache-dir)
        cache (str ys-cache-dir "/" sha1)]
    (when (fs/exists? cache)
      (slurp cache))))

(defn set
  "Write a cached value by key and return the stored value."
  [key val]
  (let [sha1 (digest/sha1 key)
        ys-cache-dir (ys-cache-dir)
        cache (str ys-cache-dir "/" sha1)]
    (spit cache val)
    val))

(defn curl
  "Fetch a URL through curl, caching the response by URL."
  [url]
  (or (get url)
    (let [resp (http/get url)
          text (if-let [body (:body resp)]
                 (str body)
                 (die resp))]
      (set url text))))

(comment
  )
