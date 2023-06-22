(ns YAMLScript.Core)

(defn ends-with? [str substr]
  (. YAMLScript.Core (ends_with_q str substr)))

(defn read-file-ys [file]
  (. YAMLScript.Core read_file_ys file))

(def load-file (fn [f]
  (cond
    (ends-with? f ".ys")
    (eval (read-file-ys f))

    (ends-with? f ".ly")
    (-load-file-ly f)

    :else
    (throw (str "Can't load-file '" f "'\n")))))

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
