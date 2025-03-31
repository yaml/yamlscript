(def foo 1)

(defn main [a]
  (let [b 3
        c (_+ b a)]
    (say c)
    (let [d (/ c 2)]
      (say d))))

(apply main ARGS)
