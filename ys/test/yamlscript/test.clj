;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.test
  (:require
   [clojure.test :as test]
   [clojure.string :as str]))

(def test-opts
  (atom {:verbose (System/getenv "TEST_VERBOSE")}))

(defn do-verbose [label want]
  (when (:verbose @test-opts)
    (let [label (if label
                  label
                  (let [label (str/replace want #"\n" "\\n")
                        label (if (> (count label) 50)
                                (str (subs label 0 50) "...")
                                label)]
                    (str "'" label "'")))]
      (println (str "* Testing: " label)))))

(defn is [got want & [label]]
  (let [want (str/replace want #"''" "\"")]
    (do-verbose label want)
    (test/is (= want got))))

(count "I like pie")

(defn like [got want & [label]]
  (do-verbose label want)
  (test/is (re-find want got)))

(defn has [got want & [label]]
  (do-verbose label want)
  (test/is (str/index-of got want)))

(comment
  )
