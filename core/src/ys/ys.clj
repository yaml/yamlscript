;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.ys
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [sci.core :as sci]
   [yamlscript.common :refer [abspath dirname]]
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
              key (if (> (count key) 1)
                    (str/replace key #"_" "-")
                    key)
              _ (when-not (or (re-matches re/symw key) (=  key "_"))
                  (util/die (str "Invalid key for set-vars: '" key "'")))
              key (symbol key)]
          (global/intern ns key v)))
      nil m)
    nil))

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
  (let [ys-file (abspath ys-file (dirname @sci/file))]
    (if (.exists (io/as-file ys-file))
      (externals/load-file-ys ys-file)
      (die (str "File not found: " ys-file)))))

(defn load-pod [& args]
  (externals/load-pod args))

(defn unload-pods []
  (externals/unload-pods))

(defn +use [ns forms]
  (when (not (seq forms))
    (die "use requires at least one form"))
  (let [forms (if (symbol? (first forms))
                (list forms)
                forms)]
    (reduce (fn [_ form]
              (let [module (first form)
                    args (rest form)]
                (externals/use-module ns module args)))
      nil forms))
  nil)

(defmacro use [& forms]
  `(+use *ns* '~forms))

(comment
  )
