(ns YAMLScript.Core)

(defn -range [start end]
  (cond
    (<= (number start) (number end))
      (range start (inc end))
    :else
      (range start (dec end) -1)))

; XXX 'ends-with?' belongs in lingy.string when that is ready
(defn ends-with? [str substr]
  (. YAMLScript.Core (ends_with_q str substr)))

(def load-file (fn [f]
  (cond
    (ends-with? f ".ys")
    (eval (read-file-ys f))

    (ends-with? f ".ly")
    (-load-file-ly f)

    :else
    (throw (str "Can't load-file '" f "'\n")))))

(defn read-file-ys [file]
  (. YAMLScript.Core read_file_ys file))

(defn read-string-ys [string]
  (. YAMLScript.Core read_string_ys string))

(defn say [& xs] (apply println xs))

(defn yamlscript-version []
  (str
    (:major *yamlscript-version*)
    "."
    (:minor *yamlscript-version*)
    (when-let [i (:incremental *yamlscript-version*)]
      (str "." i))
    (when-let [q (:qualifier *yamlscript-version*)]
      (when (pos? (count q)) (str "-" q)))
    (when (:interim *yamlscript-version*)
      "-SNAPSHOT")))

; vim: set ft=clojure:
