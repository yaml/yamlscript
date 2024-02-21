;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.ys
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [sci.core :as sci]
   [yamlscript.compiler]
   [yamlscript.re :as re]
   [yamlscript.util
    :refer [abspath
            die
            dirname
            get-yspath]]
   [yamlscript.debug :refer [www]])
  (:refer-clojure
   :exclude [compile
             load-file
             use]))

(defn -get-module [module]
  (when (not (re-matches (re/re #"(?:$nspc|$symw)") module))
    (die (str "Invalid module name: " module)))
  (let [module (str/replace module #"\." "/")]
    (str module ".ys")))

(declare load-file clj-load-file)

(defn -use-module [module & args]
  (let [file (-get-module module)
        clj-file (str/replace file #"\.ys$" ".clj")
        yspath (get-yspath @sci/file)]
    (loop [[path & yspath] yspath]
      (let [ys-path (str path "/" file)
            clj-path (str path "/" clj-file)]
        (if (.exists (io/as-file ys-path))
          (load-file ys-path)
          (if (.exists (io/as-file clj-path))
            (clj-load-file clj-path)
            (if (seq yspath)
              (recur yspath)
              (die (str "Module not found: " module)))))))))

;;-----------------------------------------------------------------------------
(defn compile [code]
  (yamlscript.compiler/compile code))

(def sci-ctx (atom nil))
(def FILE (sci/new-dynamic-var 'FILE nil))

(defn load-file [ys-file]
  (let [ys-file (abspath ys-file (dirname @sci/file))
        clj-code (->
                   ys-file
                   slurp
                   yamlscript.compiler/compile)
        ret (sci/binding
             [sci/file ys-file
              FILE ys-file]
              (sci/eval-string+ @sci-ctx clj-code))]
    (:val ret)))

(defn clj-load-file [clj-file]
  (let [clj-file (abspath clj-file (dirname @sci/file))
        clj-code (->
                   clj-file
                   slurp)
        ret (sci/binding
             [sci/file clj-file
              FILE clj-file]
              (sci/eval-string+ @sci-ctx clj-code))]
    (:val ret)))

(defmacro use [module & args]
  `(let [module# (str (quote ~module))]
     (-use-module module# ~@args)))

(comment
  www
  )
