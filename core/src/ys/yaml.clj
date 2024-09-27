;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.yaml
  (:require
   [clj-yaml.core :as yaml])
  (:refer-clojure :exclude [load]))

(defn load [str]
  (yaml/parse-string str
    :code-point-limit (* 10 1024 1024)))

(defn dump [data]
  (yaml/generate-string
    data
    :dumper-options
    {:flow-style :block}))

(comment
  )
