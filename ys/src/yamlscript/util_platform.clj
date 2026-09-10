;; Copyright 2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.util-platform
  (:require
   [babashka.fs :as fs]
   [clojure.data.json :as json]))

(defn run-command [argv]
  (try
    ;; Drain a combined stream synchronously. Future-based readers keep the
    ;; native process alive after installation while their executor expires.
    (let [command (-> (ProcessBuilder. ^java.util.List argv)
                   (.redirectErrorStream true)
                   .start)
          _ (.close (.getOutputStream command))
          out (with-open [input (.getInputStream command)] (slurp input))
          exit (.waitFor command)]
      {:exit exit :out out :err (if (zero? exit) "" out)})
    (catch Exception e {:exit 1 :out "" :err (.getMessage e)})))

(defn context []
  {:run run-command
   :entries #(mapv (comp str fs/file-name) (fs/list-dir %))
   :json json/read-str
   :os (System/getProperty "os.name")
   :arch (System/getProperty "os.arch")
   :env #(System/getenv %)
   :exe (str (fs/real-path
               (-> (java.lang.ProcessHandle/current) .info .command .get)))})
