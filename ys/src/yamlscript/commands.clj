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

(def show-log? (atom false))

(defn o [& msgs]
  (when @show-log?
    (util/eprintln (str "* " (str/join "\n  " msgs)))
    (flush)))

(defn run [cmd-args err-msg]
  (when (env "YS_BUILD_DEBUG")
    (util/eprintln
      (str "Running: " (str/join " " (remove map? cmd-args)))))
  (let [result (apply process cmd-args)]
    (when (not= 0 (:exit @result))
      (when (env "YS_BUILD_DEBUG")
        (WWW result))
      (die (str err-msg ":\n" (slurp (:err result)))))
    result))

(defn opts []
  (let [[arg1 arg2] (map str (common/get-cmd-args))
        ;; For testing with util/ysj
        devel? (boolean (and
                          (= arg1 "-jar")
                          (re-find #"yamlscript\.cli-" arg2)))
        clone-url (if devel? ".git" "https://github.com/yaml/yamlscript")
        clone-branch (or (env "YS_BUILD_BRANCH")
                       (if devel?
                         (-> (run ["git" "rev-parse" "--short" "HEAD"]
                               "Failed to get current commit hash")
                           :out slurp str/trim)
                         yamlscript-version))
        temp-dir "/tmp/yamlscript"
        clone-work-dir (str temp-dir "/" clone-branch)
        build-dir (str clone-work-dir "/build")
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
    (if-not (fs/exists? clone-work-dir)
      (let [clone-opts
            (if devel?  [] ["--depth=1" (str "--branch=" clone-branch)])
            clone-cmd
            (concat ["git clone"] (conj clone-opts clone-url clone-work-dir))]
        (o (str "Cloning YAMLScript build repo into '" clone-work-dir "'"))
        (run clone-cmd "Failed to clone YAMLScript build repo")
        (when devel?
          (o (str "Resetting YAMLScript build workdir to current commit hash: "
               clone-branch))
          (run ["git" "reset" "--hard" clone-branch]
            "Failed to reset to current commit hash")))
      (do
        (o (str "Cleaning YAMLScript build workdir: '" clone-work-dir "'"))
        (o "Cleaning the build environment")
        (run [{:dir build-dir} "git" "clean" "-dxf" "."]
          "Failed to clean the build environment")))))

(defn do-build
  ([info build-type]
   (do-build info build-type #{}))
  ([info build-type options]
   (let [stdout? (contains? options :stdout)
         {:keys [code in-file out-file ys-bin]} info
         opts (opts)
         {:keys [devel? build-dir to-tty]} opts
         ys-bin (if devel? "ys" ys-bin)
         _ (setup-yamlscript-repo opts)
         in-path (if (= in-file "--eval")
                   (let [eval-file (str build-dir "/eval.ys")]
                     (spit eval-file code)
                     eval-file)
                   in-file)
         in-path (str (fs/absolutize in-path))
         out-path (if stdout? "/dev/stdout" (str (fs/absolutize out-file)))
         make-vars [(str "BUILD-TYPE=" build-type)
                    (str "INPUT-PATH=" in-path)
                    (str "OUTPUT-PATH=" out-path)
                    (str "YS-BINARY=" ys-bin)]
         make-target (str "build-" build-type)
         make-cmd (concat
                    [(merge to-tty {:dir build-dir})
                     "make" make-target]
                    make-vars)]
     (when (env "YS_BUILD_DEBUG")
       (WWW (str/join " " (rest make-cmd))))
     (run make-cmd "Build failed"))))

(defn do-to-glj [info]
  (reset! show-log? false)
  (do-build info "glj" #{:stdout}))

(defn do-to-go [info]
  (reset! show-log? false)
  (do-build info "go" #{:stdout}))

(defn do-build-graal-bin [info]
  (reset! show-log? true)
  (let [{:keys [in-file out-file]} info]
    (o (str "Compiling '" in-file "' to binary '" out-file "'..."))
    (do-build info "graal-bin")
    (o (str "Compiled '" in-file "' to binary '" out-file "'."))))

(defn do-build-go-bin [info]
  (let [{:keys [in-file out-file]} info]
    (o (str "Compiling '" in-file "' to binary '" out-file "'..."))
    (do-build info "go-bin")
    (o (str "Compiled '" in-file "' to binary '" out-file "'."))))

(defn do-build-wasm [info]
  (let [{:keys [in-file out-file]} info]
    (o (str "Compiling '" in-file "' to WebAssembly '" out-file "'..."))
    (do-build info "wasm")
    (o (str "Compiled '" in-file "' to WebAssembly '" out-file "'."))))
