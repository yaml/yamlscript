(ns yamlscript.main-test
  (:require
   [clojure.test :refer [deftest is]]
   [yamlscript.core :as ys]))

(deftest load-test
  (let [data (ys/load "a: 1")]
    (is (= 1 (data "a")))))
