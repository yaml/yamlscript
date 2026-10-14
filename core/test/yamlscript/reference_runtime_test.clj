;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.reference-runtime-test
  (:require
   [clj-yaml.core :as yaml]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [yamlscript.compiler :as compiler]
   [yamlscript.runtime :as runtime]
   [ys.v0.global :as global]
   [ys.v0.reference :as reference]
   [ys.v0.reference-jvm :as host])
  (:import [java.util.concurrent TimeUnit]))

(defn fixtures [file]
  (yaml/parse-string (slurp (str "test/proposals/" file)) :keywords false))

(deftest reference-path-spellings
  (doseq [case (fixtures "reference-paths.yaml")]
    (testing (get case "name")
      (let [path (reference/path-string (get case "document") (get case "path"))]
        (is (= (get case "path-string") path))
        (is (= (get case "serialized") {"*" path}))))))

(deftest runtime-references
  (doseq [case (fixtures "runtime-references.yaml")]
    (testing (get case "name")
      (reset! global/stream-values [])
      (reset! global/stream-anchors_ {})
      (reset! global/doc-anchors_ {})
      (let [run #(-> (get case "ys") compiler/compile runtime/eval-string
                     reference/serialize)]
        (if-let [error (get case "error")]
          (is (try (run) false
                   (catch Exception exception
                     (str/includes? (ex-message exception)
                                    (get error "contains")))))
          (let [writer (java.io.StringWriter.)
                result (binding [*out* writer] (run))
                output (str writer)]
            (is (= (get case "serialized") result))
            (when (contains? case "stdout")
              (is (= (get case "stdout") output)))
            (doseq [part (get case "stdout-contains")]
              (is (str/includes? output part)))))))))

(deftest long-runtime-alias-chain
  (let [size 10000
        base (into {} (map #(vector (str %) nil) (range size)))
        tasks (mapv (fn [index]
                      {:path [(str index)] :alias true
                       :eval (if (= index (dec size)) (constantly 42)
                                 #(reference/link % [(str (inc index))]))})
                    (range size))
        doc (reference/document base tasks)]
    (is (= 42 (get doc "0")))
    (is (= size (count (reference/serialize doc))))))

(deftest circular-compiler-fixtures
  (doseq [case (yaml/parse-string (slurp "test/compiler.yaml") :keywords false)
          :when (contains? case "runtime-serialized")]
    (testing (get case "name")
      (reset! global/stream-values [])
      (let [code (compiler/compile (get case "yamlscript"))]
        (is (str/includes? code "reference-document"))
        (is (= (get case "runtime-serialized")
               (reference/serialize (runtime/eval-string code))))))))

(deftest long-runtime-computation-chain
  (let [size 2000
        base (into {} (map #(vector (str %) nil) (range size)))
        tasks (mapv (fn [index]
                      {:path [(str index)]
                       :eval (if (= index (dec size)) (constantly 1)
                                 #(inc (get % (str (inc index)))))})
                    (range size))
        doc (reference/document base tasks)]
    (is (= size (get doc "0")))))

(deftest deeply-linked-collections-serialize-iteratively
  (let [size 2000
        base (into {} (map #(vector (str %) {"next" nil}) (range size)))
        tasks (mapv (fn [index]
                      {:path [(str index) "next"] :alias true
                       :eval #(reference/link % [(str (mod (inc index) size))])})
                    (range size))
        doc (reference/document base tasks)
        result (reference/serialize (get doc "0"))]
    (is (= {"*" "/'0'"}
           (loop [index 0 value result]
             (if (= index size) value
                 (recur (inc index) (get value "next"))))))))

(defn run-binary [binary options source]
  (let [child (.start (ProcessBuilder. ^java.util.List (into [binary] options)))
        out (host/queue) err (host/queue)]
    (host/start #(host/send out (slurp (.getInputStream child) :encoding "UTF-8")))
    (host/start #(host/send err (slurp (.getErrorStream child) :encoding "UTF-8")))
    (with-open [writer (io/writer (.getOutputStream child) :encoding "UTF-8")]
      (.write writer ^String source))
    (when-not (.waitFor child 20 TimeUnit/SECONDS)
      (.destroyForcibly child)
      (throw (ex-info "Reference fixture timed out" {:source source})))
    {:exit (.exitValue child) :out (host/receive out) :err (host/receive err)}))

(deftest native-reference-fixtures
  (when-let [binary (System/getenv "YS_REFERENCE_TEST_BIN")]
    (doseq [case (fixtures "runtime-references.yaml")]
      (testing (get case "name")
        (let [result (run-binary binary ["-l" "-"] (get case "ys"))]
          (if-let [error (get case "error")]
            (is (and (not (zero? (:exit result)))
                     (str/includes? (:err result) (get error "contains"))))
            (do
              (is (zero? (:exit result)) (:err result))
              (when (zero? (:exit result))
                (let [lines (str/split-lines (:out result))
                      output (str/join "\n" (butlast lines))]
                  (is (= (get case "serialized") (json/read-str (last lines))))
                  (when (contains? case "stdout")
                    (is (= (get case "stdout") (str output "\n"))))
                  (doseq [part (get case "stdout-contains")]
                    (is (str/includes? output part))))))))))
    (doseq [case (fixtures "reference-paths.yaml")]
      (testing (get case "name")
        (let [source (str "(ys.v0.util/reference-path-string "
                          (pr-str (get case "document")) " "
                          (pr-str (vec (get case "path"))) ")")
              result (run-binary binary ["-C" "-l" "-"] source)]
          (is (zero? (:exit result)) (:err result))
          (when (zero? (:exit result))
            (is (= (get case "path-string") (json/read-str (:out result))))))))))

(deftest serializing-stream-documents-uses-each-document-context
  (let [case (first (filter #(= (get % "name")
                                "Local and completed-document references keep distinct prefixes")
                            (fixtures "runtime-references.yaml")))
        expected [{"foo" {"self" {"*" "/foo"}}} (get case "serialized")]]
    (reset! global/stream-values [])
    (runtime/eval-string (compiler/compile (get case "ys")))
    (is (= expected (mapv (fn [index doc] (reference/serialize doc index))
                          (range) @global/stream-values)))
    (when-let [binary (System/getenv "YS_REFERENCE_TEST_BIN")]
      (let [result (run-binary binary ["-l" "--stream" "-"] (get case "ys"))]
        (is (zero? (:exit result)) (:err result))
        (is (= expected (mapv json/read-str (str/split-lines (:out result)))))))))
