;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.v0.reference
  (:require
   [clojure.string :as str]
   [ys.v0.global :as global]
   #?@(:glj [] :default [[ys.v0.reference-jvm :as host]]))
  (:refer-clojure :exclude [send cycle]))

(def ^:private position-marker (gensym "reference-position"))

(defn collection-keys [value]
  #?(:glj (github.com:yaml:yamlscript:internal:goyamlparser.ReferenceKeys value)
     :default (keys value)))

(defn collection-vals [value]
  #?(:glj (github.com:yaml:yamlscript:internal:goyamlparser.ReferenceVals value)
     :default (vals value)))

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
  (let [entries (if (map? collection) (vec (collection-keys collection))
                    (vec (range (count collection))))
        position (first (keep-indexed #(when (= %2 key) %1) entries))
        target (when position (+ position offset))]
    (when (and target (<= 0 target) (< target (count entries)))
      (get collection (nth entries target)))))

(defn path-string
  ([path] (path-string nil path))
  ([document path]
   (let [escape
         (fn [key]
           (apply str
                  (map (fn [character]
                         (case character
                           \" "\\\"" \\ "\\\\" \/ "\\u002F"
                           \newline "\\n" \return "\\r" \tab "\\t"
                           \backspace "\\b" \formfeed "\\f"
                           (if (< (int character) 32)
                             (format "\\u%04x" (int character))
                             (str character)))) key)))
         component
         (fn [part]
           (if (integer? part) (str part)
               (let [escaped (escape part)]
                 (if (or (empty? part)
                         (re-matches #"[+-]?(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)(?:[eE][+-]?[0-9]+)?" part))
                   (str "'" escaped "'")
                   (if (or (str/starts-with? part "'")
                           (str/ends-with? part "'"))
                     (str/replace escaped "'" "\\u0027") escaped)))))]
     (str (when (some? document) (str "//" document))
          (when (or (nil? document) (seq path)) "/")
          (str/join "/" (map component path))))))

(def ^:dynamic *reader* nil)
(def ^:private link-marker (gensym "reference-link"))
(def ^:private node-marker ::node)

(defn- node [value] (get (meta value) node-marker))
(defn- link? [value] (get (meta value) link-marker))
(defn- cycle? [error]
  (loop [error error]
    (when error
      (or (when (::cycle (ex-data error)) (ex-data error))
          (recur (ex-cause error))))))

(defn- cycle [context path]
  (ex-info "Unresolvable circular reference"
           {::cycle true :context context :path path}))

(defn- queue []
  #?(:glj (github.com:yaml:yamlscript:internal:goyamlparser.ReferenceQueue)
     :default (host/queue)))
(defn- send [queue value]
  #?(:glj (github.com:yaml:yamlscript:internal:goyamlparser.ReferenceSend queue value)
     :default (host/send queue value)))
(defn- receive [queue]
  #?(:glj (github.com:yaml:yamlscript:internal:goyamlparser.ReferenceReceive queue)
     :default (host/receive queue)))
(defn- start [f]
  #?(:glj (github.com:yaml:yamlscript:internal:goyamlparser.ReferenceStart f)
     :default (host/start f)))

(declare resolve-node view schedule)

(defn- view [context path source]
  (if (or (node source) (link? source)) source
      (let [lookup #(resolve-node context (conj path %1) (get source %1))
            metadata (assoc (meta source) node-marker
                            {:context context :path path :source source})]
        (cond
          (map? source)
          #?(:glj (github.com:yaml:yamlscript:internal:goyamlparser.ReferenceMap
                   source lookup metadata)
             :default (host/map-view source lookup metadata))
          (vector? source)
          #?(:glj (github.com:yaml:yamlscript:internal:goyamlparser.ReferenceVector
                   source lookup metadata)
             :default (host/vector-view source lookup metadata))
          (sequential? source) (with-meta source metadata)
          :else source))))

(defn link [value path]
  (if-let [{:keys [context] prefix :path} (node value)]
    (with-meta [context (into prefix path)] {link-marker true})
    (if (empty? path) value
        (loop [value value [key & tail] path]
          (if-let [{:keys [context] prefix :path} (node value)]
            (with-meta [context (into prefix (cons key tail))]
              {link-marker true})
            (let [value (get value key)]
              (if (seq tail) (recur value tail) value)))))))

