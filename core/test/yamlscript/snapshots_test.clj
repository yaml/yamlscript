;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.snapshots-test
  (:require
   [clj-yaml.core :as yaml]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [clojure.walk :as walk]
   [yamlscript.compiler :as compiler]
   [yamlscript.constructor :as constructor]
   [yamlscript.global :as compiler-global]
   [yamlscript.parser :as parser]
   [yamlscript.runtime :as runtime]
   [ys.v0.global :as global]))

(def fixtures
  (yaml/parse-string (slurp "test/snapshots.yaml") :keywords true))

(defn reset-stream! []
  (reset! global/stream-values [])
  (reset! global/stream-anchors_ {})
  (reset! global/doc-anchors_ {}))

(deftest snapshot-fixtures
  (doseq [{:keys [name ys want error clojure]} fixtures]
    (testing name
      (reset-stream!)
      (if error
        (is (= error
               (try (compiler/compile ys) nil
                    (catch Exception e
                      (str/trim-newline (:cause (Throwable->map e)))))))
        (do
          (is (= (walk/stringify-keys want)
                 (runtime/eval-string (compiler/compile ys))))
          (when clojure
            (is (= (read-string (str "[" clojure "]"))
                   (read-string
                    (str "["
                         (with-redefs [compiler-global/opts
                                       (atom (dissoc @compiler-global/opts
                                                     :unordered))]
                           (binding [constructor/no-wrap true]
                             (compiler/compile ys)))
                         "]"))))))))))

(deftest snapshot-source-marks-and-anchor-tracking
  (let [events (parser/parse "!ys-0:\nvalue:: *~.seed\nseed: 7\n")
        value (first (filter #(= "~.seed" (:* %)) events))]
    (is (= [1 8 15] (:< (meta value))))
    (is (= [1 15 22] (:> (meta value)))))
  (is (= #{"base" "~base"}
         (compiler/used-anchor-names [{:* "~base"}]))))

(defn native-result [binary option source]
  (let [command [binary option "-"]
        process (.start (ProcessBuilder. ^java.util.List command))]
    (with-open [writer (io/writer (.getOutputStream process))]
      (.write writer ^String source))
    (when-not (.waitFor process 20 java.util.concurrent.TimeUnit/SECONDS)
      (.destroyForcibly process)
      (throw (ex-info "Native snapshot test timed out" {})))
    {:exit (.exitValue process)
     :out (slurp (.getInputStream process))
     :err (slurp (.getErrorStream process))}))

(deftest native-snapshot-fixtures
  (when-let [binary (System/getenv "YS_SNAPSHOT_TEST_BIN")]
    (doseq [{:keys [name ys want error clojure]} fixtures]
      (testing name
        (let [{:keys [exit out err]} (native-result binary "-l" ys)]
          (if error
            (do (is (not (zero? exit)))
                (is (str/includes? err error) err))
            (do (is (zero? exit) err)
                (is (= (json/read-str (json/write-str want))
                       (json/read-str out)))))
          (when clojure
            (let [{:keys [exit out err]} (native-result binary "-c" ys)]
              (is (zero? exit) err)
              (is (= (read-string (str "[" clojure "]"))
                     (read-string (str "[" out "]")))))))))))
