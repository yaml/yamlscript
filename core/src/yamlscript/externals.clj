;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.externals
  (:require
   [babashka.pods.sci :as pods]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [sci.core :as sci]
   [yamlscript.common :refer [abspath dirname get-yspath]]
   [yamlscript.compiler]
   [yamlscript.global :as global]
   [yamlscript.re :as re]))

(defn load-pod [args]
  (let [pod (apply pods/load-pod @global/sci-ctx args)]
    (swap! global/pods conj pod)))

(defn unload-pods []
  (doseq [pod @global/pods]
    (pods/unload-pod pod))
  (reset! global/pods []))

(defn get-module [module]
  (clojure.core/when (not (re-matches (re/re #"(?:$nspc|$symw)") module))
    (die (str "Invalid module name: " module)))
  (let [module (str/replace module #"\." "/")]
    (str module ".ys")))

(defn load-file-clj [clj-file]
  (let [clj-file (abspath clj-file (dirname @sci/file))
        clj-code (->
                   clj-file
                   slurp)
        ret (sci/binding
             [sci/file clj-file
              global/FILE clj-file]
              (sci/eval-string+ @global/sci-ctx clj-code))]
    (:val ret)))

(defn load-file-ys [ys-file]
  (let [ys-file (abspath ys-file (dirname @sci/file))
        clj-code (-> ys-file slurp yamlscript.compiler/compile)

        ;; XXX Duplicate logic from ys.ys/eval
        stream @global/stream-values
        _ (reset! global/stream-values [])
        ret (sci/binding
             [sci/file ys-file
              global/FILE ys-file]
              (sci/eval-string+ @global/sci-ctx clj-code))
        _ (reset! global/stream-values stream)]
    (:val ret)))

(defn use-module [module & args]
  (let [file (get-module module)
        clj-file (str/replace file #"\.ys$" ".clj")
        yspath (get-yspath @sci/file)]
    (loop [[path & yspath] yspath]
      (let [ys-path (str path "/" file)
            clj-path (str path "/" clj-file)]
        (if (.exists (io/as-file ys-path))
          (load-file-ys ys-path)
          (if (.exists (io/as-file clj-path))
            (load-file-clj clj-path)
            (if (seq yspath)
              (recur yspath)
              (die (str "Module not found: " module)))))))))

(comment
  )
