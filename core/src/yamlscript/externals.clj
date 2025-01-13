;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.externals
  (:require
   [babashka.pods.sci :as pods]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [sci.core :as sci]
   [yamlscript.cache :as cache]
   [yamlscript.common :refer [abspath dirname get-yspath]]
   [yamlscript.compiler]
   [yamlscript.constructor]
   [yamlscript.global :as G]
   [yamlscript.re :as re])
  (:refer-clojure
   :exclude [load-file]))

;; ----------------------------------------------------------------------------

(defn load-pod [args]
  (let [pod (apply pods/load-pod @G/sci-ctx args)]
    (swap! G/pods conj pod)))

(defn unload-pods []
  (doseq [pod @G/pods]
    (pods/unload-pod pod))
  (reset! G/pods []))

;; ----------------------------------------------------------------------------

;; XXX Duplicated logic from ys.ys/eval
(defn load-code-ys [code file]
  (let [code (binding [yamlscript.constructor/no-wrap true]
               (yamlscript.compiler/compile code))
        stream @G/stream-values
        _ (reset! G/stream-values [])
        ret (sci/binding
             [sci/file file
              G/FILE file]
              (sci/eval-string+ @G/sci-ctx code))
        _ (reset! G/stream-values stream)]
    (:val ret)))

(defn load-file-ys [file]
  (let [file (abspath file (dirname @sci/file))
        code (-> file slurp)]
    (load-code-ys code file)))

(defn load-code-clj [code file]
  (sci/binding
   [sci/file file
    G/FILE file]
    (:val (sci/eval-string+ @G/sci-ctx code))))

(defn load-file-clj [file]
  (let [file (abspath file (dirname @sci/file))
        code (-> file slurp)]
    (load-code-clj code file)))

(defn load-code-ys-or-clj
  ([code]
   (load-code-ys-or-clj code @sci/file))
  ([code file]
   (if (re-find #"^\s*[;()]" code)
     (load-code-clj code file)
     (load-code-ys code file))))

(defn load-file-ys-or-clj [module]
  (let [ys-file (str module ".ys")
        clj-file (str module ".clj")]
    (if (.exists (io/as-file clj-file))
      (do
        (load-file-clj clj-file)
        true)
      (when (.exists (io/as-file ys-file))
        (load-file-ys ys-file)
        true))))

(defn load-yspath [modpath yspath]
  (when (not (sci/find-ns @G/sci-ctx
               (symbol (str/replace modpath #"/" "."))))
    (loop [yspath yspath]
      (if (seq yspath)
        (let [[path & yspath] yspath]
          (if (load-file-ys-or-clj (str path "/" modpath))
            nil
            (recur yspath)))
        (die (str "Module not found: " (str/replace modpath #"/" "::")))))))

(defn load-path [modpath spec]
  (die ":path not implemented yet"))

(defn load-file [modpath spec]
  (die ":file not implemented yet"))

(defn load-mvn [modpath spec]
  (die ":mvn not implemented yet"))

(defn load-git [modpath spec]
  (die ":git not implemented yet"))

(defn github-raw-url [url]
  (let [url (subs url 7)
        parts (str/split url #"/")
        _ (when (< (count parts) 4)
            (die (str "Invalid github url: " url)))
        [user repo ref & path] parts
        ref (if (re-matches #"[0-9a-f]{40}" ref)
              ref (str "refs/heads/" ref))]
     (str "https://raw.githubusercontent.com/"
          (str/join "/" (concat [user repo ref] path)))))

(defn load-url [_ url]
  (let [url (cond
              (str/starts-with? url "github:") (github-raw-url url)
              (str/starts-with? url "http") url
              :else (die (str "Invalid url for ':url': " url)))]
    (-> url cache/curl load-code-ys-or-clj)))

(defn parse-args [args]
  (let [nargs (count args)]
    (loop [i 0, key nil, parsed {}]
      (if (>= i nargs)
        parsed
        (let [arg (nth args i)
              [k v] (condp = arg
                      :path [:path nil]
                      :file [:file nil]
                      :mvn [:mvn nil]
                      :git [:git nil]
                      :url [:url nil]
                      :as [:as nil]
                      :get [:get []]
                      :all [:all true]
                      :not [:not []]
                      [nil arg])
              parsed (if k
                       (if (some #{k} [:path :file :mvn :git :url])
                         (if-let [val (:from parsed)]
                           (let [key (first (keys val))]
                             (die (str "Invalid arg " k
                                    " when " key " is already set")))
                           (assoc parsed :from {k 0}))
                         (if (get parsed k)
                           (die (str "Can't set " k " twice"))
                           (assoc parsed k v)))
                       (if key
                         (if (= key :from)
                           (assoc-in parsed
                             [:from (first (keys (:from parsed)))] v)
                           (if (vector? (get parsed key))
                             (update parsed key conj v)
                             (assoc parsed key v)))
                         (die (str "Invalid arg for 'use': " arg))))
              key (cond
                    (= arg :all) nil
                    (= 0 (-> parsed :from vals first)) :from
                    k k
                    (some #{k} [:path :file :mvn :git :url]) key
                    (= :get key) :get
                    :else nil)]
          (if (< i nargs)
            (recur (inc i) key parsed)
            parsed))))))

(defn use-module [ns module args]
  (when (not (re-matches (re/re #"(?:$nspc|$symw)")
               (str/replace (str module) #"\." "::")))
    (die (str "Invalid module name: " module)))
  (let [module (str module)
        modpath (str/replace module #"\." "/")
        args (parse-args args)
        from (or (:from args) {:yspath (get-yspath @sci/file)})
        [kind spec] (first from)]
    (case kind
      :yspath (load-yspath modpath spec)
      :path (load-path modpath spec)
      :file (load-file modpath spec)
      :mvn (load-mvn modpath spec)
      :git (load-git modpath spec)
      :url (load-url modpath spec))
    (let [namespace-sym (symbol module)
          namespace (sci/find-ns @G/sci-ctx namespace-sym)]
      (when-not namespace
        (die (str "Namespace not found: " namespace-sym)))
      (when-let [as (:as args)]
        (sci/eval-string+ @G/sci-ctx
          (str "(alias '" as " '" namespace-sym ")")
          {:ns ns}))
      (when (:all args)
        (sci/eval-string+ @G/sci-ctx
          (str "(refer '" namespace-sym ")")
          {:ns ns}))
      (when-let [syms (:get args)]
        (let [syms (map str syms)
              [rename only] (if (some #(re-find #"\." %1) syms)
                              [(mapcat #(let [[o n] (str/split %1 #"\.")]
                                          [o (or n o)]) syms)
                               (map first (map #(str/split %1 #"\.") syms))]
                              [nil syms])
              code (str "(refer '" namespace-sym
                     " :only '[" (str/join " " only) "]"
                     (when rename
                       (str " :rename '{"
                         (str/join " " rename)
                         "}"))
                     ")")]
          (sci/eval-string+ @G/sci-ctx code {:ns ns})))
      (when-let [syms (:not args)]
        (let [syms (map str syms)]
          (sci/eval-string+ @G/sci-ctx
            (str "(refer '" namespace-sym
              " :exclude '[" (str/join " " syms) "])")
            {:ns ns})))
      nil)))

(comment
  )
