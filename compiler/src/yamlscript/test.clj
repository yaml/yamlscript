;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.test library defines a yaml based testing framework that is
;; used to test the YAMLScript compiler.

(ns yamlscript.test
  (:use yamlscript.debug)
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [clojure.test :as test]
   [clojure.walk :as walk]
   [clj-yaml.core :as yaml]))


;; ----------------------------------------------------------------------------
;; Key binding helpers
;; ----------------------------------------------------------------------------
(defn get-test-ns [ns]
  (let [from-ns-name (str (ns-name ns))]
    (if (str/ends-with? from-ns-name "-test")
      ns
      (find-ns (symbol (str from-ns-name "-test"))))))

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

(defn reload-all []
  (->> (all-ns)
    (filter #(re-find #"yamlscript\..*-test$" (str (ns-name %))))
    (map ns-name)
    sort
    (#(doseq [ns %]
        (require ns :reload)
        (println (str "Reloaded " ns))))))

(defn do-list-tests [ns]
  (let [ns (get-test-ns ns)
        tests
        (->> ns
          ns-publics
          keys
          (filter #(re-matches #"test-.*-\d+" (str %)))
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
               (map #(str "test-" %))
               vec)

        opts (->> opts
               (filter keyword?)
               (map #(or (get short-keys %) %))
               (#(zipmap % (repeat true))))

        opts (merge @test-opts opts)

        ns (get-test-ns *ns*)

        tests (if (nil? ns)
                []
                (if (seq want)
                  (->> want
                    (map #(ns-resolve ns (symbol %)))
                    (remove nil?)
                    vec)
                  (->> ns
                    ns-interns
                    (filter #(re-matches #"test-.*-\d+" (str (first %))))
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
(defn read-tests [file pick-func]
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
                (filter pick-func))]
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
    (filter #(re-matches #"test-.*-\d+" (str %)))
    (map #(ns-unmap ns %))
    vec))

(defmacro remove-tests []
  `(do-remove-tests ~*ns*))

(defn do-load-yaml-tests
  [ns o]
  (let [{:keys [yaml-file pick-func test-func want-func]} o
        tests (read-tests yaml-file pick-func)
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
            (let [got (test-func test)
                  want (want-func test)]
              (test/is (= want got)))))))))

(defmacro load-yaml-tests [o]
  `(do-load-yaml-tests ~*ns* ~o))

;; ----------------------------------------------------------------------------
(defn has-keys? [keys map]
  (every? #(contains? map %) keys))

(comment
  (do
    (require '[yamlscript.core :as ys])
    (->> "foo: bar baz"
      ys/compile))
  )
