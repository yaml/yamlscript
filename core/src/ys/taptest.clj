(ns ys.taptest
  (:require
   [clojure.string :as str]
   [ys.ys :as ys]
   [yamlscript.util :refer [die]])
  (:refer-clojure
   :exclude [test]))

(def counter (atom 0))

(defn- passed [test]
  (let
   [name (get test "name")
    count (deref counter)]
    (if name
      (println (str "ok " count " - " name))
      (println (str "ok " count)))))

(defn- failed [test got]
  (let
   [name (get test "name")
    count (deref counter)
    want (get test "want")]
    (if name
      (println (str "not ok " count " - " name))
      (println (str "not ok " count)))
    (println
      (str "     got: '" got "'\nexpected: '" want "'" "\n"))))

(defn plan [n]
  (println (str "1.." n)))

(defn test [tests]
  (doall
    (for [test tests]
      (let
       [count (swap! counter inc)
        code (or
               (get test "eval")
               (die (str "taptest: Test " count " is missing 'eval' key")))
        code (str "!yamlscript/v0\n" code "\n")
        got (cond
              (string? code) (ys/eval code)
              :else (die (str "taptest: Invalid eval: '"
                           code "'. Must be string.")))
        want (get test "want")
        like (get test "like")
        has (get test "has")]
        (cond
          want (if (= got want)
                 (passed test)
                 (failed test got))
          like (let [rgx (re-pattern like)]
                 (if (re-find rgx got)
                   (passed test)
                   (failed test got)))
          has (if (str/includes? got has)
                (passed test)
                (failed test got))
          :else (die (str "taptest: Test " count
                       " requires one of: 'want', 'like', 'has'")))))))

(defn done
  ([n] (let [n (or n (deref counter))]
         (println (str "1.." n))))
  ([] (done nil)))

(comment
  )
