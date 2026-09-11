;; Copyright 2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.expression-test
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [yamlscript.cli :as cli]
            [yamlscript.global :as global]
            [yamlscript.runtime :as runtime]))

(defn compile-arguments [argv]
  (let [[opts args error errors] (cli/get-opts argv)]
    (when (or error (seq errors))
      (throw (ex-info "Invalid test arguments" {:error error :errors errors})))
    (reset! global/opts opts)
    [(cli/get-compiled-code opts) args]))

(deftest compilation-is-unchanged
  (doseq [source ["json/dump({})" "say: json/dump({})" "!ys-0:\n[1, 2]"
                  "!ys-0:\n- 1\n- 2" "a: 1" "!ys-0:\na: 1"
                  "ns: expression-test\n=>: json/dump({})"]]
    (let [[[compiled _ _ documents]] (compile-arguments ["-ce" source])
          [[evaluated _ _ observed]] (compile-arguments ["-e" source])]
      ;; Evaluation normally wraps the last result for the document stream.
      (is (not (str/includes? compiled "(use v0)")))
      (is (not (str/includes? evaluated "(use v0)")))
      (is (empty? documents))
      (is (= evaluated (apply str (map :code observed))))
      (is (every? :auto-use-v0 observed)))))

(deftest expression-document-boundaries
  (binding [*in* (java.io.StringReader. "")]
    (let [[[code _ _ documents]] (compile-arguments ["json/dump({})"])]
      (is (= code (apply str (map :code documents))))
      (is (= [true] (mapv :auto-use-v0 documents)))))
  (let [file (str (fs/create-temp-file {:suffix ".ys"}))]
    (try
      (spit file "!ys-0\nanswer =: 42\n")
      (let [[[code _ _ documents]]
            (compile-arguments ["--file" file "-e" "--- !ys\njson/dump(from-file/answer)"])]
        (is (= code (apply str (map :code documents))))
        (is (= [false true] (mapv :auto-use-v0 documents))))
      (finally (fs/delete file)))))

(deftest evaluate-expressions
  (let [saved @global/sci-ctx]
    (try
      (doseq [[source expected]
              [["json/dump({})" "{}"]
               ["!ys-0:\n[1, 2]" [1 2]]
               ["!ys-0:\n- 1\n- 2" [1 2]]
               ["!ys-0:\na: 1" {"a" 1}]
               ["ns: expression-test\n=>: json/dump({})" "{}"]
               ["use str: :as json\n=>: json/upper-case('ok')" "OK"]]]
        (reset! global/sci-ctx (runtime/init-context))
        (reset! global/stream-values [])
        (let [[[code file _ documents] args] (compile-arguments ["-e" source])]
          (with-redefs [shutdown-agents (fn [])]
            (is (= expected (runtime/eval-string code file args documents))))))
      (finally (reset! global/sci-ctx saved)))))

(deftest native-expression-aliases
  (when-let [binary (System/getenv "YS_EXPRESSION_TEST_BIN")]
    (let [dir (str (fs/create-temp-dir {:prefix "ys-expression-"}))
          source (str dir "/input.ys")
          run (fn [& args]
                (apply process/shell
                  {:out :string :err :string :continue true} binary args))]
      (try
        (doseq [args [["-pe" "json/dump({})"]
                      ["-pe" "ns: example\n=>: json/dump({})"]
                      ["-e" "use: v0" "-pe" "=>: json/dump({})"]
                      ["-e" "use: ys::v0" "-pe" "=>: json/dump({})"]]]
          (let [result (apply run args)]
            (is (zero? (:exit result)) (:err result))
            (is (= "\"{}\"\n" (:out result)))))
        (doseq [allowed ["" "json"]]
          (let [result (process/shell
                         {:out :string :err :string :continue true
                          :extra-env {"YS_MODULES" allowed}}
                         binary "-pe" (if (= allowed "json") "json/dump({})" "42"))]
            (is (zero? (:exit result)) (:err result))))
        (is (= "\"OK\"\n"
              (:out (run "-pe"
                      "use str: :as json\n=>: json/upper-case('ok')"))))
        (is (not (zero? (:exit (run "-Cpe" "(json/dump {})")))))
        ;; A file stays opt-in even when followed by an expression.
        (spit source "!ys-0\n=>: json/dump({})\n")
        (is (not (zero? (:exit (run source)))))
        (is (not (zero? (:exit (run "-e" "--- !ys\n42" "--file" source)))))
        (spit source "42\n")
        (is (= "\"42\"\n"
              (:out (run "-pe" "json/dump(_)" "--file" source))))
        (spit source "!ys-0\nuse: v0\n=>: json/dump({})\n")
        (is (= "\"{}\"\n" (:out (run "-p" "--file" source))))
        (let [result (run "json/dump({})")]
          (is (zero? (:exit result)) (:err result))
          (is (str/includes? (:out result) "{}")))
        (let [output (str dir "/compiled.clj")
              result (run "-ce" "json/dump({})" "-o" output)]
          (is (zero? (:exit result)) (:err result))
          (is (str/includes? (slurp output) "(json/dump"))
          (is (not (str/includes? (slurp output) "(use v0)"))))
        (finally (fs/delete-tree dir))))))
