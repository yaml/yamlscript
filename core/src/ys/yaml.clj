;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.yaml
  (:require
   [yamlscript.debug :refer [www]]
   [clj-yaml.core :as yaml])
  (:refer-clojure :exclude [load]))

(defn load [str]
  (yaml/parse-string str))

(defn dump [data]
  (yaml/generate-string
    data
    :dumper-options
    {:flow-style :block}))

(comment)
