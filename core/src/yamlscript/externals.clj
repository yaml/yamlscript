;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.externals namespace implements runtime loading for pods,
;; source files, YSPATH modules, and remote modules used by `use`.

(ns yamlscript.externals
  (:require
   [babashka.pods.sci :as pods]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [grenadine.require-deps :as required]
   [sci.core :as sci]
   [yamlscript.cache :as cache]
   [yamlscript.deps :as deps]
   [ys.v0.common :refer [abspath dirname get-yspath]]
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
  [root module]
  (some
    (fn [[extension loader clojure-name?]]
      (let [module (if clojure-name?
                     (str/replace module "-" "_")
                     module)
            file (str root "/" module extension)]
        (when (.isFile (io/as-file file))
          (loader file)
          true)))
    [[".clj" load-file-clj true]
     [".cljc" load-file-clj true]
     [".ys" load-file-ys false]]))

(defn load-yspath
  "Load yspath into the YAMLScript runtime."
  [modpath yspath]
  (deps/add-roots! yspath)
  (when (not (sci/find-ns @G/sci-ctx
               (symbol (str/replace modpath #"/" "."))))
    (loop [yspath yspath]
      (if (seq yspath)
        (let [[path & yspath] yspath]
          (if (load-file-ys-or-clj path modpath)
            nil
            (recur yspath)))
        (die (str "Module not found: " (str/replace modpath #"/" "::")))))))

(defn load-path
  "Load path into the YAMLScript runtime."
  [modpath spec]
  (let [root (abspath spec (dirname @sci/file))]
    (when-not (.isDirectory (io/as-file root))
      (die (str "Directory not found for ':path': " root)))
    (deps/add-roots! [root])
    (when-not (load-file-ys-or-clj root modpath)
      (die (str "Module not found in ':path': "
             (str/replace modpath #"/" "::"))))))

(defn- module-root
  "Return the source root implied by an exact module file."
  [file modpath]
  (let [file (str file)
        extension (some #(when (str/ends-with? file %1) %1)
                    [".clj" ".cljc" ".ys"])
        clojure-file? (some #{extension} [".clj" ".cljc"])
        modpath (if clojure-file?
                  (str/replace modpath "-" "_")
                  modpath)
        suffix (when extension (str "/" modpath extension))]
    (if (and suffix (str/ends-with? file suffix))
      (subs file 0 (- (count file) (count suffix)))
      (dirname file))))

(defn load-file
  "Load file into the YAMLScript runtime."
  [modpath spec]
  (let [file (abspath spec (dirname @sci/file))]
    (when-not (.isFile (io/as-file file))
      (die (str "File not found for ':file': " file)))
    (deps/add-roots! [(module-root file modpath)])
    (cond
      (str/ends-with? file ".ys") (load-file-ys file)
      (or (str/ends-with? file ".clj")
        (str/ends-with? file ".cljc")) (load-file-clj file)
      :else
      (die "Invalid 'use' option ':file': expected a .ys, .clj, or "
        ".cljc file"))))

(defn load-url
  "Load url into the YAMLScript runtime."
  [_ url]
  (when-not (re-find #"^https?://" url)
    (die "Invalid 'use' option ':url': expected an HTTP(S) URL"))
  (load-code-ys-or-clj (cache/curl url) url))

(defn load-deps
  "Load a released clojurestar.deps require coordinate."
  [ns _ coordinate]
  (let [coordinate (required/parse-coordinate coordinate)]
    (deps/prepare-required!
      coordinate
      (fn [namespace]
        (sci/eval-string+ @G/sci-ctx
          (str "(require '" namespace ")")
          {:ns ns}))
      load-file-clj)))

(def source-options #{:path :file :url :deps})
(def selection-options #{:as :get :all :none :not})
(def use-options (into source-options selection-options))

(defn- use-option-error
  "Report an invalid or unsupported use option."
  [option]
  (die (str "Invalid 'use' option '" option "'")))

(defn- use-option-value
  "Take and validate one value following a use option."
  [option args pred description]
  (let [value (second args)]
    (when-not (and value (pred value))
      (die (str "Invalid 'use' option '" option
             "': expected " description)))
    value))

(defn- use-option-symbols
  "Take one or more symbol values following a use option."
  [option args]
  (let [[symbols more] (split-with (complement keyword?) (rest args))]
    (when-not (and (seq symbols) (every? symbol? symbols))
      (die (str "Invalid 'use' option '" option
             "': expected at least one symbol")))
    [symbols more]))

(defn- validate-use-options
  "Reject conflicting use options and apply default refer behavior."
  [options]
  (when (and (:none options) (some options [:get :all :not]))
    (die "Invalid 'use' options: ':none' cannot be combined with "
      "':get', ':all', or ':not'"))
  (when (and (:get options) (:all options))
    (die "Invalid 'use' options: ':get' cannot be combined with ':all'"))
  (when (and (:get options) (:not options))
    (die "Invalid 'use' options: ':get' cannot be combined with ':not'"))
  (if (some options [:as :get :all :none :not])
    options
    (assoc options :all true)))

(defn parse-args
  "Parse YAMLScript use-form arguments into a normalized option map."
  [args]
  (loop [args (seq args) options {}]
    (if-not args
      (validate-use-options options)
      (let [option (first args)]
        (when-not (and (keyword? option) (use-options option))
          (use-option-error option))
        (when (contains? options option)
          (die (str "Duplicate 'use' option '" option "'")))
        (cond
          (source-options option)
          (let [value (use-option-value option args string? "one string")]
            (when-let [[source] (:from options)]
              (die (str "Invalid 'use' option '" option
                     "': source option '" source "' is already set")))
            (recur (nnext args) (assoc options :from [option value])))

          (= option :as)
          (let [alias (use-option-value
                        option args
                        #(and (symbol? %1) (nil? (namespace %1)))
                        "one symbol")]
            (recur (nnext args) (assoc options option alias)))

          (some #{option} [:get :not])
          (let [[symbols more] (use-option-symbols option args)]
            (when (and (= option :not) (some namespace symbols))
              (die "Invalid 'use' option ':not': expected plain symbols"))
            (recur (seq more) (assoc options option (vec symbols))))

          :else
          (recur (next args) (assoc options option true)))))))

(defn use-module
  "Load a module and apply alias, refer, get, or exclusion options."
  [ns module args]
  (when (not (re-matches (re/re #"(?:$nspc|$symw)")
               (str/replace (str module) #"\." "::")))
    (die (str "Invalid module name: " module)))
  (let [module (str module)
        modpath (str/replace module #"\." "/")
        args (parse-args args)
        [kind spec] (or (:from args) [:yspath (get-yspath @sci/file)])
        loaded-namespace
        (case kind
          :yspath (do (load-yspath modpath spec) nil)
          :path (do (load-path modpath spec) nil)
          :file (do (load-file modpath spec) nil)
          :url (do (load-url modpath spec) nil)
          :deps (load-deps ns modpath spec))
        namespace-sym (symbol module)]
    (when (and loaded-namespace (not= loaded-namespace namespace-sym))
      (die (str "Dependency namespace '" loaded-namespace
             "' does not match use module '" namespace-sym "'")))
    (let [namespace-sym (symbol module)
          namespace-object (sci/find-ns @G/sci-ctx namespace-sym)]
      (when-not namespace-object
        (die (str "Namespace not found: " namespace-sym)))
      (when-let [as (:as args)]
        (sci/eval-string+ @G/sci-ctx
          (str "(alias '" as " '" namespace-sym ")")
          {:ns ns}))
      (when-let [syms (:get args)]
        (let [only (mapv #(if (clojure.core/namespace %1)
                            (symbol (clojure.core/namespace %1))
                            %1)
                     syms)
              rename (into {}
                       (keep #(when-let [old (clojure.core/namespace %1)]
                                [(symbol old) (symbol (name %1))]))
                       syms)
              code (str "(refer '" namespace-sym
                     " :only '" (pr-str only)
                     (when (seq rename)
                       (str " :rename '" (pr-str rename)))
                     ")")]
          (sci/eval-string+ @G/sci-ctx code {:ns ns})))
      (when (or (:all args) (:not args))
        (let [syms (some->> (:not args) (map str))]
          (sci/eval-string+ @G/sci-ctx
            (str "(refer '" namespace-sym
              (when syms
                (str " :exclude '[" (str/join " " syms) "]"))
              ")")
            {:ns ns})))
      nil)))

(comment
  )
