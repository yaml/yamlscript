;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.externals namespace implements runtime loading for pods,
;; source files, YSPATH modules, and remote modules used by `use`.

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

(defn load-pod
  "Load pod into the YAMLScript runtime."
  [args]
  (let [pod (apply pods/load-pod @G/sci-ctx args)]
    (swap! G/pods conj pod)))

(defn unload-pods
  "Unload every loaded pod and clear the pod registry."
  []
  (doseq [pod @G/pods]
    (pods/unload-pod pod))
  (reset! G/pods []))

;; ----------------------------------------------------------------------------

;; XXX Duplicated logic from ys.ys/eval
(defn load-code-ys
  "Load code ys into the YAMLScript runtime."
  [code file]
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

(defn load-file-ys
  "Load file ys into the YAMLScript runtime."
  [file]
  (let [file (abspath file (dirname @sci/file))
        code (-> file slurp)]
    (load-code-ys code file)))

(defn load-code-clj
  "Load code clj into the YAMLScript runtime."
  [code file]
  (sci/binding
   [sci/file file
    G/FILE file]
    (:val (sci/eval-string+ @G/sci-ctx code))))

(defn load-file-clj
  "Load file clj into the YAMLScript runtime."
  [file]
  (let [file (abspath file (dirname @sci/file))
        code (-> file slurp)]
    (load-code-clj code file)))

(defn load-code-ys-or-clj
  "Load code ys or clj into the YAMLScript runtime."
  ([code]
   (load-code-ys-or-clj code @sci/file))
  ([code file]
   (if (re-find #"^\s*[;()]" code)
     (load-code-clj code file)
     (load-code-ys code file))))

(defn load-file-ys-or-clj
  "Load file ys or clj into the YAMLScript runtime."
  [module]
  (let [ys-file (str module ".ys")
        clj-file (str module ".clj")]
    (if (.exists (io/as-file clj-file))
      (do
        (load-file-clj clj-file)
        true)
      (when (.exists (io/as-file ys-file))
        (load-file-ys ys-file)
        true))))

(defn load-yspath
  "Load yspath into the YAMLScript runtime."
  [modpath yspath]
  (when (not (sci/find-ns @G/sci-ctx
               (symbol (str/replace modpath #"/" "."))))
    (loop [yspath yspath]
      (if (seq yspath)
        (let [[path & yspath] yspath]
          (if (load-file-ys-or-clj (str path "/" modpath))
            nil
            (recur yspath)))
        (die (str "Module not found: " (str/replace modpath #"/" "::")))))))

(defn load-path
  "Load path into the YAMLScript runtime."
  [modpath spec]
  (die ":path not implemented yet"))

(defn load-file
  "Load file into the YAMLScript runtime."
  [modpath spec]
  (die ":file not implemented yet"))

(defn load-mvn
  "Load mvn into the YAMLScript runtime."
  [modpath spec]
  (die ":mvn not implemented yet"))

(defn load-git
  "Load git into the YAMLScript runtime."
  [modpath spec]
  (die ":git not implemented yet"))

(defn github-raw-url
  "Convert a raw url shorthand into a raw URL."
  [url]
  (let [[path ref] (str/split url #"\@")
        ref (or ref "HEAD")
        [user repo path] (str/split path #"/" 3)
        _ (when-not (and user repo path)
            (die (str "Invalid github url: " url)))]
    (str/join "/" ["https://raw.githubusercontent.com"
                   user repo ref path])))

(defn github-gist-url
  "Convert a gist url shorthand into a raw URL."
  [url]
  (let [url (str/replace url #"/raw/?" "/")
        [path ref] (str/split url #"\@")
        [user gist-id path] (str/split path #"/" 3)
        _ (when-not (and user gist-id)
            (die (str "Invalid github gist url: " url)))]
    (str/join "/" (remove nil? ["https://gist.githubusercontent.com"
                                user gist-id "raw" ref path]))))

(defn convert-url
  "Convert url into its canonical form."
  [url]
  (cond
    (re-find #"^https?://" url) url
    (str/starts-with? url "gist:") (github-gist-url (subs url 5))
    (str/starts-with? url "github:") (github-raw-url (subs url 7))
    (str/starts-with? url "https:") url
    (str/starts-with? url "http:") url
    (not (str/includes? url ":")) (str "https://" url)
    :else (die (str "Invalid url for ':url': " url))))

(defn load-url
  "Load url into the YAMLScript runtime."
  [_ url]
  (-> url convert-url cache/curl load-code-ys-or-clj))

(defn parse-args
  "Parse YAMLScript use-form arguments into a normalized option map."
  [args]
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
                      :none [:none true]
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
                             (update parsed key conj v) (assoc parsed key v)))
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

(defn use-module
  "Load a module and apply alias, refer, get, or exclusion options."
  [ns module args]
  (when (not (re-matches (re/re #"(?:$nspc|$symw)")
               (str/replace (str module) #"\." "::")))
    (die (str "Invalid module name: " module)))
  (let [module (str module)
        modpath (str/replace module #"\." "/")
        args (parse-args args)
        all (:all args)
        args (cond
               (:none args) (dissoc args :all :none)
               (and
                 (not all)
                 (not (or
                        (:as args)
                        (:get args)
                        (:not args)))) (assoc args :all true)
               :else args)
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
              [rename only] (if (some #(re-find #"[:/]" %1) syms)
                              [(mapcat #(let [[o n] (str/split %1 #"[:/]")]
                                          [o (or n o)]) syms)
                               (map first (map #(str/split %1 #"[:/]") syms))]
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
