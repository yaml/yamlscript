;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; This library contains the clojure.core functions that are replaced by the
;; ys::std library.
;; They can be accessed with clj/foo instead of foo.

(ns ys.clj
  (:refer-clojure :only [intern]))

(intern 'ys.clj 'compile   clojure.core/compile)
(intern 'ys.clj 'eval      clojure.core/eval)
(intern 'ys.clj 'load      clojure.core/load)
(intern 'ys.clj 'load-file clojure.core/load-file)
(intern 'ys.clj 'print     clojure.core/print)
(intern 'ys.clj 'use       clojure.core/use)
