;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The clojure.data.csv backend resolves at call time so that Clojure
;; runtimes without it can still load this namespace. The writer interop
;; lives in a private helper so its analysis is also deferred on
;; runtimes that analyze function bodies lazily.

(ns ys.v0.csv
  (:require
   [clojure.string :as str]
   [ys.v0.util :as util])
  (:refer-clojure :exclude [read]))

(defn read-csv [s]
  ((util/backend 'clojure.data.csv/read-csv)
   (str/trim-newline s) :separator \,))

(defn read-tsv [s]
  ((util/backend 'clojure.data.csv/read-csv)
   (str/trim-newline s) :separator \tab))

(defn- write-str [write-fn data sep]
  (with-open [s (java.io.StringWriter.)]
    (write-fn s data :separator sep)
    (str s)))

(defn write-csv [data]
  (write-str (util/backend 'clojure.data.csv/write-csv) data \,))

(defn write-tsv [data]
  (write-str (util/backend 'clojure.data.csv/write-csv) data \tab))

(defn read [s]
  (read-csv s))

(defn write [data]
  (write-csv data))

(comment
  )
