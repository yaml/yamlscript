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

(declare load-file)

(defn -use-module [module & args]
  (let [file (-get-module module)
        yspath (get-yspath @sci/file)]
    (loop [[path & yspath] yspath]
      (let [path (str path "/" file)]
        (if (.exists (io/as-file path))
          (load-file path)
          (if (seq yspath)
            (recur yspath)
            (die (str "Module not found: " module))))))))

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

(defmacro use [module & args]
  `(let [module# (str (quote ~module))]
     (-use-module module# ~@args)))

(comment
  www
  )
