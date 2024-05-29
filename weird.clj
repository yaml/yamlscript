(ns weird)

(def ARGS *command-line-args*)

(defn divisors [n]
  (loop [divs [1], divs2 [], i 2]
    (if (<= (* i i) n)
      (if (== (rem n i) 0)
        (let [divs (conj divs i)
              j (/ n i)
              divs2 (if (not (= i j))
                      (conj divs2 j)
                      divs2)]
          (recur divs divs2 (inc i)))
        (recur divs divs2 (inc i)))
      (concat divs2 (reverse divs)))))

(defn abundant [n divs] (> (apply + divs) n))

(defn semiperfect [n divs]
  (when (> (count divs) 0)
    (let [h (first divs)
          t (rest divs)]
      (if (< n h)
        (semiperfect n t)
        (or (= n h) (semiperfect (- n h) t) (semiperfect n t))))))

(defn sieve [limit]
  (loop [i 2, w1 (vec (repeat limit false))]
    (if (< i limit)
      (let [w2 (if (not (nth w1 i))
                 (let [divs (divisors i)]
                   (if (not (abundant i divs))
                     (assoc w1 i true)
                     (if (semiperfect i divs)
                       (loop [j i, w3 w1]
                         (if (< j limit)
                           (let [w4 (assoc w3 j true)]
                             (recur (+ j i) w4))
                           w3))
                       w1)))
                 w1)]
        (recur (+ i 2) w2))
      w1)))

(defn main
  ([max]
   (let [w (sieve 17000)]
     (println (str "The first " max " weird numbers:"))
     (loop [count 0
            n 2]
       (when (< count max)
         (let [count (if (not (nth w n))
                       (do
                         (print (str n " "))
                         (inc count))
                       count)]
           (recur count (+ n 2)))))
     (println)))
  ([] (main 25)))

(time (apply main ARGS))
