
;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; Helper utility functions.

(ns yamlscript.util
  (:require
   [yamlscript.debug :refer [www]]))


(defmacro if-let*
  ([bindings then]
   `(if-let* ~bindings ~then nil))
  ([bindings then else]
   (if (seq bindings)
     `(if-let [~(first bindings) ~(second bindings)]
        (if-let* ~(drop 2 bindings) ~then ~else)
        ~else)
     then)))

(defmacro when-let*
  ([bindings & body]
   (if (seq bindings)
     `(when-let [~(first bindings) ~(second bindings)]
        (when-let* ~(drop 2 bindings) ~@body))
     `(do ~@body))))

(comment
  www)
