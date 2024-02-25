;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamltest.core library defines a yaml based testing framework that is
;; used to test the YAMLScript compiler.

(ns yamltest.core
  #_(:use yamlscript.debug)
  (:require
   [clojure.string :as str]
   [clojure.test :as test]
   [clj-yaml.core :as yaml]))


;; ----------------------------------------------------------------------------
;; Key binding helpers
;; ----------------------------------------------------------------------------

(def prev-test-ns (atom nil))

(defn get-test-ns [ns]
  (let [from-ns-name (str (ns-name ns))]
    (swap! prev-test-ns
      (fn [x]
        (if (str/ends-with? from-ns-name "-test")
          ns
          (or
            (find-ns (symbol (str from-ns-name "-test")))
            @prev-test-ns))))))

(def short-keys {:v :verbose
                 :a :all
                 :l :list
                 :r :reload})
(def test-opts
  (atom {:verbose (System/getenv "TEST_VERBOSE")}))

(defn set-opts [opts]
  (let [opts (str/split opts #" +")]
    (reset! test-opts
      (zipmap
        (remove nil?
          (map
            (fn [opt]
              (if-let [kw (re-find #"^:" opt)]
                (let [kw (keyword (subs kw 1))]
                  (when (contains? short-keys kw) kw))
                (when (re-matches #"\d+" opt)
                  (parse-long opt))))
            opts))
        (repeat true)))))

(defn toggle-opt [opt]
  (reset! test-opts
    (assoc @test-opts opt (not (opt @test-opts)))))

(defn clear-opts []
  (reset! test-opts {}))

(defn reload-all
  ([] (reload-all true))
  ([print]
   (->> (all-ns)
     (filter #(re-find #"yamlscript\..*-test$" (str (ns-name %1))))
     (map ns-name)
     sort
     (#(doseq [ns %1]
         (when print
           (println (str "Reloading " ns)))
         (try
           (require ns :reload)
           (catch Exception e
             (println (str "Error reloading " ns ": " (.getMessage e)))
             nil)))))))

(defn do-list-tests [ns]
  (let [ns (get-test-ns ns)
        tests
        (->> ns
          ns-publics
          keys
          (filter #(re-matches #"test-.*-\d+" (str %1)))
          sort)]
    (doseq [test tests]
      (let [label (:name (meta test))]
        (println
          (str "* " test " - " label))))
    nil))

(defmacro list-tests []
  `(do-list-tests ~*ns*))

;; ----------------------------------------------------------------------------

;; Modified version of clojure.test/run-test-var
(defn run-test-vars [vars]
  (binding [test/*report-counters* (ref test/*initial-report-counters*)]
    (let [ns-obj (-> (first vars) meta :ns)
          summary (do
                    (test/do-report {:type :begin-test-ns
                                     :ns   ns-obj})
                    (test/test-vars vars)
                    (test/do-report {:type :end-test-ns
                                     :ns   ns-obj})
                    (assoc @test/*report-counters* :type :summary))]
      (test/do-report summary)
      summary)))

(defmacro run [& opts]
  (let [want (->> opts
               (filter pos-int?)
               (map #(str "test-" %1))
               vec)

        opts (->> opts
               (filter keyword?)
               (map #(or (get short-keys %1) %1))
               (#(zipmap %1 (repeat true))))

        opts (merge @test-opts opts)

        ns (get-test-ns *ns*)

        tests (if (nil? ns)
                []
                (if (seq want)
                  (->> want
                    (map #(ns-resolve ns (symbol %1)))
                    (remove nil?)
                    vec)
                  (->> ns
                    ns-interns
                    (filter #(re-matches #"test-.*-\d+" (str (first %1))))
                    (sort-by first)
                    (map second)
                    vec)))]
    `(do
       (if (:all ~opts)
           ;; XXX Tests don't sort properly with :all
         (test/run-all-tests #"yamlscript\..*-test")
         (if (seq ~tests)
           (run-test-vars ~tests)
           (str "No tests found to run. (" ~ns ")"))))))

;; ----------------------------------------------------------------------------
(defn read-tests [file pick]
  (let [tests (->> file
                slurp
                yaml/parse-string
                (remove :TEMPLATE)
                (map-indexed #(assoc %2 :num (inc %1)))
                (remove :SKIP))
        tests (if (some :ONLY tests)
                (filter :ONLY tests)
                tests)
        tests (->> tests
                (filter pick))]
    tests))

(defn add-test-to-ns [ns sym testfn]
  (intern
    (ns-name ns)
    (vary-meta
      sym assoc :test #(testfn))
    testfn))

(defn do-remove-tests [ns]
  (->> ns
    ns-publics
    keys
    (filter #(re-matches #"test-.*-\d+" (str %1)))
    (map #(ns-unmap ns %1))
    vec))

(defmacro remove-tests []
  `(do-remove-tests ~*ns*))

(defn do-load-yaml-test-files
  [ns files conf]
  (when-not (contains? conf :add-tests)
    (do-remove-tests ns))
  (some
    (fn [file]
      (let [conf (assoc conf :yaml-file file)
            {:keys [yaml-file
                    pick
                    test
                    want]} conf
            test-fn test
            tests (read-tests yaml-file pick)
            only (vec (filter :ONLY tests))
            tests (if (seq only)
                    (do (do-remove-tests ns) only)
                    tests)
            file-name (str (last (str/split yaml-file #"/")))
            file-name (str/replace file-name #"\..*$" "")]
        (doseq [test tests]
          (let [test-name (str "test-" file-name "-" (:num test))]
            (add-test-to-ns
              ns
              (with-meta
                (symbol test-name)
                {:name (:name test)})
              (fn []
                (when (:verbose @test-opts)
                  (println (str "* " test-name " - " (:name test))))
                (let [got (test-fn test)
                      want (want test)]
                  (cond
                    (and (string? want) (re-find #"^=~\s*" want))
                    (let [want (str/replace want #"^=~\s*" "")
                          want (str/trim-newline want)]
                      (test/is (re-find (re-pattern want) got)))

                    (and (string? want) (re-find #"^~~\s*" want))
                    (let [want (str/replace want #"^~~\s*" "")
                          want (str/trim-newline want)]
                      (test/is (str/includes? got want)))

                    :else
                    (test/is (= want got))))))))
        (when (seq only)
          (let [note (str "Note: ONLY is set in " yaml-file)]
            (println note)
            note))))
    files))

(defmacro load-yaml-test-files [files conf]
  `(do-load-yaml-test-files ~*ns* ~files ~conf))

;; ----------------------------------------------------------------------------
(defn has-keys? [keys map]
  (every? #(contains? map %1) keys))

(comment)
