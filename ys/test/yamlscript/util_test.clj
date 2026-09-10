;; Copyright 2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.util-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clojure.data.json :as json]
   [clojure.test :refer [deftest is testing]]
   [yamlscript.util.install :as util]
   [yamlscript.util-platform :as platform]))

(defn context [dir]
  (assoc (platform/context)
    :exe (str dir "/prefix/bin/ys")
    :env {"HOME" (str dir "/home") "TMPDIR" (str dir) "QUIET" "1"}))

(defmacro with-directory [[dir ctx] & body]
  `(let [~dir (str (fs/create-temp-dir {:prefix "ys installer test "}))
         ~ctx (context ~dir)]
     (try ~@body (finally (fs/delete-tree ~dir)))))

(defn fixture [ctx dir name version]
  (let [[os arch] (util/platform ctx)
        root (str name "-" version "-" os "-" arch)
        source (str dir "/" root)
        archive (str source ".tar.xz")]
    (fs/create-dirs source)
    (if (= name "ys")
      (do
        (spit (str source "/ys-" version) "executable payload")
        (util/run! ctx "chmod" "755" (str source "/ys-" version))
        (fs/create-sym-link (str source "/ys") (str "ys-" version))
        (fs/create-sym-link (str source "/ys-0") (str "ys-" version))
        (spit (str source "/ys-sh-" version) "obsolete helper")
        (spit (str source "/ys-" version "-build-report.html") "report"))
      (do
        (spit (str source "/libys-glojure.so") "library payload")
        (fs/create-sym-link (str source "/libys.so") "libys-glojure.so")
        (spit (str source "/libys.h") "header")))
    (util/run! ctx "tar" "-cJf" archive "-C" dir root)
    archive))

(deftest release-selection
  (let [info {"tag_name" "0.2.33"
              "assets" [{"name" "ys-0.2.33-macos-aarch64.tar.xz"}
                        {"name" "ys-0.2.33-linux-x64-graalvm.tar.xz"}]}]
    (is (= "ys-0.2.33-macos-aarch64.tar.xz"
          (get (util/release-asset info "ys" ["macos" "arm64"]) "name")))
    (is (thrown-with-msg? Exception #"has no ys asset"
          (util/release-asset info "ys" ["linux" "x64"]))))
  (is (= ["macos" "arm64"]
        (:platform (util/archive-info "ys-0.2.32-macos-aarch64.tar.xz"))))
  (is (thrown? Exception (util/archive-info "unknown.tar.xz"))))

(deftest local-installation
  (with-directory [dir ctx]
    (let [archive (fixture ctx dir "ys" "0.2.32")
          ctx (update ctx :env assoc "TARBALL" archive)
          bin (str dir "/prefix/bin")]
      (util/run-installer ctx :upgrade "0.2.32")
      (is (= "executable payload" (slurp (str bin "/ys"))))
      (is (fs/executable? (str bin "/ys")))
      (is (fs/sym-link? (str bin "/ys")))
      (is (= #{"ys" "ys-0" "ys-0.2.32"}
            (set ((:entries ctx) bin))))
      (testing "same-version reinstall replaces aliases, not their targets"
        (spit (str dir "/unrelated") "untouched")
        (fs/delete (str bin "/ys"))
        (fs/create-sym-link (str bin "/ys") (str dir "/unrelated"))
        (util/run-installer ctx :upgrade "0.2.32")
        (is (= "untouched" (slurp (str dir "/unrelated"))))
        (is (= "executable payload" (slurp (str bin "/ys")))))
      (is (empty? (fs/glob dir "**/.ys-install.*"))))))

(deftest local-library-installation
  (with-directory [dir ctx]
    (let [archive (fixture ctx dir "libys" "0.2.32")
          destination (str dir "/custom prefix")
          ctx (update ctx :env assoc "TARBALL" archive "PREFIX" destination)]
      (util/run-installer ctx :install "0.2.32")
      (is (= "library payload" (slurp (str destination "/lib/libys.so"))))
      (is (= "header" (slurp (str destination "/include/libys-0.2.32/libys.h"))))
      (is (not (fs/exists? (str destination "/bin")))))))

(deftest download-failure-leaves-installation-alone
  (with-directory [dir ctx]
    (let [run (:run ctx)
          info {"tag_name" "0.2.33"
                "assets" [{"name" "ys-0.2.33-linux-x64.tar.xz"
                           "browser_download_url" "https://example.test/ys"}]}
          ctx (assoc ctx :os "Linux" :arch "amd64" :run
                (fn [argv]
                  (cond
                    (= ["uname" "-s"] argv) {:exit 0 :out "Linux"}
                    (= ["uname" "-m"] argv) {:exit 0 :out "x86_64"}
                    (= ["curl" "-fsSL" (str util/release-api "/latest")] argv)
                    {:exit 0 :out (json/write-str info)}
                    (= "curl" (first argv)) {:exit 22 :out "" :err "HTTP 404"}
                    :else (run argv))))]
      (is (thrown-with-msg? Exception #"HTTP 404"
            (util/run-installer ctx :upgrade "0.2.32")))
      (is (not (fs/exists? (str dir "/prefix"))))
      (is (empty? (fs/glob dir ".ys-install.*"))))))

(deftest failed-extraction-cleans-up
  (with-directory [dir ctx]
    (let [archive (fixture ctx dir "ys" "0.2.32")
          run (:run ctx)
          ctx (-> ctx
                (update :env assoc "TARBALL" archive)
                (assoc :run (fn [argv]
                              (if (= ["tar" "-xf"] (vec (take 2 argv)))
                                {:exit 2 :out "" :err "broken archive"}
                                (run argv)))))]
      (is (thrown-with-msg? Exception #"broken archive"
            (util/run-installer ctx :upgrade "0.2.32")))
      (is (empty? (fs/glob dir ".ys-install.*"))))))

(deftest maven-installation
  (with-directory [dir ctx]
    (let [run (:run ctx) downloads (atom [])
          ctx (assoc ctx :run
                (fn [argv]
                  (cond
                    (= "curl" (first argv))
                    (do (swap! downloads conj (last argv))
                        (spit (nth argv 5) "downloaded")
                        {:exit 0 :out ""})
                    (= "unzip" (first argv)) {:exit 1 :out "" :err "missing"}
                    :else (run argv))))]
      (util/run-installer ctx :install-m2 "0.2.32")
      (is (= 4 (count @downloads)))
      (is (= "downloaded"
            (slurp (str dir "/home/.m2/repository/org/yamlscript/ys.v0/"
                     "0.2.32/ys.v0-0.2.32.jar"))))
      (util/run-installer ctx :install-m2 "0.2.32")
      (is (= 4 (count @downloads))))))

(deftest root-skips-bundled-maven
  (with-directory [dir ctx]
    (let [run (:run ctx) source (str dir "/bundled")
          ctx (assoc ctx :run #(if (= ["id" "-u"] %)
                                {:exit 0 :out "0\n"} (run %)))]
      (fs/create-dirs source)
      (spit (str source "/resource") "jar")
      (util/install-bundled-m2 ctx source "0.2.32")
      (is (not (fs/exists? (str dir "/home")))))))

(deftest unsupported-platform
  (with-directory [dir ctx]
    (doseq [os ["windows" "freebsd" "wasip1"]]
      (is (thrown-with-msg? Exception #"supports Linux and macOS"
            (util/run-installer (assoc ctx :os os) :upgrade "0.2.32"))))))

(deftest bundled-maven-extraction
  (with-directory [dir ctx]
    (let [source (str dir "/bundled")
          path "org/yamlscript/ys.v0/0.2.32/ys.v0-0.2.32.jar"
          run (:run ctx)
          ctx (assoc ctx :run #(if (= ["id" "-u"] %)
                                {:exit 0 :out "1000\n"} (run %)))]
      (fs/create-dirs (fs/parent (str source "/" path)))
      (with-open [zip (java.util.zip.ZipOutputStream.
                       (java.io.FileOutputStream. (str source "/" path)))]
        (.putNextEntry zip (java.util.zip.ZipEntry. "runtime.clj"))
        (.write zip (.getBytes "runtime" "UTF-8"))
        (.closeEntry zip))
      (util/install-bundled-m2 (update ctx :env assoc "M2" "0") source "0.2.32")
      (is (not (fs/exists? (str dir "/home"))))
      (util/install-bundled-m2 ctx source "0.2.32")
      (is (= "runtime"
            (slurp (str dir "/home/.m2/repository/" path ".d/runtime.clj")))))))

(deftest online-selection-and-environment
  (with-directory [dir ctx]
    (let [ys (fixture ctx dir "ys" "0.2.33")
          libys (fixture ctx dir "libys" "0.2.33")
          downloads (atom []) urls (atom []) run (:run ctx)
          info {"tag_name" "0.2.33"
                "assets" (mapv (fn [archive]
                                  {"name" (str (fs/file-name archive))
                                   "browser_download_url" archive}) [ys libys])}
          ctx (assoc ctx :run
                (fn [argv]
                  (if (= "curl" (first argv))
                    (if (= 3 (count argv))
                      (do (swap! urls conj (last argv))
                          {:exit 0 :out (json/write-str info)})
                      (do (swap! downloads conj (last argv))
                          (run ["cp" (last argv) (nth argv 5)])))
                    (run argv))))]
      (doseq [[command settings expected]
              [[:upgrade {} [ys libys]]
               [:install {} [libys]]
               [:upgrade {"LIB" "1"} [libys]]
               [:install {"BIN" "1" "LIB" "1"} [ys]]]]
        (reset! downloads [])
        (util/run-installer (update ctx :env merge settings) command "0.2.32")
        (is (= expected @downloads)))
      (is (every? #{(str util/release-api "/latest")} @urls))
      (util/run-installer (update ctx :env assoc "VERSION" "0.2.33")
        :install "0.2.32")
      (is (= (str util/release-api "/tags/0.2.33") (last @urls)))
      (is (= (str (fs/cwd) "/relative prefix")
            (util/prefix (update ctx :env assoc "PREFIX" "relative prefix")))))))

(deftest native-self-upgrade
  ;; Set this to either native engine. Always upgrade to the Glojure payload.
  (when-let [binary (System/getenv "YS_INSTALL_TEST_BIN")]
    (with-directory [dir ctx]
      (let [archive (fixture ctx dir "ys" "0.2.32")
            details (util/archive-info archive)
            source (str dir "/" (:root details))
            destination (str dir "/prefix")
            executable (str destination "/bin/ys-0.2.32")
            native (str (fs/absolutize "bin/ys-0.2.32"))
            jar-path (str "org/yamlscript/ys.v0/0.2.32/ys.v0-0.2.32.jar")
            jar (str source "/m2/repository/" jar-path)
            env (merge (into {} (System/getenv))
                  {"TARBALL" archive "HOME" (str dir "/home")
                   "TMPDIR" dir "PREFIX" "" "VERSION" ""
                   "BIN" "" "LIB" "" "M2" "1" "QUIET" "1"})]
        (fs/copy native (str source "/ys-0.2.32") {:replace-existing true})
        (fs/create-dirs (fs/parent jar))
        (with-open [zip (java.util.zip.ZipOutputStream.
                         (java.io.FileOutputStream. jar))]
          (.putNextEntry zip (java.util.zip.ZipEntry. "runtime.clj"))
          (.write zip (.getBytes "runtime" "UTF-8"))
          (.closeEntry zip))
        (util/run! ctx "env" "XZ_OPT=-0" "tar" "-cJf" archive
          "-C" dir (:root details))
        (fs/create-dirs (str destination "/bin"))
        (fs/copy binary executable)
        (fs/create-sym-link (str destination "/bin/ys") "ys-0.2.32")
        (doseq [args [["--install-m2" "--upgrade"]
                     ["--install-m2" "-o" (str dir "/ignored")]]]
          (let [result @(process/process (into [executable] args)
                          {:env env :out :string :err :string})]
            (is (not= 0 (:exit result)))
            (is (re-find #"mutually exclusive" (str (:out result) (:err result))))))
        (is (not (fs/exists? (str dir "/ignored"))))
        (doseq [attempt (range 2)]
          (let [result @(process/process [(str destination "/bin/ys") "--upgrade"]
                          {:env env :out :string :err :string})]
            (is (= 0 (:exit result)) (str "Upgrade " attempt ": " (:err result)))
            (is (= (fs/size native) (fs/size executable)))
            (is (= "YS (YAMLScript) 0.2.32\n"
                  (util/run! ctx executable "--version")))))
        (is (not (fs/exists? (str destination "/bin/ys-sh-0.2.32"))))
        (when-not (= "0" (clojure.string/trim (util/run! ctx "id" "-u")))
          (is (= "runtime"
                (slurp (str dir "/home/.m2/repository/" jar-path
                         ".d/runtime.clj")))))
        (is (empty? (fs/glob dir "**/.ys-install.*")))))))
