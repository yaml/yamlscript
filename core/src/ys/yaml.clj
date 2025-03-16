;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.yaml
  (:require
   [clojure.string :as str]
   [clj-yaml.core :as yaml])
  (:refer-clojure :exclude [load])
  #_(:import
   (java.util Optional)
   (org.snakeyaml.engine.v2.api DumpSettings)
   (org.snakeyaml.engine.v2.api.lowlevel Parse)
   ))

(def prefix-re
  #"(?x)^
    (?:
      (?:\s*\#.*\n)  # skip comments
      |
      (?:\s*\n)      # skip blank lines
    )*
    ---\s+")

(defn load [str]
  (yaml/parse-string str
    :code-point-limit (* 10 1024 1024)
    :keywords false))

(defn load-all [str]
  (let [str (str/replace str prefix-re "")
        documents (str/split str #"(?m)^---\s+")]
    (reduce
      (fn [data doc]
        (conj data (load doc)))
      []
      documents)))

(defn dump [data]
  (yaml/generate-string
    data
    :dumper-options
    {:flow-style :block}))

(defn dump-all [data]
  (str/join "\n"
    (reduce
      (fn [strings node]
        (conj strings
          (let [yaml (str/trimr
                       (yaml/generate-string node
                         :dumper-options
                         {:flow-style :block}))]
            (if (> (count data) 1)
              (str "---\n" yaml)
              yaml))))
      [] data)))

(comment
  )
