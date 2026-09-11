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
  (let [[exe error] (if (= runtime.GOOS "wasip1") ["" nil] (os.Executable))]
    (when error (throw error))
    (let [[exe error] (if (seq exe) (path:filepath.EvalSymlinks exe) [exe nil])]
      (when error (throw error))
      {:run run-command :entries directory-entries
       :start-progress
       (fn [pending success failure]
         (github.com:yaml:yamlscript:internal:goyamlparser.StartCompileProgress
           pending success failure))
       :exists? (fn [path] (let [[_ error] (os.Lstat path)] (nil? error)))
       :write-source (fn [path text executable?]
                       (when-let [error
                                  (github.com:yaml:yamlscript:internal:goyamlparser.WriteNewTextFile
                                    path text (if executable? 0755 0644))]
                         (throw error)))
       :read slurp
       :write (fn [path text]
                (when-let [error
                           (github.com:yaml:yamlscript:internal:goyamlparser.WriteTextFile
                             path text 0644)]
                  (throw error)))
       :absolute (fn [path]
                   (let [[path error] (path:filepath.Abs path)]
                     (when error (throw error)) path))
       :relative (fn [base path]
                   (let [[path error] (path:filepath.Rel base path)]
                     (when error (throw error)) path)) :json json/load
       :os runtime.GOOS :arch runtime.GOARCH
       :env #(System/getenv %) :exe exe})))
