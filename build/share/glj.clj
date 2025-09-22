(ns main
  (:gen-class)
  (:refer-clojure
   :exclude
   [atom eval print read replace reverse set])
  (:require
   [ys.std :refer :all]
   [ys.dwim :refer :all]
   [ys.csv :as csv]
   [ys.fs :as fs]
   [ys.http :as http]
   [ys.json :as json]
   [ys.yaml :as yaml]
   [yamlscript.debug :refer :all]))

(defn -parse-argv [argv]
  (mapv #(if (re-matches #"-?\d+" %) (parse-long %) %) argv))

(def ^:dynamic ARGV [])
(def ^:dynamic ARGS [])
(def ^:dynamic ENV {})

;; ------------------------------------------------------------------------

;;CODE;;

;; ------------------------------------------------------------------------

(defn -main [& argv]
  (let [args (-parse-argv argv)]
    (alter-var-root #'ARGV (constantly argv))
    (alter-var-root #'ARGS (constantly args))
    (alter-var-root #'ENV (constantly (into {} (System/getenv))))
    (apply main ARGS)))
