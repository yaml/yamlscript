;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.module.http
  (:require
   [ys.v0.ext :as ext]
   [ys.v0.http :as http]
   [ys.v0.util :as util])
  (:refer-clojure :exclude [get]))

(defn delete [& args] (apply http/delete args))
(defn get [& args] (apply http/get args))
(defn head [& args] (apply http/head args))
(defn options [& args] (apply http/options args))
(defn patch [& args] (apply http/patch args))
(defn post [& args] (apply http/post args))
(defn put [& args] (apply http/put args))

(defn curl [url]
  (let [response (get (ext/convert-url url))]
    (if-let [body (:body response)]
      (str body)
      (util/die response))))
