;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns use-test.file-with-dep
  (:require
   [use-test.file-helper :as helper]))

(defn file-total [] (+ helper/value 3))
