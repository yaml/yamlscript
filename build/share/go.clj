(ns main (:require [github.com:yaml:yamlscript:glj:ys:v0 :refer :all]))

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
