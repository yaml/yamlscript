(declare paragraph bottles)

(defn main
  ([number] (each [n (rng number 1)] (say (paragraph n))))
  ([] (main 99)))

(defn paragraph [num]
  (str (bottles num)
       " of Perl on the wall,\n" (bottles num)
       " of Perl.\nTake one down, slurp it around,\n" (bottles (- num 1))
       " of Perl on the wall." "\n"))

(defn bottles [n]
  (cond (== n 0) "No more bottles"
        (== n 1) "1 bottle"
        :else (str n " bottles")))

(apply main ARGS)
