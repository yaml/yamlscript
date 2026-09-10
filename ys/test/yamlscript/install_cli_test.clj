;; Copyright 2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.install-cli-test
  (:require
   [babashka.fs :as fs]
   [clojure.test :refer [deftest is]]
   [yamlscript.cli :as cli]))

(deftest installer-options
  (doseq [option [:install :upgrade :install-m2]]
    (is (nil? (cli/validate-opts {option true})))
    (is (nil? (cli/validate-opts {option true :stack-trace true})))
    (doseq [[key value] [[:eval ["say: 1"]] [:output "output"]
                        [:compile true] [:load true] [:to "bb"]
                        [:help true] [:version true]]]
      (is (re-find #"mutually exclusive"
            (cli/validate-opts {option true key value}))))
    (let [[opts _ error] (cli/get-opts [(str "--" (name option))])]
      (is (get opts option))
      (is (nil? error)))
    (let [[_ _ error] (cli/get-opts [(str "--" (name option)) "file.ys"])]
      (is (re-find #"do not accept file arguments" error))))
  (is (re-find #"mutually exclusive"
        (cli/validate-opts {:install true :install-m2 true}))))

(deftest invalid-output-does-not-create-file
  (let [dir (fs/create-temp-dir)
        path (str dir "/output")]
    (try
      (with-redefs [cli/exit (constantly nil)]
        (is (re-find #"mutually exclusive"
              (with-out-str (cli/-main "--install-m2" "-o" path)))))
      (is (not (fs/exists? path)))
      (finally (fs/delete-tree dir)))))
