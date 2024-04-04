;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; This library containss the clojure.core functions that are replace by the
;; ys::std library.
;; They can be accesed with clj/foo instead of foo.

(ns ys.clj
  (:refer-clojure :only [intern]))

(intern 'ys.clj 'compile   clojure.core/compile)
(intern 'ys.clj 'load      clojure.core/load)
(intern 'ys.clj 'load-file clojure.core/load-file)
(intern 'ys.clj 'num       clojure.core/num)
(intern 'ys.clj 'print     clojure.core/print)
(intern 'ys.clj 'use       clojure.core/use)
