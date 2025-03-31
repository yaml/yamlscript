;; Clojure is Lisp is nested expressions in parens.
(defn even-nums [start end]
  (let [initial-vec [],
        step 1]
    (loop [current start,
           result initial-vec]
      (if (> current end)
        result
        (recur (+ current step),
               (if (even? current)
                 (conj result current)
                 result))))))

(defn main
  ([] (main "1" "100"))
  ([y] (main "1" y))
  ([x y] (doall
           (for [i (even-nums
                     (parse-long x)
                     (parse-long y))]
             (println i)))))

(apply main *command-line-args*)
