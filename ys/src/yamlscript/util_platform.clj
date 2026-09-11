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

(defn start-progress [pending success failure]
  (let [writer *err*
        started (System/nanoTime)
        terminal? (and (System/console) (not= "dumb" (System/getenv "TERM")))
        color? (and terminal? (nil? (System/getenv "NO_COLOR")))
        emit (fn [text] (binding [*out* writer] (print text) (flush)))
        _ (emit (str "… " pending (when-not terminal? "\n")))
        ticker (when terminal?
                 (doto (Thread.
                         ^Runnable
                         (fn []
                           (try
                             (loop [] (Thread/sleep 1000) (emit ".") (recur))
                             (catch InterruptedException _))))
                   (.setDaemon true)
                   (.start)))]
    (fn [ok?]
      (when ticker (.interrupt ticker) (.join ticker))
      (emit (str (when terminal? "\r\u001b[2K")
              (when color? (if ok? "\u001b[32m" "\u001b[31m"))
              (if ok? "√" "X") (when color? "\u001b[0m") " "
              (if ok? success failure)
              (when ok?
                (format " (%.1fs)" (/ (- (System/nanoTime) started) 1e9)))
              "\n")))))

(defn context []
  {:run run-command
   :start-progress start-progress
   :write spit :read slurp
   :exists? #(or (fs/exists? %) (fs/sym-link? %))
   :write-source (fn [path text executable?]
                   (fs/create-dirs (fs/parent (fs/absolutize path)))
                   (fs/create-file path)
                   (try
                     (spit path text)
                     (when executable? (.setExecutable (fs/file path) true false))
                     (catch Exception e (fs/delete path) (throw e))))
   :absolute #(str (fs/normalize (fs/absolutize %)))
   :relative #(str (fs/relativize %1 %2))
   :entries #(mapv (comp str fs/file-name) (fs/list-dir %))
   :json json/read-str
   :os (System/getProperty "os.name")
   :arch (System/getProperty "os.arch")
   :env #(System/getenv %)
   :exe (str (fs/real-path
               (-> (java.lang.ProcessHandle/current) .info .command .get)))})
