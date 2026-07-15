;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.v0.ext
  (:require
   [clojure.string :as str]
   [babashka.process :as process]
   [ys.v0.util :as util]
   [ys.v0.yaml :as yaml]))

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

(defn github-raw-url
  "Convert a raw url shorthand into a raw URL."
  [url]
  (let [[path ref] (str/split url #"\@")
        ref (or ref "HEAD")
        [user repo path] (str/split path #"/" 3)
        _ (when-not (and user repo path)
            (util/die (str "Invalid github url: " url)))]
    (str/join "/" ["https://raw.githubusercontent.com"
                   user repo ref path])))

(defn github-gist-url
  "Convert a gist url shorthand into a raw URL."
  [url]
  (let [url (str/replace url #"/raw/?" "/")
        [path ref] (str/split url #"\@")
        [user gist-id path] (str/split path #"/" 3)
        _ (when-not (and user gist-id)
            (util/die (str "Invalid github gist url: " url)))]
    (str/join "/" (remove nil? ["https://gist.githubusercontent.com"
                                user gist-id "raw" ref path]))))

(defn convert-url
  "Convert url into its canonical form."
  [url]
  (cond
    (re-find #"^https?://" url) url
    (str/starts-with? url "gist:") (github-gist-url (subs url 5))
    (str/starts-with? url "github:") (github-raw-url (subs url 7))
    (str/starts-with? url "https:") url
    (str/starts-with? url "http:") url
    (not (str/includes? url ":")) (str "https://" url)
    :else (util/die (str "Invalid url for ':url': " url))))

(comment
  )
