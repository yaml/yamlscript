;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.csv
  (:require
   [clojure.data.csv :as csv]
   [clojure.string :as str])
  (:refer-clojure :exclude [read]))

(defn read-csv [s]
  (csv/read-csv (str/trim-newline s) :separator \,))

(defn read-tsv [s]
  (csv/read-csv (str/trim-newline s) :separator \tab))

(defn write-csv [data]
  (with-open [s (java.io.StringWriter.)]
    (csv/write-csv s data :separator \,)
    (str s)))

(defn write-tsv [data]
  (with-open [s (java.io.StringWriter.)]
    (csv/write-csv s data :separator \tab)
    (str s)))

(defn read [s]
  (read-csv s))

(defn write [data]
  (write-csv data))

(comment
  )
