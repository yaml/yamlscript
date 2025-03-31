(defn foo []
  (let [a (reduce + (rng 1 (rand-int 8)))]
    (cond
      (> a 20) "yes"
      (> a 10) "no"
      :else "maybe")))

(+++ (each [i (rng 1 10)] (say (foo))))
