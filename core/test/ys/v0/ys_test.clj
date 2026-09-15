;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.v0.ys-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [ys.v0 :as v0]
   [ys.v0.common :as common]
   [ys.v0.global :as global]
   [ys.v0.manifest :as manifest]
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

(defn refer-standard [target]
  (binding [*ns* target]
    (doseq [sym (keys (ns-publics 'ys.v0.std))]
      (ns-unmap target sym))
    (refer 'ys.v0.std)))

(deftest splits-yspath-with-platform-separator
  (is (= ["/one" "/two"]
        (#'common/split-yspath "/one:/two" ":")))
  (is (= ["D:/one" "E:/two"]
        (#'common/split-yspath "D:/one;E:/two" ";"))))

(deftest parses-portable-use-options
  (is (= {:source [:path "lib"]
          :as 'library
          :get ['one 'two/second]}
        (#'portable/parse-use-args
          [:path "lib" :as 'library :get 'one 'two/second])))
  (is (= {:source [:from "mvn:example/lib@1/example.lib"]}
        (#'portable/parse-use-args
          [:from "mvn:example/lib@1/example.lib"])))
  (is (= "Invalid 'use' option ':deps'"
        (error-message
          #(#'portable/parse-use-args [:deps "x"]))))
  (is (= (str "Invalid 'use' option ':from': source option ':path' "
           "is already set")
        (error-message
          #(#'portable/parse-use-args [:path "one" :from "two"]))))
  (is (= "Duplicate 'use' option ':all'"
        (error-message
          #(#'portable/parse-use-args [:all :all])))))

(deftest normalizes-short-use-forms
  (is (= '((ys.http :as http))
        (portable/normalize-use-forms '(http))))
  (is (= '((ys.http :as http) (ys.fs :as fs) (ys.ipc :as ipc))
        (portable/normalize-use-forms '(http fs ipc))))
  (is (= '((ys.http :all))
        (portable/normalize-use-forms '(http :all))))
  (is (= '((ys.http :as web))
        (portable/normalize-use-forms '(http :as web))))
  (is (= '((medley
             :from "mvn:dev.weavejester/medley@1.10.0/medley.core"))
        (portable/normalize-use-forms
          '(medley
             :from "mvn:dev.weavejester/medley@1.10.0/medley.core"))))
  (is (= '((foo.bar :get baz)
           (ys.http :as http)
           (xyz.abc :all))
        (portable/normalize-use-forms
          '((foo.bar :get baz) (http) (xyz.abc :all))))))

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

(deftest loads-portable-built-in-modules
  (let [target (fresh-namespace)]
    (#'portable/portable-use
      target
      '((ys.fs :as fs)))
    (is (string? ((ns-resolve target 'fs/cwd))))
    (is (= 'ys.v0.fs
          (ns-name (get (ns-aliases target) 'ys.fs))))))

(deftest restricts-portable-core-side-effects
  (let [target (fresh-namespace)]
    (binding [*ns* target]
      (v0/init))
    (doseq [sym manifest/hidden-core]
      (is (nil? (ns-resolve target sym))
        (str "portable core function is hidden: " sym)))
    (is (some? (ns-resolve target 'read)))
    (is (some? (ns-resolve target 'readline)))))

(deftest plain-use-does-not-refer-public-names
  (let [target (fresh-namespace)]
    (#'portable/portable-use target '((ys.str)))
    (is (= "QUALIFIED"
          ((ns-resolve target 'ys.str/upper-case) "qualified")))
    (is (nil? (ns-resolve target 'upper-case)))))

(deftest reselects-portable-standard-functions
  (let [target (fresh-namespace)]
    (refer-standard target)
    (#'portable/portable-use target '((ys.std :not read write)))
    (is (nil? (ns-resolve target 'read)))
    (is (nil? (ns-resolve target 'write)))
    (is (some? (ns-resolve target 'say))))
  (let [target (fresh-namespace)]
    (refer-standard target)
    (ns-unmap target 'read)
    (intern target 'read :local)
    (#'portable/portable-use target '((ys.std :none)))
    (is (= :local (var-get (ns-resolve target 'read))))))

(deftest require-is-retired
  (is (= "The 'require' function is retired. Use 'use' instead."
        (error-message #(portable/require 'ys.str)))))

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
          loader-ns (atom nil)
          target (fresh-namespace)]
      (binding [global/ENV
                {"YS_MAVEN_REPOSITORY" "/tmp/m2"
                 "YS_GITLIBS_DIR" "/tmp/gitlibs"}]
        (with-redefs [portable/resolve-require-deps
                      (fn []
                        (fn [options libspec]
                          (reset! called [options libspec])
                          (reset! loader-ns *ns*)
                          (clojure.core/alias
                            (last libspec) 'clojure.string)))]
          (#'portable/portable-use
            target
            '((clojure.string
                :from "mvn:example/lib@1/clojure.string" :none)))))
      (is (= {:mvn/local-repo "/tmp/m2"
              :gitlibs/dir "/tmp/gitlibs"}
            (first @called)))
      (is (= "mvn:example/lib@1/clojure.string"
            (first (second @called))))
      (is (= :as (second (second @called))))
      (is (not= target @loader-ns)
          "dependency loading does not rebind the caller namespace")
      (is (nil? (find-ns (ns-name @loader-ns)))
          "the temporary loader namespace is removed")))
  (testing "short module names alias the namespace loaded by the coordinate"
    (let [target (fresh-namespace)]
      (with-redefs [portable/resolve-require-deps (constantly nil)]
        (#'portable/portable-use
          target
          '((stringy :from "mvn:example/lib@1/clojure.string"
              :get upper-case))))
      (is (= "ALIAS" ((ns-resolve target 'stringy/upper-case) "alias")))
      (is (= "REFER" ((ns-resolve target 'upper-case) "refer")))))
  (testing "explicit aliases override short module names"
    (let [target (fresh-namespace)]
      (with-redefs [portable/resolve-require-deps (constantly nil)]
        (#'portable/portable-use
          target
          '((stringy :from "mvn:example/lib@1/clojure.string"
              :as text :none))))
      (is (nil? (get (ns-aliases target) 'stringy)))
      (is (= "TEXT" ((ns-resolve target 'text/upper-case) "text")))))
  (testing "JVM and BB use a dependency already on the classpath"
    (let [target (fresh-namespace)]
      (with-redefs [portable/resolve-require-deps (constantly nil)]
        (#'portable/portable-use
          target
          '((clojure.string :from "mvn:example/lib@1/clojure.string"
              :get upper-case))))
      (is (= "PORTABLE" ((ns-resolve target 'upper-case) "portable")))))
  (testing "classpath-only runtimes report unavailable namespaces"
    (let [target (fresh-namespace)]
      (with-redefs [portable/resolve-require-deps (constantly nil)]
        (is (= (str "Portable 'use :from' cannot acquire dependencies in "
                 "this runtime; put namespace 'missing.portable' on the "
                 "classpath")
              (error-message
                #(#'portable/portable-use
                   target
                   '((missing.portable
                       :from "mvn:example/lib@1/missing.portable"
                       :none))))))))))
