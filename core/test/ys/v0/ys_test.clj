;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.v0.ys-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [ys.v0.global :as global]
   [ys.v0.ys :as portable]))

(def fixture-root
  (.getCanonicalPath (java.io.File. "test")))

(defn error-message [f]
  (try
    (f)
    nil
    (catch Throwable error
      (str/trimr (ex-message error)))))

(defn fresh-namespace []
  (let [target (create-ns (gensym "portable-use-test-"))]
    (binding [*ns* target]
      (refer 'clojure.core))
    target))

(deftest parses-portable-use-options
  (is (= {:from [:path "lib"]
          :as 'library
          :get ['one 'two/second]}
        (#'portable/parse-use-args
          [:path "lib" :as 'library :get 'one 'two/second])))
  (is (= {:from [:deps "mvn:example/lib@1/example.lib"]
          :all true}
        (#'portable/parse-use-args
          [:deps "mvn:example/lib@1/example.lib"])))
  (is (= "Duplicate 'use' option ':all'"
        (error-message
          #(#'portable/parse-use-args [:all :all])))))

(deftest loads-portable-files-and-paths
  (testing "file source with alias and no referred names"
    (let [target (fresh-namespace)
          file (str fixture-root "/use_test/portable_lib.cljc")]
      (#'portable/portable-use
        target
        (list
          (list 'use-test.portable-lib
            :file file :as 'portable :none)))
      (is (= 4 ((ns-resolve target 'portable/portable-value))))))
  (testing "path source with selected rename"
    (let [target (fresh-namespace)]
      (#'portable/portable-use
        target
        (list
          (list 'use-test.path-only :path fixture-root
            :get 'path-value/renamed)))
      (is (= 1 ((ns-resolve target 'renamed)))))))

(deftest validates-portable-file-and-url-sources
  (let [target (fresh-namespace)]
    (is (= "Portable 'use :file' does not support .ys files"
          (error-message
            #(#'portable/portable-use
               target
               '((example.module :file "example.ys" :none))))))
    (is (= "Invalid 'use' option ':url': expected an HTTPS URL"
          (error-message
            #(#'portable/portable-use
               target
               '((example.module :url "http://example.com/x.clj"
                   :none))))))))

(deftest loads-portable-dependencies
  (testing "dialect loader receives environment-controlled cache paths"
    (let [called (atom nil)
          target (fresh-namespace)]
      (binding [global/ENV
                {"YS_MAVEN_REPOSITORY" "/tmp/m2"
                 "YS_GITLIBS_DIR" "/tmp/gitlibs"}]
        (with-redefs [portable/resolve-require-deps
                      (fn []
                        (fn [options libspec]
                          (reset! called [options libspec])))]
          (#'portable/portable-use
            target
            '((clojure.string
                :deps "mvn:example/lib@1/clojure.string" :none)))))
      (is (= [{:mvn/local-repo "/tmp/m2"
               :gitlibs/dir "/tmp/gitlibs"}
              ["mvn:example/lib@1/clojure.string"]]
            @called))))
  (testing "JVM and BB use a dependency already on the classpath"
    (let [target (fresh-namespace)]
      (with-redefs [portable/resolve-require-deps (constantly nil)]
        (#'portable/portable-use
          target
          '((clojure.string :deps "mvn:example/lib@1/clojure.string"
              :get upper-case))))
      (is (= "PORTABLE" ((ns-resolve target 'upper-case) "portable")))))
  (testing "classpath-only runtimes report unavailable namespaces"
    (let [target (fresh-namespace)]
      (with-redefs [portable/resolve-require-deps (constantly nil)]
        (is (= (str "Portable 'use :deps' cannot acquire dependencies in "
                 "this runtime; put namespace 'missing.portable' on the "
                 "classpath")
              (error-message
                #(#'portable/portable-use
                   target
                   '((missing.portable
                       :deps "mvn:example/lib@1/missing.portable"
                       :none))))))))))
