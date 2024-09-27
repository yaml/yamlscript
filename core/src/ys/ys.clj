;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.ys
  (:require
   [babashka.pods.sci :as pods]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [sci.core :as sci]
   [yamlscript.common :refer [abspath dirname get-yspath]]
   [yamlscript.compiler]
   [yamlscript.re :as re])
  (:refer-clojure
   :exclude [compile
             eval
             for
             load-file
             use
             when]))


;;-----------------------------------------------------------------------------
(def sci-ctx (atom nil))

(def pods (atom []))

(def FILE (sci/new-dynamic-var 'FILE nil))


;;-----------------------------------------------------------------------------
(defn -get-module [module]
  (clojure.core/when (not (re-matches (re/re #"(?:$nspc|$symw)") module))
    (die (str "Invalid module name: " module)))
  (let [module (str/replace module #"\." "/")]
    (str module ".ys")))

(defn -clj-load-file [clj-file]
  (let [clj-file (abspath clj-file (dirname @sci/file))
        clj-code (->
                   clj-file
                   slurp)
        ret (sci/binding
             [sci/file clj-file
              FILE clj-file]
              (sci/eval-string+ @sci-ctx clj-code))]
    (:val ret)))

(declare load-file)

(defn -use-module [module & _args]
  (let [file (-get-module module)
        clj-file (str/replace file #"\.ys$" ".clj")
        yspath (get-yspath @sci/file)]
    (loop [[path & yspath] yspath]
      (let [ys-path (str path "/" file)
            clj-path (str path "/" clj-file)]
        (if (.exists (io/as-file ys-path))
          (load-file ys-path)
          (if (.exists (io/as-file clj-path))
            (-clj-load-file clj-path)
            (if (seq yspath)
              (recur yspath)
              (die (str "Module not found: " module)))))))))


;;-----------------------------------------------------------------------------
(defn compile [code]
  (yamlscript.compiler/compile code))

(defn eval [ys-code]
  (let [clj-code (yamlscript.compiler/compile ys-code)
        ret (sci/binding
             [sci/file "EVAL"
              FILE "EVAL"]
              (sci/eval-string+ @sci-ctx clj-code))]
    (:val ret)))

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

(defn load-pod [& args]
  (let [pod (apply pods/load-pod @sci-ctx args)]
    (swap! pods conj pod)))

(defn unload-pods []
  (doseq [pod @pods]
    (pods/unload-pod pod))
  (reset! pods []))

(defmacro use [module & args]
  `(let [module# (str (quote ~module))]
     (-use-module module# ~@args)))


;;-----------------------------------------------------------------------------
(defmacro for [bindings & body]
  `(do
     (doall (clojure.core/for [~@bindings] (do ~@body)))
     nil))

(defn if [cond then else]
  (if cond then else))

(defn when [cond then]
  (clojure.core/when cond then))

(comment
  )
