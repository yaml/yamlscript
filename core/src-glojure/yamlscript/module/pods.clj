;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.module.pods
  (:require [ys.v0.ys :as ys]))

(defn load-pod [& args]
  (apply ys/load-pod args))

(defn unload-pods []
  (ys/unload-pods))
