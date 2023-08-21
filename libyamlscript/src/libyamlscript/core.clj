;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; YAMLScript is a programming language that is hosted by Clojure platforms.
;; It can be used to add scripting to abilities to YAML files.
;;
;; This library reads YAMLScript code and converts it into Clojure code.

(ns libyamlscript.core
  (:require
   [yamlscript.core :as ys]
   [clojure.data.json :as json]
   [sci.core :as sci])
  (:gen-class
   :methods [^{:static true} [compileYsToClj [String] String]
             ^{:static true} [evalYsToJson [String] String]]))

(defn -compileYsToClj
  "Compile a YAMLScript code string to a Clojure code string."
  [^String ys-str]

  (try
    (->> ys-str
         ys/compile
         (assoc {} :clojure)
         json/write-str)

    (catch Exception e
      ;; XXX throw errors for now
      (throw e)

      (json/write-str
       {:error (str (type e))
        :message (.getMessage e)}))))

(defn -evalYsToJson
  "Convert a YAMLScript code string to Clojure, eval the Clojure code with
  SCI, encode the resulting value as JSON and return the JSON string."
  [^String ys-str]
  (sci/binding [sci/out *out*]
    (try
      (->> ys-str
           ys/compile
           sci/eval-string
           json/write-str)

      (catch Exception e
        ;; XXX throw errors for now
        (throw e)

        (json/write-str
         {:error (str (type e))
          :message (.getMessage e)})))))
