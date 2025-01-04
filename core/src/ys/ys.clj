;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.ys
  (:require
   [babashka.pods.sci :as pods]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [sci.core :as sci]
   [yamlscript.common :refer [abspath dirname get-yspath]]
   [yamlscript.compiler]
   [yamlscript.global :as global]
   [yamlscript.re :as re]
   [yamlscript.util :as util])
  (:refer-clojure
   :exclude [compile
             eval
             for
             load-file
             use
             when]))

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
              global/FILE clj-file]
              (sci/eval-string+ @global/sci-ctx clj-code))]
    (:val ret)))


;;-----------------------------------------------------------------------------
(defn compile [code]
  (yamlscript.compiler/compile code))

(defn +def-vars [ns m]
  (let [ns
        (condf ns
          #(= (type %1) clojure.lang.Namespace) ns
          #(= (type %1) sci.lang.Namespace) ns
          string? (global/create-ns (symbol ns))
          symbol? (global/create-ns ns)
          (util/die (str "Invalid namespace for set-vars: '" ns "'")))]
    (reduce-kv
      (fn [_ k v]
        (let [key (condf k
                    string? k
                    keyword? (name k)
                    symbol? (name k)
                    (util/die (str "Invalid key for set-vars: '" k "'")))
              key (str/replace key #"_" "-")
              _ (when-not (re-matches re/symw key)
                  (util/die (str "Invalid key for set-vars: '" key "'")))
              key (symbol key)]
          (global/intern ns key v)))
      nil m)))

(defmacro def-vars-from-map [ns m]
  `(let [[m# ns#] (if (~m "+")
                    [(dissoc ~m "+") (symbol (~m "+"))]
                    [~m ~ns])]
     (+def-vars ns# m#)))

(defn eval- [ys-code file stream-mode]
  (let [stream @global/stream-values
        _ (reset! global/stream-values [])
        clj-code (ys.ys/compile ys-code)
        value (sci/binding
               [sci/file file
                global/FILE file]
                (sci/eval-string+ @global/sci-ctx clj-code))
        value (if stream-mode
                @global/stream-values
                (:val value))
        _ (reset! global/stream-values stream)]
    value))

(defn eval [ys-code] (ys.ys/eval- ys-code "EVAL" false))
(defn eval-stream [ys-code] (ys.ys/eval- ys-code "EVAL" true))

;; XXX This should work but doesn't.
#_(defn load-file [ys-file]
  (let [ys-file (abspath ys-file (dirname @sci/file))
        clj-code (-> ys-file slurp ys.ys/compile)]
    (ys.ys/eval- clj-code ys-file)))

(defn load-file [ys-file]
  (let [ys-file (abspath ys-file (dirname @sci/file))
        clj-code (-> ys-file slurp ys.ys/compile)

        ;; XXX Duplicate logic from eval-
        stream @global/stream-values
        _ (reset! global/stream-values [])
        ret (sci/binding
             [sci/file ys-file
              global/FILE ys-file]
              (sci/eval-string+ @global/sci-ctx clj-code))
        _ (reset! global/stream-values stream)]
    (:val ret)))

(defn load-pod [& args]
  (let [pod (apply pods/load-pod @global/sci-ctx args)]
    (swap! global/pods conj pod)))

(defn unload-pods []
  (doseq [pod @global/pods]
    (pods/unload-pod pod))
  (reset! global/pods []))

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
