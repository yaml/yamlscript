;; Copyright 2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.util-platform
  (:require [ys.v0.json :as json]))

(defn run-command [argv]
  (let [cmd (apply os:exec.Command (first argv) (rest argv))
        [bytes error] (.CombinedOutput cmd)
        out (fmt.Sprintf "%s" bytes)]
    {:exit (if error 1 0) :out out
     :err (if error (str error "\n" out) "")}))

(defn directory-entries [path]
  (let [[entries error] (os.ReadDir path)]
    (when error (throw error))
    (mapv #(.Name %) entries)))

(defn context []
  (let [[exe error] (os.Executable)]
    (when error (throw error))
    (let [[exe error] (path:filepath.EvalSymlinks exe)]
      (when error (throw error))
      {:run run-command :entries directory-entries :json json/load
       :os runtime.GOOS :arch runtime.GOARCH
       :env #(System/getenv %) :exe exe})))
