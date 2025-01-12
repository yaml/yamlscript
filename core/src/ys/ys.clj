;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.ys
  (:require
   [clojure.string :as str]
   [sci.core :as sci]
   [yamlscript.compiler]
   [yamlscript.externals :as externals]
   [yamlscript.global :as global]
   [yamlscript.re :as re]
   [yamlscript.util :as util])
  (:refer-clojure
   :exclude [compile
             eval
             load-file
             use]))

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

(defn eval
  ([ys-code] (ys.ys/eval ys-code "EVAL" false))
  ([ys-code file stream-mode]
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
     value)))

(defn eval-stream [ys-code]
  (ys.ys/eval ys-code "EVAL" true))

(defn load-file [ys-file]
  (externals/load-file-ys ys-file))

(defn load-pod [& args]
  (externals/load-pod args))

(defn unload-pods []
  (externals/unload-pods))

(defn +use [module & args]
  (externals/use-module module args))

(defmacro use [module & args]
  `(let [module# (str (quote ~module))]
     (+use module# ~@args)))

(comment
  )