(defn sibling-link [collection key offset]
  (let [entries (if (map? collection) (vec (collection-keys collection))
                    (vec (range (count collection))))
        position (first (keep-indexed #(when (= %2 key) %1) entries))
        target (when position (+ position offset))]
    (when (and target (<= 0 target) (< target (count entries)))
      (link collection [(nth entries target)]))))

(defn- unwrap [{:keys [value error]}]
  (if error (throw error) value))

(defn- resolve-node [context path raw]
  (if *reader* (*reader* context path raw)
      (schedule {:operation :node :context context :path path :raw raw
                 :chain [] :links #{} :continuations [{:operation :finish}]})))

(defn- schedule [initial]
  (let [events (queue)]
    (loop [jobs [initial]]
      (let [job (if (seq jobs) (peek jobs) (receive events))
            jobs (if (seq jobs) (pop jobs) [])
            {:keys [operation context path raw value error chain links
                    continuations]} job]
        (case operation
          :done
          (let [waiting (:waiting (get @(:cache context) path))]
            (swap! (:cache context) assoc path
                   (select-keys job [:value :error]))
            (recur (into jobs
                         (map #(merge % (select-keys job [:value :error])
                                      {:operation :value}) waiting))))

          :node
          (let [id [(:id context) path]
                cached (get @(:cache context) path)
                task (get (:tasks context) path)]
            (cond
              (and cached (not (:waiting cached)))
              (recur
               (conj jobs
                     (merge job
                            (cond
                              (:cycle cached)
                              {:error (if (link? (:value cached))
                                        (apply cycle (:value cached))
                                        (cycle context path))}
                              (contains? cached :resolved)
                              {:value (:resolved cached)}
                              :else cached)
                            {:operation :value})))

              (and cached (some #{id} chain))
              (recur (conj jobs (assoc job :operation :value
                                       :error (cycle context path))))

              cached
              (do (swap! (:cache context) update-in [path :waiting] conj job)
                  (recur jobs))

              (nil? task)
              (recur (conj jobs (assoc job :operation :value :value raw)))

              (:alias task)
              (let [value ((:eval task) (view context [] (:base context)))]
                (swap! (:cache context) assoc path {:value value})
                (recur (conj jobs (assoc job :operation :value :value value))))

              :else
              (do
                (swap! (:cache context) assoc path {:waiting [job]})
                (start
                 (fn []
                   (binding
                    [*reader*
                     (fn [target target-path target-raw]
                       (let [reply (queue)]
                         (send events
                               {:operation :node :context target
                                :path target-path :raw target-raw
                                :chain (conj chain id) :links #{}
                                :continuations [{:operation :deliver
                                                 :reply reply}]})
                         (unwrap (receive reply))))]
                     (let [result
                           (try
                             {:value ((:eval task)
                                      (view context [] (:base context)))}
                             (catch #?(:glj go/any :default Throwable) error
                               {:error (if (cycle? error)
                                         (cycle context path) error)}))]
                       (send events
                             (merge result {:operation :done
                                            :context context :path path}))))))
                (recur jobs))))

          :value
          (if (and (not error) (link? value))
            (let [[target target-path] value
                  id [(:id target) target-path]
                  origin (or (:origin job) [target target-path])]
              (if (contains? links id)
                (do
                  (doseq [[target target-path] (:link-nodes job)]
                    (when (link? (:value (get @(:cache target) target-path)))
                      (swap! (:cache target) update target-path assoc :cycle true)))
                  (recur (conj jobs (assoc job :error (apply cycle origin)))))
                (recur
                 (conj jobs
                       (assoc job :value (view target [] (:base target))
                              :links (conj links id) :origin origin
                              :link-nodes (conj (vec (:link-nodes job)) value)
                              :continuations
                              (conj (conj continuations
                                          {:operation :resolved
                                           :context context :path path})
                                    {:operation :walk :keys target-path}))))))
            (let [value (when-not error (view context path value))
                  continuation (peek continuations)
                  continuations (pop continuations)]
              (case (:operation continuation)
                :finish (unwrap {:value value :error error})
                :deliver
                (do (send (:reply continuation) {:value value :error error})
                    (recur jobs))
                :resolved
                (do
                  (when-not error
                    (swap! (:cache (:context continuation))
                           update (:path continuation) assoc :resolved value))
                  (recur (conj jobs (assoc job :value value
                                           :continuations continuations))))
                :walk
                (if (or error (empty? (:keys continuation)))
                  (recur (conj jobs (assoc job :value value
                                           :continuations continuations)))
                  (let [[key & tail] (:keys continuation)
                        origin (node value)
                        child (get (or (:source origin) value) key)
                        next-job (if origin
                                   {:operation :node
                                    :context (:context origin)
                                    :path (conj (:path origin) key) :raw child}
                                   {:operation :value :value child})]
                    (recur
                     (conj jobs
                           (merge (dissoc job :value :error) next-job
                                  {:continuations
                                   (conj continuations
                                         {:operation :walk :keys tail})})))))))))))))

(defn document [base tasks]
  (let [context {:id (gensym "document") :base base
                 :tasks (into {} (map #(vector (:path %) %) tasks))
                 :cache (atom {}) :number (atom nil)}
        root (view context [] base)]
    (doseq [{:keys [path]} tasks]
      (try (resolve-node context path (get-in base path))
           (catch #?(:glj go/any :default Throwable) error
             (when-not (cycle? error) (throw error)))))
    (try (resolve-node context [] base)
         (catch #?(:glj go/any :default Throwable) error
           (if (cycle? error) (link root []) (throw error))))))

(defn register-document [value number]
  (when-let [context (if (link? value) (first value) (:context (node value)))]
    (when (nil? @(:number context)) (reset! (:number context) number)))
  value)

(defn serialize
  ([value] (serialize value nil))
  ([value document]
   (let [current (or document (dec (count @global/stream-values)))
         marker (fn [context path]
                  {"*" (path-string
                        (let [number @(:number context)]
                          (when (and (some? number) (not= current number)) number))
                        path)})]
     (loop [jobs [{:fetch (fn [] value) :active #{}}] results []]
       (if (empty? jobs) (peek results)
           (let [{:keys [fetch active kind size keys blank]} (peek jobs)
                 jobs (pop jobs)]
             (if kind
               (let [start (- (count results) size)
                     children (subvec results start)
                     value (case kind
                             :map (into blank (map vector keys children))
                             :vector (vec children)
                             :seq (apply list children))]
                 (recur jobs (conj (subvec results 0 start) value)))
               (let [value
                     (try (let [value (fetch)]
                            (if (link? value)
                              (schedule {:operation :value :value value
                                         :context (first value) :path (second value)
                                         :chain [] :links #{}
                                         :continuations [{:operation :finish}]})
                              value))
                          (catch #?(:glj go/any :default Throwable) error
                            (if-let [{:keys [context path]} (cycle? error)]
                              (marker context path) (throw error))))
                     origin (node value)
                     id (when origin [(:id (:context origin)) (:path origin)])
                     circular? (and id (contains? active id))
                     active (if id (conj active id) active)
                     kind (when-not circular?
                            (cond (map? value) :map
                                  (vector? value) :vector
                                  (sequential? value) :seq))]
                 (if-not kind
                   (recur jobs
                          (conj results
                                (if circular?
                                  (marker (:context origin) (:path origin)) value)))
                   (let [keys (when (= kind :map) (vec (collection-keys value)))
                         children
                         (case kind
                           :map (mapv (fn [key] {:fetch #(get value key)
                                                 :active active}) keys)
                           :vector (mapv (fn [index] {:fetch #(nth value index)
                                                      :active active})
                                         (range (count value)))
                           :seq (mapv (fn [child] {:fetch (fn [] child)
                                                   :active active}) value))
                         assembly {:kind kind :size (count children) :keys keys
                                   :blank (when (= kind :map) (empty value))}]
                     (recur (into (conj jobs assembly) (reverse children))
                            results)))))))))))
