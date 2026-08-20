;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The ys.v0.ys namespace is the standard library's bridge to the YS
;; compiler and runtime. Under the ys runtime the real implementations are
;; installed into the hooks below. Under plain Clojure runtimes (babashka,
;; JVM Clojure) the compiler-dependent functions die with a clear message,
;; while the portable ones use plain Clojure implementations.

(ns ys.v0.ys
  (:require
   [clojure.string :as str]
   [ys.v0.common :refer [get-yspath]]
   [ys.v0.global :as global]
   [ys.v0.re :as re]
   [ys.v0.util :as util])
  (:refer-clojure
   :exclude [compile
             eval
             load-file
             use]))

(def hooks
  "Implementation hooks installed by the ys runtime."
  (atom {}))

(defn- hook [key & args]
  (if-let [f (get @hooks key)]
    (apply f args)
    (util/die
      (str "ys/" (name key) " requires the ys runtime "
        "(not available in this Clojure runtime)"))))

(defn compile [code]
  (hook :compile code))

(defn eval
  ([ys-code] (hook :eval ys-code))
  ([ys-code file stream-mode] (hook :eval ys-code file stream-mode)))

(defn eval-stream [ys-code]
  (hook :eval-stream ys-code))

(defn load-file [ys-file]
  (hook :load-file ys-file))

(defn load-url [url]
  (hook :load-url url))

