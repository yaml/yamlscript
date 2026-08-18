;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.externals-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [sci.core :as sci]
   [yamlscript.compiler :as compiler]
   [yamlscript.deps :as deps]
   [yamlscript.externals :as externals]
   [yamlscript.runtime :as runtime]))

(def fixture-root
  (.getCanonicalPath (java.io.File. "test")))

(defn eval-ys
  "Compile and evaluate a YAMLScript test program."
  [source]
  (runtime/eval-string (compiler/compile (str "!ys-0\n" source))))

(defn error-message
  "Return the message thrown by f."
  [f]
  (try
    (f)
    nil
    (catch Throwable error
      (str/trimr (ex-message error)))))

(deftest parses-use-options
  (is (= {:from [:path "lib"]
          :as 'library
          :get ['one 'two/second]}
        (externals/parse-args
          [:path "lib" :as 'library :get 'one 'two/second])))
  (is (= {:not ['one 'two]}
        (externals/parse-args [:not 'one 'two])))
  (is (= {:from [:deps "mvn:example/lib@1/example.lib"]
          :all true}
        (externals/parse-args
          [:deps "mvn:example/lib@1/example.lib"])))
  (is (= {:none true}
        (externals/parse-args [:none])))
  (is (= {:as 'library}
        (externals/parse-args [:as 'library]))))

(deftest rejects-invalid-use-options
  (testing "removed and unknown options"
    (is (= "Invalid 'use' option ':mvn'"
          (error-message #(externals/parse-args [:mvn "x"]))))
    (is (= "Invalid 'use' option ':git'"
          (error-message #(externals/parse-args [:git "x"]))))
    (is (= "Invalid 'use' option ':refer'"
          (error-message #(externals/parse-args [:refer 'one])))))
  (testing "arity and type validation"
    (is (= "Invalid 'use' option ':path': expected one string"
          (error-message #(externals/parse-args [:path]))))
    (is (= "Invalid 'use' option ':as': expected one symbol"
          (error-message #(externals/parse-args [:as "alias"]))))
    (is (= (str "Invalid 'use' option ':get': expected at least one "
             "symbol")
          (error-message #(externals/parse-args [:get]))))
    (is (= "Invalid 'use' option ':not': expected plain symbols"
          (error-message #(externals/parse-args [:not 'one/two])))))
  (testing "duplicates and conflicts"
    (is (= "Duplicate 'use' option ':all'"
          (error-message #(externals/parse-args [:all :all]))))
    (is (= (str "Invalid 'use' option ':file': source option ':path' "
             "is already set")
          (error-message
            #(externals/parse-args [:path "one" :file "two"]))))
    (is (= (str "Invalid 'use' options: ':get' cannot be combined with "
             "':all'")
          (error-message
            #(externals/parse-args [:all :get 'one]))))
    (is (= (str "Invalid 'use' options: ':get' cannot be combined with "
             "':not'")
          (error-message
            #(externals/parse-args [:get 'one :not 'two]))))
    (is (= (str "Invalid 'use' options: ':none' cannot be combined with "
             "':get', ':all', or ':not'")
          (error-message
            #(externals/parse-args [:none :not 'one]))))))

(deftest loads-local-use-sources
  (testing "path source and default refer all"
    (is (= [1 2]
          (eval-ys
            (format
              (str "ns: use-path-case\n"
                "use use-test::path-lib: :path %s\n"
                "vector: path-one() path-two()")
              (pr-str fixture-root))))))
  (testing "exact YAMLScript file and get rename"
    (is (= [3 4]
          (eval-ys
            (format
              (str "ns: use-file-case\n"
                "use use-test::file-lib: :file %s "
                ":get file-one/renamed file-two\n"
                "vector: renamed() file-two()")
              (pr-str (str fixture-root "/use_test/file_lib.ys")))))))
  (testing "portable Clojure file, alias, and none"
    (is (= 4
          (eval-ys
            (format
              (str "ns: use-portable-case\n"
                "use use-test::portable-lib: :file %s "
                ":as portable :none\n"
                "=>: portable/portable-value()")
              (pr-str
                (str fixture-root "/use_test/portable_lib.cljc")))))))
  (testing "exact Clojure file and dynamically loaded dependency"
    (is (= 8
          (eval-ys
            (format
              (str "ns: use-file-dependency-case\n"
                "use use-test::file-with-dep: :file %s "
                ":get file-total\n"
                "=>: file-total()")
              (pr-str
                (str fixture-root "/use_test/file_with_dep.clj")))))))
  (testing "exclude selected names"
    (is (= 1
          (eval-ys
            (format
              (str "ns: use-not-case\n"
                "use use-test::path-lib: :path %s :all :not path-two\n"
                "=>: path-one()")
              (pr-str fixture-root)))))
    (is (= "Could not resolve symbol: path-two"
          (error-message
            #(eval-ys
               (format
                 (str "ns: use-not-missing-case\n"
                   "use use-test::path-lib: :path %s :not path-two\n"
                   "=>: path-two()")
                 (pr-str fixture-root))))))))

(deftest validates-url-and-dependency-sources
  (is (= "Invalid 'use' option ':url': expected an HTTP(S) URL"
        (error-message #(externals/load-url nil "github:one/two/file"))))
  (doseq [[coordinate provider]
          [["mvn:example/lib@1/str" :mvn]
           ["gist:owner/0123456789abcdef/source.clj" :gist]
           ["https://gist.github.com/owner/0123456789abcdef" :gist]
           ["github:owner/repo/main/src/str.cljc" :github]]]
    (let [parsed (atom nil)]
      (with-redefs [deps/prepare-required!
                    (fn [coordinate require! _]
                      (reset! parsed coordinate)
                      (require! 'str)
                      'str)]
        (is (nil?
              (externals/use-module
                (sci/create-ns (gensym "use-deps-case"))
                'str
                [:deps coordinate :none])))
        (is (= provider (:provider @parsed))))))
  (is (= "Unsupported require coordinate: https://example.com/source.clj"
        (error-message
          #(externals/use-module
             (sci/create-ns 'use-deps-invalid-case)
             'str
             [:deps "https://example.com/source.clj" :none]))))
  (with-redefs [deps/prepare-required! (fn [& _] 'other.namespace)]
    (is (= (str "Dependency namespace 'other.namespace' does not match "
             "use module 'str'")
          (error-message
            #(externals/use-module
               (sci/create-ns 'use-deps-mismatch-case)
               'str
               [:deps "mvn:example/lib@1/str" :none]))))))
