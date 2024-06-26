(defn fizzbuzz [count]
  (map (fn [n]
         (cond (zero? (mod n 15)) "FizzBuzz"
               (zero? (mod n 3))  "Fizz"
               (zero? (mod n 5))  "Buzz"
               :else n))
    (range 1 (inc count))))

(defn main [& [count]]
  (let [count (if count
                (parse-long count)
                100)]
    (doall (map println (fizzbuzz count)))))

(apply main *command-line-args*)
