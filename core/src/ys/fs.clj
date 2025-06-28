;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.fs
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [java-time.api :as jtime])
  (:refer-clojure :exclude [abs empty? find]))

(def this 'ys.fs)
(def TRUE (constantly true))

(defn- multi [func]
  (fn [& paths]
    (let [sequential (and (= 1 (count paths))
                       (sequential? (first paths)))
          associative (and (= 1 (count paths))
                        (associative? (first paths)))
          path-list (cond
                      sequential (first paths)
                      associative (keys (first paths))
                      :else paths)
          return (for [path path-list] (func path))]
      (cond
        sequential return
        associative (zipmap (keys paths) return)
        (= 1 (count return)) (first return)
        :else return))))

(defn- when-str [x]
  (if x (str x) nil))

(declare
  dir?
  file?
  filename
  ls
  size?
  )

;; FS boolean functions
(intern this 'abs? (multi fs/absolute?))
(intern this 'dir? (multi fs/directory?))
(intern this 'empty? (multi
                       (fn [path]
                         (condf path
                           file? (-> (not (size? path)))
                           dir? (-> path ls seq not)
                           TRUE
                           (die "empty? called with non-file, non-dir path: '"
                             path "'")))))
(intern this 'exec? fs/executable?)
(intern this 'exists? fs/exists?)
(intern this 'file? fs/regular-file?)
(intern this 'link? fs/sym-link?)
(intern this 'read? fs/readable?)
(intern this 'rel? fs/relative?)
(intern this 'size? (multi (fn [path] (not= 0 (fs/size path)))))
(intern this 'write? fs/writable?)

;; FS boolean short functions
(intern this 'd dir?)
(intern this 'e exists?)
(intern this 'f file?)
(intern this 'l link?)
(intern this 'r read?)
(intern this 's size?)
(intern this 'w write?)
(intern this 'x exec?)
(intern this 'z empty?)

;; FS getters
(intern this 'abs (multi #(-> %1 fs/absolutize str)))
(intern this 'basename (multi #(-> %1 fs/canonicalize fs/file-name str)))
(intern this 'ctime (multi #(fs/file-time->millis (fs/last-modified-time %1))))
(intern this 'cwd #(str (fs/cwd)))
(intern this 'dirname (multi #(-> %1 fs/canonicalize fs/parent str)))
(intern this 'filename
  (fn
    ([path] (fs/file-name path))
    ([path ext]
     (if (= ext "*")
       (str/replace (filename path) #"(\w)\.\w{1,16}$" "$1")
       (fs/strip-ext (filename path) {:ext (str/replace ext #"^\." "")})))))
(intern this 'find
  (fn [path]
    (filter #(re-find #"/" %1)
      (map str
        (file-seq (io/file path))))))
(intern this 'glob (multi #(map str (fs/glob "." %1))))
(intern this 'ls #(map str (fs/list-dir %1)))
(intern this 'mtime (multi #(fs/file-time->millis (fs/last-modified-time %1))))
(intern this 'path (multi #(-> %1 fs/canonicalize)))
(intern this 'readlink (multi #(-> %1 fs/read-link)))
(intern this 'rel fs/relativize)
(intern this 'rel (multi #(str (fs/relativize (fs/cwd) (fs/canonicalize %1)))))
(intern this 'which (multi #(-> %1 fs/which when-str)))

;; FS mutators
(intern this 'cp #(str (fs/copy %1 %2)))
(intern this 'cp-r #(str (fs/copy-tree %1 %2)))
(intern this 'mkdir (multi #(-> %1 fs/canonicalize fs/create-dir str)))
(intern this 'mkdir-p (multi #(-> %1 fs/canonicalize fs/create-dirs str)))
(intern this 'mv #(str (fs/move %1 %2)))
(intern this 'rm (multi #(-> %1 fs/canonicalize fs/delete boolean not)))
(intern this 'rm-f (multi #(-> %1 fs/canonicalize fs/delete-if-exists boolean)))
(intern this 'rm-r (multi #(-> %1 fs/canonicalize fs/delete-tree boolean)))
(intern this 'rmdir (multi #(-> %1 fs/canonicalize fs/delete not)))
(intern this 'touch (multi #(str
                              (if (exists? %1)
                                (fs/set-last-modified-time %1 (jtime/instant))
                                (fs/create-file %1)))))

(comment
  )
