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
        out (with-out-str (apply cli/-main args))]
    (str/trim-newline out)))

(test/deftest cli-test

  (has (ys)
    "Usage: ys [options] [file]")

  (has (ys "-h")
    "Usage: ys [options] [file]")

  (has (ys "--help")
    "Usage: ys [options] [file]")

  (is (ys "-le" "a: b")
     "{''a'':''b''}")

  (is (ys "-e" "say: ''Hello''")
    "Hello")

  (is (ys "-pe" "(+ 1 2)")
    "3")

  #__)

(test/run-tests)

(comment)