(defn- default-load-pod [args]
  (let [load-pod (requiring-resolve 'babashka.pods/load-pod)]
    (apply load-pod args)))

(defn load-pod [& args]
  (if-let [f (get @hooks :load-pod)]
    (f args)
    (default-load-pod args)))

(defn unload-pods []
  (when-let [f (get @hooks :unload-pods)]
    (f)))

(defn- default-def-vars [ns m force]
  (let [ns (condf ns
             #(instance? clojure.lang.Namespace %1) ns
             string? (create-ns (symbol ns))
             symbol? (create-ns ns)
             (util/die (str "Invalid namespace for set-vars: '" ns "'")))]
    (reduce-kv
      (fn [_ k v]
        (let [key (condf k
                    string? k
                    keyword? (name k)
                    symbol? (name k)
                    (util/die (str "Invalid key for set-vars: '" k "'")))
              key (str/replace key #"_" "-")
              _ (when-not (re-matches re/symw key)
                  (util/die (str "Invalid key for set-vars: '" key "'")))
              key (symbol key)]
          (when (and (not force)
                  (ns-resolve ns key))
            (util/die
              (str "Variable '" key
                "' already defined in namespace '" (ns-name ns) "'")))
          (intern ns key v)))
      nil m)
    nil))

(defn +def-vars
  ([ns m]
   (+def-vars ns m false))
  ([ns m force]
   (let [m (dissoc m "_" '_)]
     (if-let [f (get @hooks :+def-vars)]
       (f ns m force)
       (default-def-vars ns m force)))))

(defmacro def-vars-from-map [ns m]
  `(let [[m# ns#] (if (~m "+")
                    [(dissoc ~m "+") (symbol (~m "+"))]
                    [~m ~ns])]
     (+def-vars ns# m#)))

(def source-options #{:path :file :url :deps})
(def selection-options #{:as :get :all :none :not})
(def use-options (into source-options selection-options))

(defn- use-option-value [option args pred description]
  (let [value (second args)]
    (when-not (and value (pred value))
      (util/die (str "Invalid 'use' option '" option
                  "': expected " description)))
    value))

(defn- use-option-symbols [option args]
  (let [[symbols more] (split-with (complement keyword?) (rest args))]
    (when-not (and (seq symbols) (every? symbol? symbols))
      (util/die (str "Invalid 'use' option '" option
                  "': expected at least one symbol")))
    [symbols more]))

(defn- validate-use-options [options]
  (when (and (:none options) (some options [:get :all :not]))
    (util/die "Invalid 'use' options: ':none' cannot be combined with "
      "':get', ':all', or ':not'"))
  (when (and (:get options) (:all options))
    (util/die "Invalid 'use' options: ':get' cannot be combined with ':all'"))
  (when (and (:get options) (:not options))
    (util/die "Invalid 'use' options: ':get' cannot be combined with ':not'"))
  (if (some options [:as :get :all :none :not])
    options
    (assoc options :all true)))

(defn- parse-use-args [args]
  (loop [args (seq args) options {}]
    (if-not args
      (validate-use-options options)
      (let [option (first args)]
        (when-not (and (keyword? option) (use-options option))
          (util/die (str "Invalid 'use' option '" option "'")))
        (when (contains? options option)
          (util/die (str "Duplicate 'use' option '" option "'")))
        (cond
          (source-options option)
          (let [value (use-option-value option args string? "one string")]
            (when-let [[source] (:from options)]
              (util/die (str "Invalid 'use' option '" option
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
              (util/die
                "Invalid 'use' option ':not': expected plain symbols"))
            (recur (seq more) (assoc options option (vec symbols))))

          :else
          (recur (next args) (assoc options option true)))))))

(defn- resolve-bb-add-classpath []
  (try
    (require 'babashka.classpath)
    (resolve 'babashka.classpath/add-classpath)
    (catch Throwable _ nil)))

(defn- add-jvm-load-path [path]
  (try
    (clojure.core/eval
      (read-string
        (str
          "(let [thread (Thread/currentThread) "
          "loader (clojure.lang.DynamicClassLoader. "
          "(.getContextClassLoader thread)) "
          "file (java.io.File. " (pr-str path) ")] "
          "(.addURL loader (.toURL (.toURI file))) "
          "(.setContextClassLoader thread loader))")))
    true
    (catch Throwable _ false)))

(defn- add-load-paths [paths]
  (let [add-load-path (resolve 'add-load-path)
        add-classpath (when-not add-load-path
                        (resolve-bb-add-classpath))]
    (doseq [path paths]
      (cond
        add-load-path (add-load-path path)
        add-classpath (add-classpath path)
        (add-jvm-load-path path) nil
        :else
        (util/die "This Clojure runtime cannot load modules from paths")))))

(defn- resolve-require-deps []
  (try
    (require 'clojurestar.deps)
    (resolve 'clojurestar.deps/require-deps*)
    (catch Throwable error
      (if (some resolve
            '[*glojure-version* *jolt-version* *gobb-version*])
        (throw error)
        nil))))

(defn- load-classpath-dependency [module]
  (try
    (require module)
    (catch Throwable _
      (util/die
        (str "Portable 'use :deps' cannot acquire dependencies in this "
          "runtime; put namespace '" module "' on the classpath")))))

(defn- load-portable-module [module options]
  (let [[kind spec] (or (:from options) [:yspath (get-yspath *file*)])]
    (case kind
      :yspath
      (do
        (add-load-paths spec)
        (require module))

      :path
      (do
        (add-load-paths [spec])
        (require module))

      :file
      (do
        (when (str/ends-with? spec ".ys")
          (util/die "Portable 'use :file' does not support .ys files"))
        (when-not (or (str/ends-with? spec ".clj")
                    (str/ends-with? spec ".cljc"))
          (util/die
            "Invalid 'use' option ':file': expected a .clj or .cljc file"))
        (clojure.core/load-file spec))

      :url
      (do
        (when-not (str/starts-with? spec "https://")
          (util/die "Invalid 'use' option ':url': expected an HTTPS URL"))
        (load-string (slurp spec)))

      :deps
      (let [dependency-options
            (cond-> {}
              (get global/ENV "YS_MAVEN_REPOSITORY")
              (assoc :mvn/local-repo
                (get global/ENV "YS_MAVEN_REPOSITORY"))

              (get global/ENV "YS_GITLIBS_DIR")
              (assoc :gitlibs/dir (get global/ENV "YS_GITLIBS_DIR")))]
        (if-let [require-deps (resolve-require-deps)]
          (require-deps dependency-options [spec])
          (load-classpath-dependency module)))))
  (when-not (find-ns module)
    (util/die (str "Namespace not found: " module))))

(defn- select-portable-vars [module options]
  (when-let [alias (:as options)]
    (clojure.core/alias alias module))
  (when-let [symbols (:get options)]
    (let [only (mapv #(if (namespace %1)
                        (symbol (namespace %1))
                        %1)
                 symbols)
          rename (into {}
                   (keep #(when-let [old (namespace %1)]
                            [(symbol old) (symbol (name %1))]))
                   symbols)]
      (refer module :only only :rename rename)))
  (when (or (:all options) (:not options))
    (refer module :exclude (vec (:not options)))))

(defn- portable-use [ns forms]
  (when-not (seq forms)
    (util/die "use requires at least one form"))
  (let [forms (if (symbol? (first forms)) (list forms) forms)]
    (binding [*ns* ns]
      (doseq [form forms]
        (let [module (first form)
              options (parse-use-args (rest form))]
          (load-portable-module module options)
          (select-portable-vars module options)))))
  nil)

(defn +use [ns forms]
  (if-let [f (get @hooks :+use)]
    (f ns forms)
    (portable-use ns forms)))

(defmacro use [& forms]
  `(+use *ns* '~forms))

(comment
  )
