;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The ys.v0.ys namespace is the standard library's bridge to the YS
;; compiler and runtime. Under the ys runtime the real implementations are
;; installed into the hooks below. Under plain Clojure runtimes (babashka,
;; JVM Clojure) the compiler-dependent functions die with a clear message,
;; while the portable ones use plain Clojure implementations.

(ns ys.v0.ys
  (:require
   [clojure.string :as str]
   [ys.v0.re :as re]
   [ys.v0.util :as util])
  (:refer-clojure
   :exclude [compile
             eval
             load-file
             use]))

(def hooks
  "Implementation hooks installed by the ys runtime."
  (atom {}))

(defn- hook [key & args]
  (if-let [f (get @hooks key)]
    (apply f args)
    (util/die
      (str "ys/" (name key) " requires the ys runtime "
        "(not available in this Clojure runtime)"))))

(defn compile [code]
  (hook :compile code))

(defn eval
  ([ys-code] (hook :eval ys-code))
  ([ys-code file stream-mode] (hook :eval ys-code file stream-mode)))

(defn eval-stream [ys-code]
  (hook :eval-stream ys-code))

(defn load-file [ys-file]
  (hook :load-file ys-file))

(defn load-url [url]
  (hook :load-url url))

(defn- default-load-pod [args]
  (let [load-pod (requiring-resolve 'babashka.pods/load-pod)]
    (apply load-pod args)))

(defn load-pod [& args]
  (if-let [f (get @hooks :load-pod)]
    (f args)
    (default-load-pod args)))

(defn unload-pods []
  (when-let [f (get @hooks :unload-pods)]
    (f)))

(defn- default-def-vars [ns m force]
  (let [ns (condf ns
             #(instance? clojure.lang.Namespace %1) ns
             string? (create-ns (symbol ns))
             symbol? (create-ns ns)
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
          (when (and (not force)
                  (ns-resolve ns key))
            (util/die
              (str "Variable '" key
                "' already defined in namespace '" (ns-name ns) "'")))
          (intern ns key v)))
      nil m)
    nil))

(defn +def-vars
  ([ns m]
   (+def-vars ns m false))
  ([ns m force]
   (let [m (dissoc m "_" '_)]
     (if-let [f (get @hooks :+def-vars)]
       (f ns m force)
       (default-def-vars ns m force)))))

(defmacro def-vars-from-map [ns m]
  `(let [[m# ns#] (if (~m "+")
                    [(dissoc ~m "+") (symbol (~m "+"))]
                    [~m ~ns])]
     (+def-vars ns# m#)))

(defn +use [ns forms]
  (hook :+use ns forms))

(defmacro use [& forms]
  `(+use *ns* '~forms))

(comment
  )
