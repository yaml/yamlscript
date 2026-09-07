;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.module.fs
  (:require [clojure.string :as str])
  (:refer-clojure :exclude [abs empty? find read]))

(defn- multi [function]
  (fn [& values]
    (let [value (first values)
          sequential (and (= 1 (count values)) (sequential? value))
          associative (and (= 1 (count values)) (associative? value))
          paths (cond
                  sequential value
                  associative (keys value)
                  :else values)
          results (map function paths)]
      (cond
        sequential results
        associative (zipmap paths results)
        (= 1 (count values)) (first results)
        :else results))))

(defn- stat [path]
  (let [[info error] (os.Stat (str path))]
    (when error (throw error))
    info))

(defn- mode-string [path]
  (str (.Mode (stat path))))

(defn read [path]
  (let [[bytes error] (os.ReadFile (str path))]
    (when error (throw error))
    (fmt.Sprintf "%s" bytes)))

(defn write [path content]
  (let [error
        (github.com:yaml:yamlscript:internal:goyamlparser.WriteTextFile
          (str path) (str content) 0644)]
    (when error (throw error))))

(def abs? (multi #(path:filepath.IsAbs (str %1))))
(def dir? (multi #(.IsDir (stat %1))))
(def exists?
  (multi
    #(let [[_ error] (os.Stat (str %1))]
       (nil? error))))
(def file? (multi #(not (.IsDir (stat %1)))))
(def link?
  (multi
    #(let [[info error] (os.Lstat (str %1))]
       (when error (throw error))
       (str/starts-with? (str (.Mode info)) "L"))))
(def read? (multi #(str/includes? (mode-string %1) "r")))
(def write? (multi #(str/includes? (mode-string %1) "w")))
(def exec? (multi #(str/includes? (mode-string %1) "x")))
(def rel? (multi #(not (path:filepath.IsAbs (str %1)))))
(def size? (multi #(not= 0 (.Size (stat %1)))))

(declare ls)

(def empty?
  (multi
    #(cond
       (file? %1) (not (size? %1))
       (dir? %1) (not (seq (ls %1)))
       :else
       (throw
         (ex-info
           (str "empty? called with non-file, non-dir path: '" %1 "'")
           {})))))

(def d dir?)
(def e exists?)
(def f file?)
(def l link?)
(def r read?)
(def s size?)
(def w write?)
(def x exec?)
(def z empty?)

(def abs
  (multi
    #(let [[path error] (path:filepath.Abs (str %1))]
       (when error (throw error))
       path)))

(def basename (multi #(path:filepath.Base (str %1))))
(def cwd
  (fn []
    (let [[path error] (os.Getwd)]
      (when error (throw error))
      path)))
(def dirname (multi #(path:filepath.Dir (str %1))))

(defn filename
  ([path]
   (path:filepath.Base (str path)))
  ([path extension]
   (let [filename (filename path)
         suffix (path:filepath.Ext filename)]
     (if (= extension "*")
       (if (seq suffix)
         (subs filename 0 (- (count filename) (count suffix)))
         filename)
       (let [extension (str/replace (str extension) #"^\." "")
             wanted (str "." extension)]
         (if (= suffix wanted)
           (subs filename 0 (- (count filename) (count suffix)))
           filename))))))

(defn- millis [path]
  (.UnixMilli (.ModTime (stat path))))

(def ctime (multi millis))
(def mtime (multi millis))
(def path (multi #(path:filepath.Clean (str %1))))

(defn ls [path]
  (let [[entries error] (os.ReadDir (str path))]
    (when error (throw error))
    (map #(path:filepath.Join (str path) (.Name %1)) entries)))

(defn- tree [path]
  (cons (str path)
    (when (dir? path)
      (mapcat tree (ls path)))))

(defn find [path]
  (rest (tree path)))

(def glob
  (multi
    #(let [[paths error] (path:filepath.Glob (str %1))]
       (when error (throw error))
       paths)))

(def readlink
  (multi
    #(let [[path error] (os.Readlink (str %1))]
       (when error (throw error))
       path)))

(def rel
  (multi
    #(let [[path error] (path:filepath.Rel (cwd) (str %1))]
       (when error (throw error))
       path)))

(def which
  (multi
    #(let [[path error] (os:exec.LookPath (str %1))]
       (when-not error path))))

(defn cp [source target]
  (let [content (read source)
        error
        (github.com:yaml:yamlscript:internal:goyamlparser.WriteTextFile
          (str target) content (.Perm (.Mode (stat source))))]
    (when error (throw error))
    (str target)))

(declare cp-r)

(defn cp-r [source target]
  (if (dir? source)
    (do
      (let [error (os.MkdirAll (str target) (.Perm (.Mode (stat source))))]
        (when error (throw error)))
      (doseq [child (ls source)]
        (cp-r child (path:filepath.Join (str target) (basename child))))
      (str target))
    (cp source target)))

(def mkdir
  (multi
    #(let [path (path:filepath.Clean (str %1))
           error (os.Mkdir path 0755)]
       (when error (throw error))
       path)))

(def mkdir-p
  (multi
    #(let [path (path:filepath.Clean (str %1))
           error (os.MkdirAll path 0755)]
       (when error (throw error))
       path)))

(defn mv [source target]
  (let [error (os.Rename (str source) (str target))]
    (when error (throw error))
    (str target)))

(def rm
  (multi
    #(let [error (os.Remove (path:filepath.Clean (str %1)))]
       (when error (throw error))
       true)))

(def rm-f
  (multi
    #(let [path (path:filepath.Clean (str %1))
           [_ stat-error] (os.Stat path)]
       (if stat-error
         false
         (let [error (os.Remove path)]
           (when error (throw error))
           true)))))

(def rm-r
  (multi
    #(let [error (os.RemoveAll (path:filepath.Clean (str %1)))]
       (when error (throw error))
       true)))

(def rmdir rm)

(def touch
  (multi
    #(let [path (str %1)]
       (if (exists? path)
         (let [now (time.Now)
               error (os.Chtimes path now now)]
           (when error (throw error)))
         (write path ""))
       path)))
