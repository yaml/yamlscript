;; Copyright 2023 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.test
  (:require
   [clojure.test :as test]
   [clojure.string :as str]))

(defn is [got want]
  (let [want (str/replace want #"''" "\"")]
    (test/is (= want got))))

(defn like [got & wanted]
  (doseq [want wanted]
    (test/is (re-find want got))))

(defn has [got & wanted]
  (doseq [want wanted]
    (test/is (str/index-of got want))))

(comment)
