;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.core
  (:require
   [clojure.data.json :as json])
   (:refer-clojure :exclude [load])
  (:import (org.yamlscript.yamlscript YAMLScript)))

(defn load [ys-code]
  (let [result (json/read-str (.getRAWResult (YAMLScript.) ys-code))
        data (result "data")
        error (result "error")]
    (cond
      error (throw (Exception. (error "cause")))
      data data
      :else (throw
              (Exception. "Unexpected response from 'libyamlscript'")))))
