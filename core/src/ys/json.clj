;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.json
  (:require
   [clojure.data.json :as json])
  (:refer-clojure :exclude [load]))

(defn load [str]
  (json/read-str str))

(defn dump [data]
  (json/write-str data))

(defn pretty [data]
  (with-out-str
    (json/pprint data)))

(comment
  )
