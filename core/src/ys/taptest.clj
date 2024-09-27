(ns ys.taptest
  (:require
   [babashka.process :as process]
   [clojure.string :as str]
   [ys.ys :as ys]
   [yamlscript.common])
  (:refer-clojure
   :exclude [test]))

(def counter (atom 0))

(defn- get-test-name [test]
  (let [name (or
               (get test "name")
               (get test "cmnd")
               (get test "code")
               "")
        name (if (and
                   (= "error" (get test "what"))
                   (get test "want"))
               (get test "want")
               name)
        name (str/replace
               (str/trim name)
               #"\n"
               (str/re-quote-replacement "\\n"))]
    name))

(defn- passed [test]
  (let
   [name (get-test-name test)
    count (deref counter)]
    (if name
      (println (str "ok " count " - " name))
      (println (str "ok " count)))))

;; TODO - Handle unexpected LazySeq values
(defn- format-value [value]
  (let  [value (with-out-str (pr value))]
    (if (re-find #"^[^\"].*\n" value)
      (str "'" value "'")
      value)))

(defn- failed [test got]
  (let
   [name (get-test-name test)
    count (deref counter)
    keys (set (keys test))
    want (if (contains? keys "want")  ;; want can be false or nil
           (get test "want")
           (or
             (get test "like")
             (get test "have")))
    want (format-value want)
    got (format-value got)
    out (str " Failed test '" name "'\n"
          "        got: "   got "\n   expected: " want)
    out (str/replace out #"(?m)^" "#  ")]
    (if name
      (println (str "not ok " count " - " name))
      (println (str "not ok " count)))
    (println out)))

(defn- check-string [value key count]
  (when-not (string? value)
    (die (str "taptest: Test " count " '" key "' key must be a string"))))

(defn- run-code [test]
  (swap! counter inc)
  (let [count (deref counter)
        code (get test "code")
        what (get test "what")
        _ (check-string code "code" count)
        code (str "!yamlscript/v0\n" code "\n")]
    (try (if (= "out" what)
           (with-out-str (ys/eval code))
           (ys/eval code))
         (catch Exception e
           (cond
             (= "error" what) (str/trim-newline (.getMessage e))
             (get test "form") (Throwable->map e)
             :else (throw e))))))

(defn- run-cmnd [test]
  (swap! counter inc)
  (let [count (deref counter)
        cmnd (get test "cmnd")
        what (get test "what")
        _ (check-string cmnd "cmnd" count)
        ret (process/sh cmnd)]
    (if (get test "form")
      ret
      (case what
        "all" {"exit" (:exit ret)
               "out" (:out ret)
               "err" (:err ret)}
        "exit" (:exit ret)
        "out" (:out ret)
        "err" (:err ret)
        ""))))

(defn- get-tests [tests]
  (let [tests2 (filter #(get %1 "ONLY") tests)]
    (if (empty? tests2)
      (filter #(not (get %1 "SKIP")) tests)
      tests2)))

(defn- init-test [test]
  (let [keys (set (keys test))
        what (get test "what")
        form (get test "form")]
    (when (and what form (not= what "out"))
      (die "taptest: 'what' and 'form' are mutually exclusive"))
    (cond
      (contains? keys "code")
      (assoc test "what" (or what "value"))
      ,
      (contains? keys "cmnd")
      (assoc test "what" (or what "out"))
      ,
      (some #{"diag" "note"} keys)
      test
      ,
      :else
      (die "taptest: Test requires one of: 'code', 'cmnd'"))))

(defn- normalize [got test]
  (let [what (get test "what")
        form (get test "form")]
    (cond
      form (if (fn? form)
             (form got test)
             (die "taptest: 'form' must be a function"))
      (or (= what "out") (= what "err"))
      (let [got (str/trimr got)]
        (if (empty? got)
          ""
          (str got "\n")))
      :else got)))


;;------------------------------------------------------------------------------
;; Public API functions
;;------------------------------------------------------------------------------
(defn test [tests]
  (let [tests (get-tests tests)]
    (doall
      (for [test tests]
        (let [test (init-test test)
              keys (set (keys test))
              _ (do
                  (when-let [note (get test "note")]
                    (println (str "# " note)))
                  (when-let [diag (get test "diag")]
                    (binding [*out* *err*]
                      (println (str "# " diag)))))
              got (cond
                    (contains? keys "code") [(run-code test)]
                    (contains? keys "cmnd") [(run-cmnd test)])]
          (when got
            (let [got (first got)]
              (cond
                (contains? keys "want")
                (let [got (normalize got test)
                      want (normalize (get test "want")
                             {"what" (get test "what")})]
                  (if (= got want)
                    (passed test)
                    (failed test got)))
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
                (contains? keys "code")
                (let [test (assoc test "want" true)]
                  (if (= got true)
                    (passed test)
                    (failed test got)))
                (contains? keys "note") nil
                (contains? keys "diag") nil
                ,
                :else
                (die (str "taptest: Test " @counter
                       " requires one of: 'want', 'like', 'have'"))))))))))

(defn plan [n]
  (println (str "1.." n)))

(defn done
  ([n] (let [n (or n (deref counter))]
         (println (str "1.." n))))
  ([] (done nil)))

(comment
  )
