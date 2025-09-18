(ns program
  (:gen-class)
  (:refer-clojure
   :exclude
   [atom eval print read replace reverse set])
  (:require
   [ys.std :refer :all]
   [ys.dwim :refer :all]
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
