;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.module.csv
  (:require [clojure.string :as str])
  (:refer-clojure :exclude [load]))

(defn- parse-csv [text separator]
  (if (empty? text)
    []
    (loop [index 0 rows [] row [] field "" quoted false]
      (if (>= index (count text))
        (if (and (empty? row) (empty? field))
          rows
          (conj rows (conj row field)))
        (let [character (nth text index)
              next-character (when (< (inc index) (count text))
                               (nth text (inc index)))]
          (cond
            (and quoted (= character \"))
            (if (= next-character \")
              (recur (+ index 2) rows row (str field \") quoted)
              (recur (inc index) rows row field false))

            quoted
            (recur (inc index) rows row (str field character) quoted)

            (and (= character \") (empty? field))
            (recur (inc index) rows row field true)

            (= character separator)
            (recur (inc index) rows (conj row field) "" false)

            (= character \newline)
            (recur (inc index) (conj rows (conj row field)) [] "" false)

            (= character \return)
            (if (= next-character \newline)
              (recur (+ index 2) (conj rows (conj row field)) [] "" false)
              (recur (inc index) rows row field false))

            :else
            (recur (inc index) rows row (str field character) false)))))))

(defn read-csv [text]
  (parse-csv text \,))

(defn read-tsv [text]
  (parse-csv text \tab))

(defn- quote-cell [value separator]
  (let [text (if (nil? value) "" (str value))]
    (if (or (str/includes? text (str separator))
          (str/includes? text "\"")
          (str/includes? text "\n")
          (str/includes? text "\r"))
      (str "\"" (str/replace text "\"" "\"\"") "\"")
      text)))

(defn- write-delimited [data separator]
  (apply str
    (map #(str (str/join (str separator)
                 (map (fn [value] (quote-cell value separator)) %1))
            "\n")
      data)))

(defn write-csv [data]
  (write-delimited data \,))

(defn write-tsv [data]
  (write-delimited data \tab))

(def load read-csv)
(def dump write-csv)
