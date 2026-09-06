;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; Stream I/O functions owned by the ys::io public module.

(ns ys.v0.io
  (:require
   [ys.v0.global :as global]
   [ys.v0.util :as util])
  (:refer-clojure :exclude [print]))

(defn err [& xs]
  (binding [*out* (global/current-error)]
    (apply clojure.core/print xs)
    (flush)))

(defn out [& xs]
  (binding [*out* (global/current-output)]
    (apply clojure.core/print xs)
    (flush)))

(defn pp [x]
  (binding [*out* (global/current-output)]
    (util/pprint* x)))

(defn print [& xs]
  (binding [*out* (global/current-output)]
    (apply clojure.core/print xs)
    (flush)))

(defn say [& xs]
  (binding [*out* (global/current-output)]
    (apply println xs)))

(defn warn [& xs]
  (binding [*out* (global/current-error)]
    (apply println xs)
    (flush)))

(defn readline [reader]
  (binding [*in* reader]
    (clojure.core/read-line)))

(comment
  )
