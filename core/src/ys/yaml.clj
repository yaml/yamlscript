;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.yaml
  (:require
   [clj-yaml.core :as yaml])
  (:refer-clojure :exclude [load])
  #_(:import
   (java.util Optional)
   (org.snakeyaml.engine.v2.api DumpSettings)
   (org.snakeyaml.engine.v2.api.lowlevel Parse)
   ))

(defn load [str]
  (yaml/parse-string str
    :code-point-limit (* 10 1024 1024)))

(defn dump [data]
  (yaml/generate-string
    data
    :dumper-options
    {:flow-style :block}))

#_(defn dump-all [data]
  (yaml/generate-all
    data
    :dumper-options
    {:flow-style :block}))

(comment
  )
