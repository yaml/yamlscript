;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.deps namespace adapts Grenadine dependency acquisition to
;; the YAMLScript SCI runtime and provides SCI's dynamic source loader.

(ns yamlscript.deps
  (:require
   [babashka.http-client :as http]
   [babashka.process :as process]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [grenadine.gitlibs :as gitlibs]
   [grenadine.require-deps :as required]
   [grenadine.runtime :as grenadine]
   [yamlscript.compiler :as compiler]
   [yamlscript.constructor :as constructor])
  (:import
   [java.io ByteArrayOutputStream File FileInputStream InputStream]
   [java.nio.charset StandardCharsets]
   [java.nio.file CopyOption Files LinkOption OpenOption Path Paths
    StandardCopyOption]
   [java.nio.file.attribute FileAttribute]
   [java.security MessageDigest]
   [java.util.zip ZipInputStream]))

(defonce roots (atom []))
(defonce basis
  (atom {:libs {}
         :classpath {}
         :classpath-roots []
         :grenadine/loaded {}}))
(defonce required-state
  (atom {:coordinates {}
         :namespaces {}}))

(defn- path
  "Convert value to a java.nio.file.Path."
  ^Path [value]
  (Paths/get (str value) (make-array String 0)))

(defn- read-all
  "Read an input stream into a byte array."
  [^InputStream stream]
  (with-open [input stream
              output (ByteArrayOutputStream.)]
    (io/copy input output)
    (.toByteArray output)))

(defn- response-bytes
  "Normalize an HTTP response body to bytes."
  [body]
  (cond
    (nil? body) nil
    (bytes? body) body
    (string? body) (.getBytes ^String body StandardCharsets/UTF_8)
    (instance? InputStream body) (read-all body)
    :else
    (throw
      (ex-info (str "Unsupported HTTP response body: " (type body))
        {:body-type (type body)}))))

(defn- http-get
  "Fetch URL and return the response shape expected by Grenadine."
  [url]
  (try
    (let [response (http/get url {:throw false :as :bytes})
          body (:body response)]
      {:status (:status response)
       :headers (:headers response)
       :body (response-bytes body)})
    (catch Throwable _
      {:status 0 :headers {} :body nil})))

