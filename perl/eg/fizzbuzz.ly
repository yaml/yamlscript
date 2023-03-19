(defn main []
  (let [
    count (number (nth *ARGV* 0 "100"))
    result (fizzbuzz count)]
    (map println result)))

(defn fizzbuzz [n]
  (map
    (fn [x]
      (cond
        (zero? (mod x 15)) "FizzBuzz"
        (zero? (mod x 5)) "Buzz"
        (zero? (mod x 3)) "Fizz"
        :else x))
    (range 1 (inc n))))

(main)
