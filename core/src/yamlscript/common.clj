;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.common)

(def opts (atom
            (if (System/getenv "YS_UNORDERED")
              {:unordered true}
              {})))

(def $ (atom {}))
(def $# (atom 0))

(def stream-anchors_ (atom {}))
(def doc-anchors_ (atom {}))

(def error-msg-prefix (atom ()))
(defn reset-error-msg-prefix!
  ([] (reset! error-msg-prefix "Error :"))
  ([prefix] (reset! error-msg-prefix prefix)))
(reset-error-msg-prefix!)

(comment
  )
