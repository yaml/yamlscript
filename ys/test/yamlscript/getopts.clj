;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.getopts
  (:require
   [clojure.test :as test]
   [yamlscript.cli :as cli]
   [yamlscript.common]
   [yamlscript.test :as yt :refer [is]])
  (:refer-clojure))

(defn ys [& args]
  (let [[opts args error] (cli/get-opts args)
        opts (if (empty? (:eval opts))
               (dissoc opts :eval)
               opts)
        opts (if (empty? (:debug-stage opts))
               (dissoc opts :debug-stage)
               opts)
        opts (if (nil? (:file opts))
               (dissoc opts :file)
               opts)]
    (if error
      error
      [opts args])))

#_(ns-unmap *ns* 'is)
#_(defn is [in out]
  (println (apply str "* " in))
  (yt/is in out))

(test/deftest opts-args-test

; )(comment

  (is (ys)
    [{:help true} []])

  (is (ys "-p") [{:print true} []])

  (is (ys "foo")
    [{:file "foo"} []])

  (is (ys ".foo")
    [{:eval [".foo"],
      :mode "code"
      :load true
      :to "yaml"
      :file "-"} []])

  (is (ys ".foo" "bar" "baz")
    [{:eval [".foo"]
      :file "bar"
      :mode "code"
      :load true
      :to "yaml"}
     ["baz"]])

  (is (ys ".foo" "--" "bar" "baz")
    [{:eval [".foo"]
      :file "-"
      :mode "code"
      :load true
      :to "yaml"}
     ["bar" "baz"]])

  (is (ys "-e" ".foo" "--" "bar" "baz")
    [{:eval [".foo"]
      :mode "code"}
     ["bar" "baz"]])

  (is (ys "-f" ".foo" "bar" "baz")
    [{:file ".foo"}
     ["bar" "baz"]])

  (is (ys "-f" ".foo" "bar" "-e" "baz")
    [{:eval ["baz"], :file ".foo", :mode "code"}
     ["bar"]])

  (is (ys "-Ye" ".0.name" "-")
    [{:eval [".0.name"]
      :file "-"
      :yaml true
      :to "yaml"
      :mode "code"
      :load true}
     []])

  (is (ys "-mb" "-le" "a: b")
    [{:eval ["a: b"], :mode "b", :load true} []])

  (is (ys "-ce" "std/say: 123")
    [{:eval ["std/say: 123"], :compile true, :mode "code"} []])

  (is (ys "-lp") "Options --print and --load are mutually exclusive.")

  #__)

(swap! cli/testing (constantly true))
(test/run-tests)

(comment
  )
