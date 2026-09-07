;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; Process adapter used to replace the limited ys-v0-glj implementation.

(ns yamlscript.process
  (:require [clojure.string :as str]))

(defn- bytes-to-str [bytes]
  (fmt.Sprintf "%s" bytes))

(defn- split-opts [args]
  (if (map? (first args))
    [(first args) (rest args)]
    [{} args]))

(defn- env-strings [env]
  ((go/slice-of go/string)
   (map (fn [[key value]] (str key "=" value)) env)))

(defn- shell-command [command]
  (if (= runtime.GOOS "windows")
    ["cmd.exe" "/d" "/s" "/c" command]
    ["/bin/sh" "-c" command]))

(defn sh [& all-args]
  "Run a command and return its exit status, stdout, and stderr."
  (let [[opts args] (split-opts all-args)]
    (if (empty? args)
      {:exit 1 :out "" :err "No command specified"}
      (let [command (if (= 1 (count args))
                      (shell-command (str (first args)))
                      (mapv str args))
            [name & args] command
            cmd (apply os:exec.Command name args)
            _ (when-let [input (:in opts)]
                (set! (. cmd Stdin) (strings.NewReader (str input))))
            _ (when-let [dir (:dir opts)]
                (set! (. cmd Dir) (str dir)))
            _ (when-let [env (:env opts)]
                (set! (. cmd Env) (env-strings env)))
            [stdout-pipe stdout-error] (.StdoutPipe cmd)
            [stderr-pipe stderr-error] (.StderrPipe cmd)]
        (if (or stdout-error stderr-error)
          {:exit 1 :out "" :err "Failed to create process pipes"}
          (let [start-error (.Start cmd)]
            (if start-error
              {:exit 1 :out "" :err (str "Failed to start: " start-error)}
              (let [[stdout stdout-error] (io.ReadAll stdout-pipe)
                    [stderr stderr-error] (io.ReadAll stderr-pipe)
                    wait-error (.Wait cmd)
                    exit (if wait-error
                           (.ExitCode (.ProcessState cmd))
                           0)]
                {:exit exit
                 :out (if stdout-error "" (bytes-to-str stdout))
                 :err (if stderr-error "" (bytes-to-str stderr))}))))))))

(defn shell [& all-args]
  (let [[opts args] (split-opts all-args)]
    (sh opts (str/join " " args))))

(defn process [& args]
  (apply shell args))

(defn exec [& args]
  (apply sh args))
