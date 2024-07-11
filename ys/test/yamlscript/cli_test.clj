;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.cli-test
  (:require
   [clojure.test :as test]
   [clojure.string :as str]
   [yamlscript.cli :as cli]
   [yamlscript.test :refer [has is like]]))

(defn ys [& args]
  (let [out (try
              (with-out-str (apply cli/-main args))
              (catch Exception e
                (.getMessage e)))]
    (str/trim-newline out)))

(test/deftest cli-test

  (has (ys)
    "Usage: ys [<option...>] [<file>]"
    "No args prints help")

  (has (ys "-h")
    "Usage: ys [<option...>] [<file>]"
    "-h prints help")

  (has (ys "--help")
    "Usage: ys [<option...>] [<file>]"
    "--help prints help")

  (like (ys "--version")
    #"^YAMLScript \d+\.\d+\.\d+$"
    "--version prints version")

  (is (ys "-mb" "-le" "a: b")
    "{\"a\":\"b\"}"
    "-l with bare mode")

  (is (ys "-e" "say: \"Hello\"")
    "Hello"
    "-e uses code mode")

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
    #"std.say"
    "'say' evaluates to a symbol")

  ;; Figure out why this stopped working
  #_(has (ys "-ce" "std/say: 123")
    "(std/say 123)"
    "-c prints Clojure code of compilation")

  (is (ys "-e" "std/say: 123")
    "123"
    "std/say is the YS std println")

  (is (ys "-mc" "-le" "say: 12345" "-e" "=>: 67890")
    "12345\n67890"
    "-l in code mode")

  (has (ys "--to=foo")
    "must be one of: json, yaml, edn"
    "Validate --to=...")

  (has (ys "--run" "--load" "...")
    "Error: Options --load and --run are mutually exclusive"
    "Can't use multiple action options together")

  (has (ys "-J" "-Y" "...")
    "Error: Options --yaml and --json are mutually exclusive"
    "Can't use multiple data format options together")

  (has (ys "--help" "--stack-trace" "...")
    "Error: Options --help and --stack-trace are mutually exclusive"
    "Can't use other options with --help")

  (has (ys "--version" "--stack-trace" "...")
    "Error: Options --version and --stack-trace are mutually exclusive"
    "Can't use other options with --version")

  #_(has (ys "--mode=code" "--run" "...")
    "Error: Options --mode and --run are mutually exclusive"
    "Can't use --mode with certain actions")

  #_(has (ys "--kill" "-e" "...")
    "Error: Options --eval and --kill are mutually exclusive"
    "Can't --eval with certain actions")

  (has (ys "-ple" "...")
    "Error: Options --print and --load are mutually exclusive"
    "Can't use --print with --load")

  (has (ys "--run" "--to=json" "-e" "...")
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

  (like (ys "-pe" "find-ns: quote(str)")
    #"sci\.lang\.Namespace"
    "clojure.string ns available as str")

  (like (ys "-pe" ".[Character Long Double String Boolean]")
    #"(?s)Character.*Long.*Double.*String.*Boolean"
    "Standard java classes available")

  (is (ys "-pe" "+{:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 7 :h 8 :i 9 :j 10
                   :k 1 :l 1 :m 1 :n 1 :o 1 :p 1 :q 1 :r 1 :s 1 :t 1
                   :u 1 :v 1 :w 1 :x 1 :y 1 :z 1}")
    "{:a 1,
 :b 2,
 :c 3,
 :d 4,
 :e 5,
 :f 6,
 :g 7,
 :h 8,
 :i 9,
 :j 10,
 :k 1,
 :l 1,
 :m 1,
 :n 1,
 :o 1,
 :p 1,
 :q 1,
 :r 1,
 :s 1,
 :t 1,
 :u 1,
 :v 1,
 :w 1,
 :x 1,
 :y 1,
 :z 1}"
    "Literal maps > 8 pairs ordered by default")

  #__)

(swap! cli/testing (constantly true))
(test/run-tests)

(comment)
