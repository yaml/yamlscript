;; This code is licensed under MIT license (See License for details)
;; Copyright 2023-2024 Ingy dot Net

(ns a0.patch-pprint
  (:require [clojure.pprint :as pprint]))

;; The code in this file was copied from
;; https://github.com/babashka/babashka/blob/7ecd2fe5/src/babashka/impl/pprint.clj#L7-L64

(defonce patched? (volatile! false))

(when-not @patched?
  (alter-var-root #'pprint/write-option-table
    (fn [m]
      (zipmap (keys m)
        (map find-var (vals m))))))

(def new-table-ize
  (fn [t m]
    (apply hash-map
      (mapcat
        #(when-let [v (get t (key %1))] [v (val %1)])
        m))))

(when-not @patched?
  (alter-var-root #'pprint/table-ize (constantly new-table-ize))
  (alter-meta! #'pprint/write-option-table dissoc :private)
  (alter-meta! #'pprint/with-pretty-writer dissoc :private)
  (alter-meta! #'pprint/pretty-writer? dissoc :private)
  (alter-meta! #'pprint/make-pretty-writer dissoc :private)
  (alter-meta! #'pprint/execute-format dissoc :private))

(def new-write
  (fn [object & kw-args]
    (let [options (merge {:stream true} (apply hash-map kw-args))]
      #_:clj-kondo/ignore
      (with-bindings (new-table-ize pprint/write-option-table options)
        (with-bindings
          (if (or (not (= pprint/*print-base* 10)) pprint/*print-radix*)
            {#'pr @#'pprint/pr-with-base} {})
          (let [optval (if (contains? options :stream)
                         (:stream options)
                         true)
                base-writer (condp = optval
                              nil (java.io.StringWriter.)
                              true *out*
                              optval)]
            (if pprint/*print-pretty*
              (pprint/with-pretty-writer base-writer
                (pprint/write-out object))
              (binding [*out* base-writer]
                (pr object)))
            (when (nil? optval)
              (.toString ^java.io.StringWriter base-writer))))))))

(when-not @patched?
  (alter-var-root #'pprint/write (constantly new-write)))

(comment
  )
