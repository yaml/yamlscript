;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.module.pprint
  (:require
   [ys.v0.pprint :as pprint]))

(def write pprint/write)

(defn pp
  ([value]
   (print (write value :stream nil))
   (println))
  ([value writer]
   (binding [*out* writer]
     (print (write value :stream nil))
     (println))))
