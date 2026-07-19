;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The clj-yaml (SnakeYAML) backend resolves at call time so that Clojure
;; runtimes without it (like jolt) can still load this namespace.

(ns ys.v0.yaml
  (:require
   [clojure.string :as str]
   [ys.v0.util :as util])
  (:refer-clojure :exclude [load]))

;; Alternate clj-yaml.core implementations (like jolt's libyaml based
;; one) may not support the option arguments; retry with just the data
;; (a genuine data error throws the same way on the retry).
(defn- parse-string [str & opts]
  (let [f (util/backend 'clj-yaml.core/parse-string)]
    (util/catching
      (apply f str opts)
      (f str))))

(defn- generate-string [data & opts]
  (let [f (util/backend 'clj-yaml.core/generate-string)]
    (util/catching
      (apply f data opts)
      (f data))))

;; Skip leading comment and blank lines before a --- document marker.
;; Spelled without (?x) so it also compiles on Go RE2 engines.
(def prefix-re
  #"^(?:(?:[ \t]*#.*\n)|(?:\s*\n))*---\s+")

(defn load [str]
  (parse-string str
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
  (generate-string
    data
    :dumper-options
    {:flow-style :block}))

(defn dump-all [data]
  (str/join "\n"
    (reduce
      (fn [strings node]
        (conj strings
          (let [yaml (str/trimr
                       (generate-string node
                         :dumper-options
                         {:flow-style :block}))]
            (if (> (count data) 1)
              (str "---\n" yaml)
              yaml))))
      [] data)))

(comment
  )
