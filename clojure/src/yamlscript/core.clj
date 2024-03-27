(ns yamlscript.core
  (:require
   [clojure.data.json :as json])
   (:refer-clojure :exclude [load])
  (:import (org.yamlscript.yamlscript YAMLScript)))

(defn load [ys-code]
  (let [result (json/read-str (.getRAWResult (YAMLScript.) ys-code))
        error (result "error")]
    (if error
      (throw (Exception. (error "cause")))
      (result "data"))))
