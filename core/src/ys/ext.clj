;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.ext
  (:require
   [clojure.string :as str]
   [babashka.process :as process]
   [yamlscript.util :as util]
   [ys.yaml :as yaml]))

(defn github-raw-url [spec]
  (let [[owner repo ref & path] (str/split spec #"/")]
    (format
      "https://raw.githubusercontent.com/%s/%s/refs/heads/%s/%s"
      owner repo ref (str/join "/" path))))

(defn yq [data cmd]
  (let [yaml (yaml/dump data)
        res (process/sh {:in yaml}
              "yq" "-e" cmd)
        {:keys [exit out err]} res]
    (when (and
            (not= 0 exit)
            (not= err "Error: no matches found\n"))
      (util/die "yq error: " (:err res)))

    (let [data (if (str/blank? out)
                 nil
                 (yaml/load-all out))]
      (if (= 1 (count data))
        (first data)
        data))))

(comment
  )
