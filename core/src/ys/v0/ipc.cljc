;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; Process functions owned by the ys::ipc public module.

(ns ys.v0.ipc
  (:require
   [clojure.string :as str]
   [ys.v0.global :as global]
   [ys.v0.util :as util]))

(defn- backend [name]
  (util/backend (symbol "babashka.process" name)))

(defn- process-opts [[opts & xs]]
  (let [opts (if (map? opts)
               [(assoc opts :env (or (:env opts) global/env))]
               [{:env global/env} opts])]
    (vec (concat opts xs))))

(defn exec [& xs]
  (apply (backend "exec") (process-opts xs)))

(defn process [& xs]
  (apply (backend "process") (process-opts xs)))

(defn sh [& xs]
  (apply (backend "sh") (process-opts xs)))

(defn shell [& xs]
  (apply (backend "shell") (process-opts xs)))

(defn sh-out [& xs]
  (let [ret (apply sh xs)]
    (when (not= 0 (:exit ret))
      (util/die (:err ret)))
    (str/trim-newline (:out ret))))

(defn bash [& xs]
  (let [cmd (str/join " " xs)]
    (sh "bash -c" cmd)))

(defn bash-out [& xs]
  (let [cmd (str/join " " xs)]
    (sh-out "bash -c" cmd)))

(comment
  )
