;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.module.io
  (:require [ys.v0.reference :as reference])
  (:refer-clojure :exclude [print]))

(defn err [& xs]
  (binding [*out* *err*]
    (apply clojure.core/print (map reference/serialize xs))
    (flush)))

(defn out [& xs]
  (apply clojure.core/print (map reference/serialize xs))
  (flush))

(defn print [& xs]
  (apply clojure.core/print (map reference/serialize xs))
  (flush))

(defn say [& xs]
  (apply println (map reference/serialize xs))
  (try
    (flush)
    (catch go/any _ nil)))

(defn warn [& xs]
  (binding [*out* *err*]
    (apply println (map reference/serialize xs))
    (flush)))

(defn readline [reader]
  (binding [*in* reader]
    (clojure.core/read-line)))
