;; Copyright 2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.util.install
  (:require [clojure.string :as str])
  (:refer-clojure :exclude [run!]))

;; Installation policy is shared by the native engines. The context supplies
;; run (argv -> exit/out/err), entries (directory -> names), json, env and exe.
(def release-api
  "https://api.github.com/repos/yaml/yamlscript/releases")

(def data-json-version "2.4.0")

(defn fail [message]
  (throw (ex-info message {})))

(defn run! [ctx & argv]
  (let [{:keys [exit out err]} ((:run ctx) (vec argv))]
    (when-not (= 0 exit)
      (fail (str (first argv) " failed (" exit "): " (str/trim err))))
    out))

(defn test-path [ctx flag path]
  (= 0 (:exit ((:run ctx) ["test" flag path]))))

(defn parent [path]
  (let [dir (str/replace path #"/[^/]+/?$" "")]
    (if (empty? dir) "/" dir)))

(defn setting [ctx name]
  (let [value ((:env ctx) name)]
    (when (seq value) value)))

(defn say [ctx message]
  (when-not (setting ctx "QUIET") (println message)))

(defn platform [ctx]
  (let [os (or (:os ctx) (str/trim (run! ctx "uname" "-s")))
        arch (or (:arch ctx) (str/trim (run! ctx "uname" "-m")))
        os ({"Linux" "linux" "Darwin" "macos" "Mac OS X" "macos"
             "linux" "linux" "darwin" "macos"} os)
        arch ({"x86_64" "x64" "amd64" "x64"
               "arm64" "arm64" "aarch64" "arm64"} arch)]
    (when-not (and os arch)
      (fail "Installation supports Linux and macOS on x64 and ARM64."))
    [os arch]))

(defn absolute [ctx path]
  (if (str/starts-with? path "/") path
    (str (str/trim (run! ctx "pwd" "-P")) "/" path)))

(defn prefix [ctx]
  (absolute ctx (or (setting ctx "PREFIX") (parent (parent (:exe ctx))))))

(defn temp-dir [ctx base]
  (str/trim (run! ctx "mktemp" "-d" (str base "/.ys-install.XXXXXX"))))

(defn cleanup [ctx dir]
  (run! ctx "rm" "-rf" dir))

(defn install-file [ctx source target]
  (let [dir (parent target)]
    (run! ctx "mkdir" "-p" dir)
    (when (test-path ctx "-d" target)
      (fail (str "Cannot replace directory: " target)))
    (let [stage (temp-dir ctx dir)]
      (try
        (run! ctx "cp" "-pP" source (str stage "/payload"))
        ;; A same-filesystem rename also replaces a running executable.
        (run! ctx "mv" "-f" (str stage "/payload") target)
        (finally (cleanup ctx stage))))))

(defn install-tree [ctx source target]
  (run! ctx "mkdir" "-p" target)
  (doseq [name ((:entries ctx) source)]
    (let [from (str source "/" name) to (str target "/" name)]
      (if (and (test-path ctx "-d" from)
               (not (test-path ctx "-L" from)))
        (install-tree ctx from to)
        (install-file ctx from to)))))

(defn download [ctx url target]
  (run! ctx "curl" "-fsSL" "--retry" "2" "-o" target url))

(defn release-info [ctx]
  (let [version (setting ctx "VERSION")
        _ (when (and version
                     (not (re-matches #"[0-9]+\.[0-9]+\.[0-9]+" version)))
            (fail (str "Invalid VERSION: " version)))
        url (str release-api (if version (str "/tags/" version) "/latest"))
        info ((:json ctx) (run! ctx "curl" "-fsSL" url))]
    (when-not (re-matches #"[0-9]+\.[0-9]+\.[0-9]+"
                (or (get info "tag_name") ""))
      (fail "Release metadata has no valid version."))
    info))

(defn release-asset [info name [os arch]]
  (let [version (get info "tag_name")
        arches (if (= arch "arm64") ["arm64" "aarch64"] [arch])
        names (map #(str name "-" version "-" os "-" % ".tar.xz") arches)]
    (or (some (fn [name]
                (some #(when (= name (get % "name")) %) (get info "assets")))
          names)
      (fail (str "Release " version " has no " name " asset for "
              os " " arch)))))

(defn archive-info [path]
  (let [name (last (str/split path #"/"))
        match (re-matches
                #"(ys|libys)-([0-9]+\.[0-9]+\.[0-9]+)-(linux|macos)-(x64|arm64|aarch64)\.tar\.xz"
                name)]
    (when-not match (fail (str "Unsupported release archive: " name)))
    {:name (nth match 1) :version (nth match 2)
     :platform [(nth match 3)
                (if (= "aarch64" (nth match 4)) "arm64" (nth match 4))]
     :root (str/replace name #"\.tar\.xz$" "")}))

(defn unpack [ctx archive dir root]
  ;; Glojure's split-lines retains the trailing empty line from tar's output.
  (let [entries (remove str/blank?
                  (str/split-lines (run! ctx "tar" "-tf" archive)))]
    (when (or (empty? entries)
            (some #(or (str/starts-with? % "/")
                       (some #{".."} (str/split % #"/"))
                       (not (or (= % root)
                                (str/starts-with? % (str root "/")))))
              entries))
      (fail "Archive contains paths outside its release directory.")))
  (run! ctx "tar" "-xf" archive "-C" dir)
  (str dir "/" root))

(defn extract-runtime [ctx repository version]
  (let [jar (str repository "/org/yamlscript/ys.v0/" version
             "/ys.v0-" version ".jar")]
    (when (and (test-path ctx "-f" jar)
               (not (test-path ctx "-e" (str jar ".d")))
               (= 0 (:exit ((:run ctx) ["unzip" "-v"]))))
      (let [stage (temp-dir ctx (parent jar))]
        (try
          (run! ctx "unzip" "-o" "-q" jar "-d" (str stage "/contents"))
          (run! ctx "mv" (str stage "/contents") (str jar ".d"))
          (finally (cleanup ctx stage)))))))

(defn m2-directory [ctx]
  (str (or (setting ctx "HOME")
           (fail "HOME is required for Maven installation."))
    "/.m2/repository"))

(defn install-bundled-m2 [ctx source version]
  (when (and (test-path ctx "-d" source) (not= "0" (setting ctx "M2")))
    (if (= "0" (str/trim (run! ctx "id" "-u")))
      (say ctx (str "Not installing the ys.v0 jars into ~/.m2 "
                 "(running as root).\n"
                 "Run 'ys --install-m2' as a normal user to enable "
                 "Java-free 'ys -T bb' scripts."))
      (let [repository (m2-directory ctx)]
        (install-tree ctx source repository)
        (extract-runtime ctx repository version)
        (say ctx "Installed the ys.v0 jars into ~/.m2/repository")))))

(defn payload-files [ctx source name version]
  (let [files ((:entries ctx) source)
        files (filter
                (if (= name "ys")
                  #(contains? #{"ys" "ys-0" (str "ys-" version)} %)
                  #(boolean
                     (re-matches #"libys[^/]*\.(so|dylib)(\.[0-9.]+)?" %)))
                files)]
    (when (or (empty? files)
            (and (= name "ys") (not (some #{(str "ys-" version)} files))))
      (fail (str "Archive is missing the " name " payload.")))
    (doseq [file files]
      (let [path (str source "/" file)]
        (when-not (test-path ctx "-f" path)
          (fail (str "Invalid release payload: " file)))
        (when (test-path ctx "-L" path)
          (let [target (str/trim (run! ctx "readlink" path))]
            (when-not (some #{target} files)
              (fail (str "Release link points outside its payload: " file)))))))
    ;; Publish real files before their aliases.
    (sort-by #(if (test-path ctx "-L" (str source "/" %)) 1 0) files)))

(defn install-payload [ctx source name version destination]
  (let [files (payload-files ctx source name version)
        subdir (if (= name "ys") "bin" "lib")]
    (doseq [file files]
      (install-file ctx (str source "/" file)
        (str destination "/" subdir "/" file)))
    (if (= name "ys")
      (install-bundled-m2 ctx (str source "/m2/repository") version)
      (doseq [file (filter #(str/ends-with? % ".h") ((:entries ctx) source))]
        (install-file ctx (str source "/" file)
          (str destination "/include/libys-" version "/" file))))
    (say ctx (str "Installed " name " " version " into " destination))))

(defn install-m2 [ctx version]
  (let [repository (m2-directory ctx)]
    (doseq [[path name repo]
            [[(str "org/yamlscript/ys.v0/" version) (str "ys.v0-" version)
              "https://repo.clojars.org"]
             [(str "org/clojure/data.json/" data-json-version)
              (str "data.json-" data-json-version)
              "https://repo1.maven.org/maven2"]]
            extension ["jar" "pom"]]
      (let [dir (str repository "/" path)
            file (str name "." extension)
            target (str dir "/" file)]
        (if (test-path ctx "-e" target)
          (say ctx (str "Already installed: " target))
          (do
            (run! ctx "mkdir" "-p" dir)
            (let [stage (temp-dir ctx dir)]
              (try
                (download ctx (str repo "/" path "/" file) (str stage "/file"))
                (install-file ctx (str stage "/file") target)
                (say ctx (str "Installed: " target))
                (finally (cleanup ctx stage))))))))
    (extract-runtime ctx repository version)))

(defn install-release [ctx command]
  (let [host (platform ctx)
        destination (prefix ctx)
        local (setting ctx "TARBALL")
        local (when local (absolute ctx local))
        info (when-not local (release-info ctx))
        names (cond local [(:name (archive-info local))]
                    (setting ctx "BIN") ["ys"]
                    (or (= command :install) (setting ctx "LIB")) ["libys"]
                    :else ["ys" "libys"])
        base (absolute ctx (or (setting ctx "TMPDIR") "/tmp"))
        stage (temp-dir ctx base)]
    (try
      ;; Download and validate every archive before changing the installation.
      (let [payloads
              (mapv
                (fn [name]
                  (let [asset (when-not local (release-asset info name host))
                        filename (if local local (get asset "name"))
                        details (archive-info filename)
                        archive (str stage "/" name ".tar.xz")
                        dir (str stage "/" name)]
                    (when-not (= host (:platform details))
                      (fail "Release archive does not match this platform."))
                    (if local
                      (run! ctx "cp" local archive)
                      (download ctx (get asset "browser_download_url") archive))
                    (run! ctx "mkdir" "-p" dir)
                    (let [source (unpack ctx archive dir (:root details))]
                      (payload-files ctx source name (:version details))
                      [source name (:version details)])))
                names)]
        (doseq [[source name version] payloads]
          (install-payload ctx source name version destination))
        (when (and (some #{"ys"} names)
                   (not (some #{(str destination "/bin")}
                          (str/split (or (setting ctx "PATH") "") #":"))))
          (say ctx (str "Add " destination "/bin to your PATH.")))
        (when (and (some #{"libys"} names)
                   (not (contains? #{"/usr/local"
                                      (str (setting ctx "HOME") "/.local")}
                          destination))
                   (not (some #{(str destination "/lib")}
                          (str/split
                            (or (setting ctx "LD_LIBRARY_PATH") "") #":"))))
          (say ctx (str "Add " destination "/lib to your LD_LIBRARY_PATH."))))
      (finally (cleanup ctx stage)))))

(defn run-installer [ctx command version]
  (platform ctx)
  (if (= command :install-m2)
    (install-m2 ctx version)
    (install-release ctx command)))
