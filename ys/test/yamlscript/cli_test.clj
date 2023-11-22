;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.cli-test
  (:require
   [clojure.test :as test]
   [clojure.string :as str]
   [yamlscript.cli :as cli]
   [yamlscript.test :refer :all]))

(defn ys [& args]
  (let [args (map #(str/replace % #"''" "\"") args)
        out (try
              (with-out-str (apply cli/-main args))
              (catch Exception e
                (.getMessage e)))]
    (str/trim-newline out)))

(test/deftest cli-test

  (has (ys)
    "Usage: ys [options] [file]")

  (has (ys "-h")
    "Usage: ys [options] [file]")

  (has (ys "--help")
    "Usage: ys [options] [file]")

  (like (ys "--version")
    #"^YAMLScript \d+\.\d+\.\d+$")

  (is (ys "-le" "a: b")
     "{''a'':''b''}")

  (is (ys "-e" "say: ''Hello''")
    "Hello")

  (is (ys "-pe" "(+ 1 2)")
    "3")

  (is (ys "../test/hello.ys")
    "Hello")

  (is (ys "-l" "../test/hello.ys")
    "Hello\n12345")

  (is (ys "-p" "../test/hello.ys")
    "Hello\n12345")

  (like (ys "-pe" "say")
    #"ys\.std.say")

  (is (ys "-ce" "std/say: 123")
    "(std/say 123)")

  (is (ys "-e" "std/say: 123")
    "123")

  (is (ys "-ms" "-le" "say: 12345" "-e" "identity: 67890")
    "12345\n67890")

  (has (ys "--to=foo")
    "must be one of: json, yaml, edn")

  (has (ys "--run" "--load" "...")
    "Error: Options --load and --run are mutually exclusive")

  (has (ys "-J" "-Y" "...")
    "Error: Options --yaml and --json are mutually exclusive")

  (has (ys "--help" "--debug" "...")
    "Error: Options --help and --debug are mutually exclusive")

  (has (ys "--version" "--debug" "...")
    "Error: Options --version and --debug are mutually exclusive")

  (has (ys "--mode=script" "--nrepl" "...")
    "Error: Options --mode and --nrepl are mutually exclusive")

  (has (ys "--kill" "-e" "...")
    "Error: Options --eval and --kill are mutually exclusive")

  (has (ys "-ple" "...")
    "Error: Options --print and --load are mutually exclusive")

  (has (ys "--to=json" "...")
    "Error: Options --to and --run are mutually exclusive")

  (is (ys "-Y" "../test/loader.ys")
"foo: This is a string
bar:
  foo:
    bar:
    - aaa: 1
    - bbb: 2")

  #__)

(swap! cli/testing (constantly true))
(test/run-tests)

(comment)
