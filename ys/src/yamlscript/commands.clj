;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.commands library handles install, upgrade, and binary commands
;; that were previously delegated to shell scripts.

(ns yamlscript.commands
  (:require
   [babashka.fs :as fs]
   [babashka.process :refer [process]]
   [clojure.string :as str]
   [yamlscript.common :as common]
   [yamlscript.util :as util]
   [yamlscript.global :as global :refer [env]])
  (:refer-clojure))

(def yamlscript-version "0.2.4")

(defn o [& msgs]
  (util/eprintln (str "* " (str/join "\n  " msgs)))
  (flush))

(defn run [cmd-args err-msg]
  (when (env "YS_BINARY_DEBUG")
    (util/eprintln
      (str "Running: " (str/join " " (remove map? cmd-args)))))
  (let [result (apply process cmd-args)]
    (when (not= 0 (:exit @result))
      (die (str err-msg ":\n" (slurp (:err result)))))
    result))

(defn opts []
  (let [[arg1 arg2] (map str (common/get-cmd-args))
        ;; For testing with util/ysj
        devel? (boolean (and
                          (= arg1 "-jar")
                          (re-find #"yamlscript\.cli-" arg2)))
        clone-url (if devel? ".git" "https://github.com/yaml/yamlscript")
        clone-branch (or (env "YS_BINARY_BRANCH") yamlscript-version)
        temp-dir "/tmp/yamlscript"
        clone-work-dir (str temp-dir "/" (if devel? "devel" clone-branch))
        build-dir (str clone-work-dir "/binary/graalvm")
        to-tty {:out :inherit :err :inherit}
        install-prefix (or (env "PREFIX")
                         (-> (java.lang.ProcessHandle/current)
                           .info .command .get
                           fs/parent str))]
    {:devel? devel?
     :clone-url clone-url
     :clone-branch clone-branch
     :temp-dir temp-dir
     :clone-work-dir clone-work-dir
     :build-dir build-dir
     :to-tty to-tty
     :install-prefix install-prefix}))

(defn do-install [_opts _args]
  (let [install-prefix (:install-prefix (opts))]
    (o (str "Installing YAMLScript with PREFIX=" install-prefix))
    (run [{:extra-env {"PREFIX" install-prefix "LIB" "1"}}
          "bash" "-c" "curl -sS https://yamlscript.org/install | bash"]
      "Installation failed")))

(defn do-upgrade [_opts _args]
  (let [install-prefix (:install-prefix (opts))]
    (o (str "Upgrading YAMLScript with PREFIX=" install-prefix))
    (run [{:extra-env {"PREFIX" install-prefix}}
          "bash" "-c" "curl -sS https://yamlscript.org/install | bash"]
      "Upgrade failed")))

(defn setup-yamlscript-repo [opts]
  (let [{:keys [devel?
                clone-url
                clone-branch
                clone-work-dir
                build-dir]} opts]

    (o "Setting up YAMLScript repo for binary compilation")
    (let [clone-opts ["--depth=1"]
          clone-opts (if-not devel?
                       (conj clone-opts (str "--branch=" clone-branch))
                       clone-opts)
          clone-opts (conj clone-opts clone-url clone-work-dir)
          clone-cmd (concat ["git clone"] clone-opts)]
      (when (and devel? (fs/exists? clone-work-dir))
        (o (str "Deleting existing devel repo: " clone-work-dir))
        (fs/delete-tree clone-work-dir))
      (when-not (fs/exists? clone-work-dir)
        (o "Cloning YAMLScript repo into /tmp/:"
          (str "  " (str/join " " clone-cmd)))
        (run clone-cmd
          "Failed to clone YAMLScript repo")))

    ;; Reset and clean the repo
    (o "Cleaning the build environment")
    (run [{:dir build-dir} "make" "realclean"]
      "Failed to clean the build environment")))

(defn get-in-path [code in-file build-dir]
  ;; Handle inline code case and determine input path
  (if (= in-file "--eval")
    (let [eval-file (str build-dir "/eval.ys")]
      (spit eval-file code)
      eval-file)
    (str (fs/absolutize in-file))))

(defn do-build-graal [info]
  (let [{:keys [code in-file out-file ys-bin]} info
        opts (opts)
        {:keys [devel?
                build-dir
                to-tty]} opts
        ys-bin (if devel? "ys" ys-bin)
        _ (setup-yamlscript-repo opts)
        in-path (get-in-path code in-file build-dir)
        out-path (str (fs/absolutize out-file))]
    ;; Build the binary using the YAMLScript binary/graalvm system
    (o (str "Compiling '" in-file "' to binary '" out-file "'..."))
    (o "This may take a few minutes...")

    (run [(merge to-tty {:dir build-dir})
          "make" "build"
          (str "YS-PROGRAM-FILE=" in-path)
          (str "YS-BIN=" ys-bin)]
      "Build failed")

    ;; Move the resulting binary to the desired location
    (let [program-file (str build-dir "/program")]
      (when-not (fs/exists? program-file)
        (die "Build completed but binary not found"))

      (fs/copy program-file out-path {:replace-existing true})

      (o (str "Compiled '" in-file "' to binary '" out-file "'.")))))

(defn do-to-glj [info] info)

(defn do-to-go [info] info)

(defn do-build-gobin [info] info)

(defn do-build-wasm [info] info)
