;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; This library compiles into the `libyamlscript.so` shared library.

(ns libyamlscript.core
  (:require
   [yamlscript.debug]
   [yamlscript.runtime :as runtime]
   [yamlscript.compiler :as compiler]
   [clojure.data.json :as json]
   [sci.core :as sci])
  (:gen-class
   :methods [^:static [loadYsToJson [String] String]]))

(declare json-write-str error-map debug)

(defn -loadYsToJson
  "Convert a YAMLScript code string to Clojure, eval the Clojure code with
  SCI, encode the resulting value as JSON and return the JSON string."
  [^String ys-str]
  (debug "CLJ libyamlscript load - input string:" ys-str)
  (let [resp (sci/binding [sci/out *out*]
               (try
                 (->> ys-str
                   compiler/compile
                   runtime/eval-string
                   (assoc {} :data)
                   json-write-str)

                 (catch Exception e
                   (-> e
                     error-map
                     json-write-str))))]
    (debug "CLJ libyamlscript load - response string:" resp)
    resp))

(defn json-write-str [data]
  (json/write-str
    data
    {:escape-unicode false
     :escape-js-separators false
     :escape-slash false}))

(defn error-map [e]
  (let [err (Throwable->map e)]
    {:error {:cause (:cause err)
             :type (get-in err [:via 0 :type])
             :trace (get-in err [:trace])}}))

(defn debug [& msg]
  (when (System/getenv "LIBYAMLSCRIPT_DEBUG")
    (binding [*out* *err*]
      (apply println msg))))

(comment
  (-loadYsToJson "!yamlscript/v0/data\nsay: 42")
  )
