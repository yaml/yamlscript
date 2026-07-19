;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The ys.v0.common namespace holds small helpers shared by the compiler,
;; runtime, and module loader. The :glj reader conditional branches keep
;; it loadable on glojure, which has no java.io or ProcessHandle interop.

(ns ys.v0.common
  (:require
   #?@(:glj [] :default [[clojure.java.io :as io]])
   [clojure.string :as str]
   [ys.v0.debug]
   [ys.v0.util :as util]))

(defn cwd
  "Return the current working directory."
  []
  (System/getProperty "user.dir"))

(defn abspath
  "Return an absolute path, resolving relative paths from base."
  ([path] (abspath path (cwd)))
  ([path base]
   #?(:glj
      (if (str/starts-with? path "/")
        path
        (str base "/" path))
      :default
      (if (-> path io/file .isAbsolute)
        path
        (.getAbsolutePath (io/file (abspath base) path))))))

(def ^:private atom-type (type (atom nil)))

(defn atom?
  "Return true when x is a Clojure atom."
  [x]
  (= (type x) atom-type))

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
  #?(:glj
     (let [path (str/replace path #"/+$" "")]
       (if (str/includes? path "/")
         (let [parent (str/replace path #"/[^/]*$" "")]
           (if (= "" parent) "/" parent))
         "."))
     :default
     (->
       path
       io/file
       .getParent
       (or "."))))

#?(:glj
   (do
     (defn get-process-handle [] nil)
     (defn get-process-info [] nil)
     (defn get-cmd-path [] nil)
     (defn get-cmd-bin [] nil)
     (defn get-cmd-args [] [])
     (defn get-cmd-pid [] nil))
   :default
   (do
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
       (-> ^java.lang.ProcessHandle (get-process-handle) .pid))))

(defn get-yspath
  "Return yspath for the current context."
  [base]
  (let [yspath (or
                 (get (System/getenv) "YSPATH")
                 (when (re-matches #"/NO-NAME$" base) (cwd))
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

(def ^:private regex-type (type #""))

(defn regex?
  "Return true when x is a regular expression pattern."
  [x]
  (= (type x) regex-type))

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
