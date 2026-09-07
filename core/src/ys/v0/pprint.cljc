;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; Portable pretty printing for the ys::pprint public module.

(ns ys.v0.pprint
  (:require
   [clojure.string :as str]
   [ys.v0.global :as global]))

(def ^:dynamic *right-margin* 72)
(def ^:dynamic *miser-width* 40)

(def ^:private option-names
  #{:length :level :miser-width :pretty :right-margin :stream
    :suppress-namespaces})

(defn- spaces [n]
  (apply str (repeat n " ")))

(defn- scalar-str [value suppress-namespaces]
  (if (and suppress-namespaces (symbol? value) (namespace value))
    (name value)
    (pr-str value)))

(declare render)

(defn- limited-items [value length]
  (let [items (if length (take (inc length) value) value)]
    (if (and length (> (count items) length))
      [(take length items) true]
      [items false])))

(defn- flat-str [value options depth]
  (let [level (:level options)]
    (if (and level (>= depth level))
      "#"
      (cond
        (map? value)
        (let [[items more?] (limited-items value (:length options))
              parts (map (fn [[key item]]
                           (str (flat-str key options (inc depth)) " "
                             (flat-str item options (inc depth))))
                      items)]
          (str "{" (str/join ", "
                     (cond-> (vec parts) more? (conj "..."))) "}"))

        (vector? value)
        (let [[items more?] (limited-items value (:length options))]
          (str "[" (str/join " "
                     (cond-> (mapv #(flat-str %1 options (inc depth))
                               items)
                       more? (conj "..."))) "]"))

        (set? value)
        (let [[items more?] (limited-items value (:length options))]
          (str "#{" (str/join " "
                      (cond-> (mapv #(flat-str %1 options (inc depth))
                                items)
                        more? (conj "..."))) "}"))

        (seq? value)
        (let [[items more?] (limited-items value (:length options))]
          (str "(" (str/join " "
                     (cond-> (mapv #(flat-str %1 options (inc depth))
                               items)
                       more? (conj "..."))) ")"))

        :else
        (scalar-str value (:suppress-namespaces options))))))

(defn- render-seq [value column depth options open close]
  (let [[items more?] (limited-items value (:length options))
        items (cond-> (vec items) more? (conj '...))
        indent (inc column)]
    (if (empty? items)
      (str open close)
      (str open
        (render (first items) indent (inc depth) options)
        (apply str
          (map #(str "\n" (spaces indent)
                  (render %1 indent (inc depth) options))
            (rest items)))
        close))))

(defn- render-map [value column depth options]
  (let [[items more?] (limited-items value (:length options))
        entries
        (mapv
          (fn [[key item]]
            (let [key-text (render key (inc column) (inc depth) options)
                  value-column (+ column 2 (count key-text))]
              (str key-text " "
                (render item value-column (inc depth) options))))
          items)
        entries (cond-> entries more? (conj "..."))
        indent (inc column)]
    (if (empty? entries)
      "{}"
      (str "{" (first entries)
        (apply str
          (map #(str ",\n" (spaces indent) %1) (rest entries)))
        "}"))))

(defn- render [value column depth options]
  (let [flat (flat-str value options depth)
        margin (:right-margin options)]
    (if (or (not (:pretty options))
          (<= (+ column (count flat)) margin)
          (not (coll? value)))
      flat
      (cond
        (map? value) (render-map value column depth options)
        (vector? value) (render-seq value column depth options "[" "]")
        (set? value) (render-seq value (inc column) depth options "#{" "}")
        (seq? value) (render-seq value column depth options "(" ")")
        :else flat))))

(defn- parse-options [args]
  (when (odd? (count args))
    (throw (ex-info "ys::pprint/write requires key/value options" {})))
  (let [options (apply hash-map args)
        invalid (seq (remove option-names (keys options)))]
    (when invalid
      (throw
        (ex-info
          (str "Invalid ys::pprint/write option: " (first invalid)) {})))
    (merge {:length nil
            :level nil
            :miser-width *miser-width*
            :pretty true
            :right-margin *right-margin*
            :stream true
            :suppress-namespaces false}
      options)))

(defn write [value & args]
  (let [options (parse-options args)
        text (render value 0 0 options)
        stream (:stream options)]
    (if (nil? stream)
      text
      (binding [*out* (if (= true stream) *out* stream)]
        (print text)
        nil))))

(defn pp
  ([value]
   (binding [*out* (global/current-output)]
     (print (write value :stream nil))
     (println)))
  ([value writer]
   (binding [*out* writer]
     (print (write value :stream nil))
     (println))))

(comment
  )
