;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; This library contains the clojure.core functions that are replaced by the
;; ys::std library.
;; They can be accessed with clj/foo instead of foo.
;;
;; Functions that the host Clojure runtime does not provide (babashka has no
;; compile, load or load-file) are skipped.

(ns ys.v0.clj
  (:refer-clojure :only [doseq intern ns-resolve var-get when-let]))

(doseq [sym '[atom compile eval load load-file
              print read replace reverse set use]]
  (when-let [v (ns-resolve 'clojure.core sym)]
    (intern 'ys.v0.clj sym (var-get v))))
