;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.v0.reference-jvm
  (:refer-clojure :exclude [send])
  (:import
   [clojure.lang AMapEntry APersistentMap APersistentVector IObj IKVReduce]
   [java.util.concurrent LinkedBlockingQueue]))

(declare map-view vector-view)

(defn- entry [key value]
  (proxy [AMapEntry] []
    (key [] key)
    (val [] (value))
    (getKey [] key)
    (getValue [] (value))))

(defn map-view [source lookup metadata]
  (proxy [APersistentMap IObj IKVReduce] []
    (count [] (count source))
    (containsKey [key] (contains? source key))
    (entryAt [key]
      (when (contains? source key) (entry key #(lookup key))))
    (valAt
      ([key] (when (contains? source key) (lookup key)))
      ([key missing] (if (contains? source key) (lookup key) missing)))
    (seq [] (seq (map #(entry % (fn [] (lookup %))) (keys source))))
    (iterator [] (clojure.lang.SeqIterator.
                  (seq (map #(entry % (fn [] (lookup %))) (keys source)))))
    (assoc [key value]
      (map-view (assoc source key value)
                #(if (= key %) value (lookup %)) {}))
    (without [key]
      (map-view (dissoc source key) lookup {}))
    (empty [] (empty source))
    (kvreduce [f initial]
      (loop [keys (seq (keys source)) result initial]
        (if (empty? keys) result
            (let [key (first keys) result (f result key (lookup key))]
              (if (reduced? result) @result
                  (recur (next keys) result))))))
    (meta [] metadata)
    (withMeta [metadata] (map-view source lookup metadata))))

(defn vector-view [source lookup metadata]
  (proxy [APersistentVector IObj IKVReduce] []
    (count [] (count source))
    (nth
      ([index]
       (if (< -1 index (count source)) (lookup index)
           (throw (IndexOutOfBoundsException.))))
      ([index missing]
       (if (< -1 index (count source)) (lookup index) missing)))
    (assocN [index value]
      (vector-view (assoc source index value)
                   #(if (= index %) value (lookup %)) {}))
    (cons [value]
      (let [index (count source)]
        (vector-view (conj source value)
                     #(if (= index %) value (lookup %)) {})))
    (pop [] (vector-view (pop source) lookup {}))
    (empty [] [])
    (kvreduce [f initial]
      (loop [index 0 result initial]
        (if (= index (count source)) result
            (let [result (f result index (lookup index))]
              (if (reduced? result) @result
                  (recur (inc index) result))))))
    (meta [] metadata)
    (withMeta [metadata] (vector-view source lookup metadata))))

(defn queue [] (LinkedBlockingQueue.))
(defn send [^LinkedBlockingQueue queue value] (.put queue value))
(defn receive [^LinkedBlockingQueue queue] (.take queue))

(defn start [f]
  ;; Each evaluator can suspend inside an ordinary collection function.
  ;; Virtual threads preserve its stack without recursive evaluator calls.
  (let [convey (when-let [namespace (find-ns 'sci.impl.vars)]
                 (ns-resolve namespace 'binding-conveyor-fn))
        task (bound-fn [] (f))
        task (if convey (convey task) task)]
    (Thread/startVirtualThread (reify Runnable (run [_] (task))))))
