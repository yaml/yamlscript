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
    want (str/trim-newline (with-out-str (prn (get test "want"))))
    got (str/trim-newline (with-out-str (prn got)))]
    (if name
      (println (str "not ok " count " - " name))
      (println (str "not ok " count)))
    (println
      (str "     got: '" got "'\nexpected: '" want "'" "\n"))))

(defn plan [n]
  (println (str "1.." n)))

(defn run [test]
  (let [count (deref counter)
        keys (set (keys test))
        _ (when (not (contains? keys "code"))
            (die (str "taptest: Test " count " is missing 'code' key")))
        code (get test "code")
        _ (when (not (string? code))
            (die (str "taptest: Test " count
                   " 'code' key must be a string")))
        code (str "!yamlscript/v0\n" code "\n")]
    (try (ys/eval code)
         (catch Exception e
           (if (get test "fail")
             (.getMessage e)
             (throw e))))))

(defn test [tests]
  (doall
    (for [test tests]
      (let [keys (set (keys test))
            count (swap! counter inc)
            got (run test)]
        (cond
          (contains? keys "want")
          (if (= got (get test "want"))
            (passed test)
            (failed test got))
          ,
          (contains? keys "like")
          (let [rgx (re-pattern (get test "like"))]
            (if (re-find rgx got)
              (passed test)
              (failed test got)))
          ,
          (contains? keys "have")
          (if (str/includes? got (get test "have"))
            (passed test)
            (failed test got))
          ,
          :else
          (die (str "taptest: Test " count
                 " requires one of: 'want', 'like', 'have'")))))))

(defn done
  ([n] (let [n (or n (deref counter))]
         (println (str "1.." n))))
  ([] (done nil)))

(comment
  )
