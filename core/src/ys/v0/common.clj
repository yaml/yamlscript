;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The ys.v0.common namespace holds small helpers shared by the compiler,
;; runtime, and module loader.

(ns ys.v0.common
  (:require
   [babashka.fs :refer [cwd]]
   [clojure.java.io :as io]
   [clojure.stacktrace]
   [clojure.string :as str]
   [ys.v0.debug]
   [ys.v0.util :as util]))

;; Use for error messages at some point
#_(defn find-var-by-value [x]
  (let [all-the-vars (mapcat (fn [ns]
                               (vals (ns-publics ns)))
                             (all-ns))]
    (first (filter (fn [var]
                     (identical? x @var)) all-the-vars))))
#_(time (prn (meta (find-var-by-value inc))))

(defn abspath
  "Return an absolute path, resolving relative paths from base."
  ([path] (abspath path (str (cwd))))
  ([path base]
   (if (-> path io/file .isAbsolute)
     path
     (.getAbsolutePath (io/file (abspath base) path)))))

(defn atom?
  "Return true when x is a Clojure atom."
  [x]
  (= (type x) clojure.lang.Atom))

(defn chop
  "Drop N trailing items from a string or sequence."
  ([S] (chop 1 S))
  ([N S]
   (let [lst (drop-last N S)]
     (if (string? S)
       (str/join "" lst)
       lst))))

(defn dirname
  "Return the parent directory of a path, or . for no parent."
  [path]
  (->
    path
    io/file
    .getParent
    (or ".")))

(defn get-process-handle
  "Return process handle for the current context."
  []
  (java.lang.ProcessHandle/current))

(defn get-process-info
  "Return process info for the current context."
  []
  (-> ^java.lang.ProcessHandle (get-process-handle) .info))

(defn get-cmd-path
  "Return cmd path for the current context."
  []
  (-> ^java.lang.ProcessHandle$Info (get-process-info) .command .get))

(defn get-cmd-bin
  "Return cmd bin for the current context."
  []
  (-> ^String (get-cmd-path) io/file .getParent))

(defn get-cmd-args
  "Return cmd args for the current context."
  []
  (-> ^java.lang.ProcessHandle$Info
      (get-process-info)
      .arguments
      (.orElse (into-array String []))))

(defn get-cmd-pid
  "Return cmd pid for the current context."
  []
  (-> ^java.lang.ProcessHandle (get-process-handle) .pid))

(defn get-yspath
  "Return yspath for the current context."
  [base]
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

(defn re-find+
  "Reprocess find+ for YAMLScript parsing."
  [R S]
  (re-find R (str S)))

(defn regex?
  "Return true when x is a Java regular expression pattern."
  [x]
  (= (type x) java.util.regex.Pattern))

(intern 'clojure.core (with-meta 'TTT {:macro true}) @#'ys.v0.debug/TTT)
(intern 'clojure.core 'YSC ys.v0.debug/YSC)
(intern 'clojure.core 'YSC0 ys.v0.debug/YSC0)
(intern 'clojure.core 'DBG ys.v0.debug/DBG)
(intern 'clojure.core 'PPP ys.v0.debug/PPP)
(intern 'clojure.core 'WWW ys.v0.debug/WWW)
(intern 'clojure.core 'XXX ys.v0.debug/XXX)
(intern 'clojure.core 'YYY ys.v0.debug/YYY)
(intern 'clojure.core 'ZZZ ys.v0.debug/ZZZ)

(comment
  )
