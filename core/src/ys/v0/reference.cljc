;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.v0.reference)

(def ^:private position-marker (gensym "reference-position"))

(defn mark [path value]
  (with-meta [path value] {position-marker true}))

(defn positions
  "Unwrap compiler markers and record evaluated sequence paths."
  [document]
  (letfn [(visit [value path locations]
            (cond
              (get (meta value) position-marker)
              (visit (second value) path (assoc locations (first value) path))
              (or (map? value) (vector? value))
              (reduce-kv
               (fn [[result locations] key child]
                 (let [[child locations]
                       (visit child (conj path key) locations)]
                   [(assoc result key child) locations]))
               [(empty value) locations] value)
              :else [value locations]))]
    (visit document [] {})))

(defn sibling
  "Read a sibling using the evaluated collection's insertion order."
  [collection key offset]
  (let [entries (if (map? collection) (vec (keys collection))
                    (vec (range (count collection))))
        position (first (keep-indexed #(when (= %2 key) %1) entries))
        target (when position (+ position offset))]
    (when (and target (<= 0 target) (< target (count entries)))
      (get collection (nth entries target)))))