(defn- digest
  "Return a lowercase hexadecimal digest for bytes."
  [algorithm ^bytes bytes]
  (let [name (case algorithm :sha1 "SHA-1" :sha256 "SHA-256")
        result (.digest (MessageDigest/getInstance name) bytes)]
    (apply str (map #(format "%02x" (bit-and 255 %1)) result))))

(defn- atomic-move!
  "Move a file atomically when supported by the filesystem."
  [from to]
  (try
    (Files/move
      (path from)
      (path to)
      (into-array StandardCopyOption
        [StandardCopyOption/ATOMIC_MOVE
         StandardCopyOption/REPLACE_EXISTING]))
    (catch Throwable _
      (Files/move
        (path from)
        (path to)
        (into-array StandardCopyOption
          [StandardCopyOption/REPLACE_EXISTING]))))
  nil)

(defn- delete-tree!
  "Delete a directory tree when it exists."
  [target]
  (let [file (File. (str target))]
    (when (.exists file)
      (doseq [child (reverse (file-seq file))]
        (when-not (.delete ^File child)
          (throw
            (ex-info (str "Unable to delete " child)
              {:path (str child)}))))))
  nil)

(defn- run-process
  "Run a subprocess and return Grenadine's process result shape."
  [{:keys [args dir env]}]
  (let [opts (cond-> {}
               dir (assoc :dir dir)
               env (assoc :extra-env env))]
    (select-keys (apply process/sh opts args) [:exit :out :err])))

(defn- extract-jar!
  "Safely extract a JAR into a digest-specific source directory."
  [jar destination]
  (let [marker (str destination "/.grenadine-complete")]
    (when-not (Files/exists (path marker) (make-array LinkOption 0))
      (let [temporary (str destination ".part")
            temporary-path (.normalize (.toAbsolutePath (path temporary)))]
        (delete-tree! temporary)
        (Files/createDirectories temporary-path (make-array FileAttribute 0))
        (try
          (with-open [input (ZipInputStream. (FileInputStream. (str jar)))]
            (loop [entry (.getNextEntry input)]
              (when entry
                (let [target (.normalize
                               (.resolve temporary-path (.getName entry)))]
                  (when-not (.startsWith target temporary-path)
                    (throw
                      (ex-info (str "Unsafe JAR entry: " (.getName entry))
                        {:entry (.getName entry)})))
                  (if (.isDirectory entry)
                    (Files/createDirectories
                      target (make-array FileAttribute 0))
                    (do
                      (when-let [parent (.getParent target)]
                        (Files/createDirectories
                          parent (make-array FileAttribute 0)))
                      (let [options
                            ^"[Ljava.nio.file.CopyOption;"
                            (into-array CopyOption
                              [StandardCopyOption/REPLACE_EXISTING])]
                        (Files/copy
                          ^InputStream input ^Path target options))))
                  (.closeEntry input)
                  (recur (.getNextEntry input))))))
          (Files/write ^Path
            (.resolve temporary-path ".grenadine-complete")
            ^bytes (byte-array 0)
            ^"[Ljava.nio.file.OpenOption;" (make-array OpenOption 0))
          (when (Files/exists (path destination) (make-array LinkOption 0))
            (delete-tree! destination))
          (atomic-move! temporary destination)
          (catch Throwable error
            (delete-tree! temporary)
            (throw error))))))
  destination)

(defn host
  "Return the host effects used by Grenadine under YAMLScript."
  []
  {:http-get http-get
   :read-bytes #(Files/readAllBytes (path %1))
   :write-bytes!
   (fn [target ^bytes bytes]
     (Files/write ^Path (path target) bytes
       ^"[Ljava.nio.file.OpenOption;" (make-array OpenOption 0))
     nil)
   :bytes->utf8 #(String. ^bytes %1 StandardCharsets/UTF_8)
   :utf8->bytes #(.getBytes ^String %1 StandardCharsets/UTF_8)
   :digest digest
   :byte-count (fn [^bytes value] (alength value))
   :exists? #(Files/exists (path %1) (make-array LinkOption 0))
   :directory? #(.isDirectory (File. (str %1)))
   :regular-file? #(.isFile (File. (str %1)))
   :find-files
   (fn [root predicate]
     (->> (file-seq (File. (str root)))
       (filter #(.isFile ^File %1))
       (map #(.getCanonicalPath ^File %1))
       (filter predicate)
       vec))
   :canonical-path #(.getCanonicalPath (File. (str %1)))
   :absolute-path #(.getAbsolutePath (File. (str %1)))
   :run-process run-process
   :read-edn edn/read-string
   :mkdirs!
   (fn [target]
     (Files/createDirectories (path target) (make-array FileAttribute 0))
     nil)
   :atomic-move! atomic-move!
   :delete! (fn [target] (Files/deleteIfExists (path target)) nil)
   :delete-tree! delete-tree!
   :extract-jar! extract-jar!
   :home-dir #(System/getProperty "user.home")
   :getenv #(System/getenv %1)})

(defn add-roots!
  "Append canonical source roots to SCI's dynamic search path."
  [new-roots]
  (let [new-roots (mapv #(.getCanonicalPath (File. (str %1))) new-roots)]
    (swap! roots #(vec (distinct (concat %1 new-roots)))))
  nil)

(defn- namespace-path
  "Convert a namespace symbol to its conventional source path."
  [namespace]
  (-> (str namespace)
    (str/replace "-" "_")
    (str/replace "." "/")))

(defn- source-file
  "Find namespace source in the registered roots."
  [namespace]
  (let [base (namespace-path namespace)]
    (some
      (fn [root]
        (some
          (fn [extension]
            (let [file (str root "/" base extension)]
              (when (.isFile (File. file)) file)))
          [".clj" ".cljc" ".ys"]))
      @roots)))

(defn load-fn
  "Load Clojure, portable Clojure, or YAMLScript source for SCI."
  [{:keys [namespace]}]
  (when-let [file (source-file namespace)]
    (let [source (slurp file)
          source (if (str/ends-with? file ".ys")
                   (binding [constructor/no-wrap true]
                     (compiler/compile source))
                   source)]
      {:file file :source source})))

(defn- environment-option
  "Return a non-empty environment option from a Grenadine host."
  [runtime-host name]
  (let [value ((:getenv runtime-host) name)]
    (when (seq value) value)))

(defn add-libs!
  "Resolve dependencies and append their extracted source roots."
  [libs]
  (let [runtime-host (host)
        maven-repository
        (environment-option runtime-host "YS_MAVEN_REPOSITORY")
        gitlibs-dir
        (environment-option runtime-host "YS_GITLIBS_DIR")]
    (grenadine/add-libs! basis add-roots! libs
      (cond-> {:host runtime-host}
        maven-repository (assoc :local-repo maven-repository)
        gitlibs-dir (assoc :gitlibs-dir gitlibs-dir)))))

(defn- required-host
  "Return the smaller host used by source-file coordinates."
  []
  (let [runtime-host (host)]
    {:home-dir (:home-dir runtime-host)
     :gitlibs-dir
     #(gitlibs/gitlibs-dir
        {:host runtime-host
         :gitlibs-dir
         (environment-option runtime-host "YS_GITLIBS_DIR")})
     :file-exists? (:regular-file? runtime-host)
     :mkdirs! (:mkdirs! runtime-host)
     :delete! (:delete! runtime-host)
     :atomic-move! (:atomic-move! runtime-host)
     :read-text
     (fn [file]
       ((:bytes->utf8 runtime-host) ((:read-bytes runtime-host) file)))
     :download!
     (fn [url file]
       (let [{:keys [status body]} ((:http-get runtime-host) url)]
         (when (and (<= 200 status 299) body)
           ((:write-bytes! runtime-host) file body)
           true)))}))

(defn- read-first-form
  "Read the namespace form from acquired Clojure source."
  [source]
  (binding [*read-eval* false]
    (read-string source)))

(defn- loaded-namespace
  "Return the namespace already loaded for a coordinate identity."
  [identity]
  (get-in @required-state [:coordinates identity]))

(defn- load-required!
  "Load one coordinate while preventing namespace identity conflicts."
  [coordinate namespace load!]
  (let [identity (:identity coordinate)
        loaded (get-in @required-state [:namespaces namespace])]
    (cond
      (= identity (:identity loaded)) namespace
      loaded (required/namespace-conflict! namespace loaded coordinate)
      :else
      (do
        (load!)
        (swap! required-state
          (fn [state]
            (-> state
              (assoc-in [:coordinates identity] namespace)
              (assoc-in [:namespaces namespace] coordinate))))
        namespace))))

(defn prepare-required!
  "Acquire a released require-deps coordinate and load its namespace."
  [coordinate require! load-file!]
  (or
    (loaded-namespace (:identity coordinate))
    (case (:provider coordinate)
      :mvn
      (load-required!
        coordinate (:namespace coordinate)
        #(do
           (add-libs!
             {(:lib coordinate) {:mvn/version (:version coordinate)}})
           (require! (:namespace coordinate))))

      :gist
      (let [{:keys [path source]}
            (required/acquire-gist! (required-host) {} coordinate)
            namespace
            (required/gist-namespace coordinate (read-first-form source))]
        (load-required! coordinate namespace #(load-file! path)))

      :github
      (let [{:keys [path source]}
            (required/acquire-github! (required-host) {} coordinate)
            namespace
            (required/github-namespace coordinate (read-first-form source))]
        (load-required! coordinate namespace #(load-file! path))))))

(comment
  )
