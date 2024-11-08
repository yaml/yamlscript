;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.common
  (:require
   [babashka.fs :refer [cwd]]
   [clojure.java.io :as io]
   [clojure.stacktrace]
   [clojure.string :as str]
   [yamlscript.debug]
   [yamlscript.util :as util]))

;; Use for error messages at some point
#_(defn find-var-by-value [x]
  (let [all-the-vars (mapcat (fn [ns]
                               (vals (ns-publics ns)))
                             (all-ns))]
    (first (filter (fn [var]
                     (identical? x @var)) all-the-vars))))
#_(time (prn (meta (find-var-by-value inc))))

(defn abspath
  ([path] (abspath path (str (cwd))))
  ([path base]
   (if (-> path io/file .isAbsolute)
     path
     (.getAbsolutePath (io/file (abspath base) path)))))

(defn atom? [x]
  (= (type x) clojure.lang.Atom))

(defn chop
  ([S] (chop 1 S))
  ([N S]
   (let [lst (drop-last N S)]
     (if (string? S)
       (str/join "" lst)
       lst))))

(defn dirname [path]
  (->
    path
    io/file
    .getParent
    (or ".")))

(defn get-process-handle []
  (java.lang.ProcessHandle/current))

(defn get-process-info []
  (-> ^java.lang.ProcessHandle (get-process-handle) .info))

(defn get-cmd-path []
  (-> ^java.lang.ProcessHandle$Info (get-process-info) .command .get))

(defn get-cmd-bin []
  (-> ^String (get-cmd-path) io/file .getParent))

(defn get-cmd-args []
  (-> ^java.lang.ProcessHandle$Info (get-process-info) .arguments .get))

(defn get-cmd-pid []
  (-> ^java.lang.ProcessHandle (get-process-handle) .pid))

(defn get-yspath [base]
  (let [yspath (or
                 (get (System/getenv) "YSPATH")
                 (when (re-matches #"/NO-NAME$" base) (str (cwd)))
                 (->
                   base
                   dirname
                   abspath))
        _ (when-not yspath
            (util/die "YSPATH environment variable not set"))]
    (str/split yspath #":")))

(defn regex? [x]
  (= (type x) java.util.regex.Pattern))

(intern 'clojure.core (with-meta 'TTT {:macro true}) @#'yamlscript.debug/TTT)
(intern 'clojure.core 'YSC yamlscript.debug/YSC)
(intern 'clojure.core 'DBG yamlscript.debug/DBG)
(intern 'clojure.core 'PPP yamlscript.debug/PPP)
(intern 'clojure.core 'WWW yamlscript.debug/WWW)
(intern 'clojure.core 'XXX yamlscript.debug/XXX)
(intern 'clojure.core 'YYY yamlscript.debug/YYY)
(intern 'clojure.core 'ZZZ yamlscript.debug/ZZZ)

(comment
  )
