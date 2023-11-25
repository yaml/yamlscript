;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.cli-test
  (:require
   [clojure.test :as test]
   [clojure.string :as str]
   [yamlscript.cli :as cli]
   [yamlscript.test :refer :all]))

(defn ys [& args]
  (let [out (try
              (with-out-str (apply cli/-main args))
              (catch Exception e
                (.getMessage e)))]
    (str/trim-newline out)))

(test/deftest cli-test

  (has (ys)
    "Usage: ys [options] [file]"
    "No args prints help")

  (has (ys "-h")
    "Usage: ys [options] [file]"
    "-h prints help")

  (has (ys "--help")
    "Usage: ys [options] [file]"
    "--help prints help")

  (like (ys "--version")
    #"^YAMLScript \d+\.\d+\.\d+$"
    "--version prints version")

  (is (ys "-le" "a: b")
    "{\"a\":\"b\"}"
    "-l uses data mode and prints json")

  (is (ys "-e" "say: \"Hello\"")
    "Hello"
    "-e uses script mode")

  (is (ys "-pe" "(+ 1 2)")
    "3"
    "-p prints result of evaluation")

  (is (ys "../test/hello.ys")
    "Hello"
    "File arg is loaded and run")

  (is (ys "-l" "../test/hello.ys")
    "Hello\n12345"
    "-l prints json result of file run")

  (is (ys "-p" "../test/hello.ys")
    "Hello\n12345"
    "-p prints Clojure result of file run")

  (like (ys "-pe" "say")
    #"ys\.std.say"
    "'say' evaluates to a symbol")

  (is (ys "-ce" "std/say: 123")
    "(std/say 123)"
    "-c prints Clojure code of compilation")

  (is (ys "-e" "std/say: 123")
    "123"
    "std/say is the YS std println")

  (is (ys "-ms" "-le" "say: 12345" "-e" "=>: 67890")
    "12345\n67890"
    "-l in script mode")

  (has (ys "--to=foo")
    "must be one of: json, yaml, edn"
    "Validate --to=...")

  (has (ys "--run" "--load" "...")
    "Error: Options --load and --run are mutually exclusive"
    "Can't use multiple action options together")

  (has (ys "-J" "-Y" "...")
    "Error: Options --yaml and --json are mutually exclusive"
    "Can't use multiple data format options together")

  (has (ys "--help" "--debug" "...")
    "Error: Options --help and --debug are mutually exclusive"
    "Can't use other options with --help")

  (has (ys "--version" "--debug" "...")
    "Error: Options --version and --debug are mutually exclusive"
    "Can't use other options with --version")

  (has (ys "--mode=script" "--nrepl" "...")
    "Error: Options --mode and --nrepl are mutually exclusive"
    "Can't use i--mode with certain actions")

  (has (ys "--kill" "-e" "...")
    "Error: Options --eval and --kill are mutually exclusive"
    "Can't --eval with certain actions")

  (has (ys "-ple" "...")
    "Error: Options --print and --load are mutually exclusive"
    "Can't use --print with --load")

  (has (ys "--to=json" "...")
    "Error: Options --to and --run are mutually exclusive"
    "Can't use --to with --run")

  (is (ys "-Y" "../test/loader.ys")
    "foo: This is a string
bar:
  foo:
    bar:
    - aaa: 1
    - bbb: 2"
    "Testing the 'load' function to load another YS file")

  #__)

(swap! cli/testing (constantly true))
(test/run-tests)

(comment)
