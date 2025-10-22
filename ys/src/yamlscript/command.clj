;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.commands library handles install, upgrade, and binary commands
;; that were previously delegated to shell scripts.

(ns yamlscript.command
  (:require
   [babashka.fs :as fs]
   [babashka.process :refer [process]]
   [clojure.string :as str]
   [yamlscript.common :as common]
   [yamlscript.util :as util]
   [yamlscript.global :as global :refer [env]])
  (:refer-clojure))

(def yamlscript-version "0.2.7")
(def temp-dir "/tmp/yamlscript/build")

(def show-log? (atom false))
(def debug? (atom false))

(defn log [& msgs]
  (util/eprintln (str "* " (str/join "\n  " msgs)))
  (flush))

(defn o [& msgs]
  (when @show-log? (apply log msgs)))

(defn run [cmd-args err-msg]
  (when @debug?
    (util/eprintln
      (str "Running: " (str/join " " (remove map? cmd-args)))))
  (let [result (apply process cmd-args)]
    (when (not= 0 (:exit @result))
      (when @debug? (WWW result))
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
        temp-dir (or (env "YS_BUILD_TEMP_DIR") temp-dir)
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
                clone-work-dir]} opts]
    (if (fs/exists? clone-work-dir)
      (o (str "Building in " clone-url " workdir: '" clone-work-dir "'"))
      (let [clone-opts
            (if devel? [] ["--depth=1" (str "--branch=" clone-branch)])
            clone-cmd
            (concat ["git clone"] (conj clone-opts clone-url clone-work-dir))]
        (o (str "Cloning " clone-url " build repo into '" clone-work-dir "'"))
        (run clone-cmd (str "Failed to clone " clone-url " build repo"))
        (when devel?
          (o (str "Resetting " clone-work-dir " to git commit: " clone-branch))
          (run ["git" "reset" "--hard" clone-branch]
            "Failed to reset to current commit hash"))))))

(defn do-build [info build-type out-file]
  (let [{:keys [code in-file ys-bin]} info
        opts (opts)
        {:keys [devel? build-dir to-tty]} opts
        ys-bin (if devel? "ys" ys-bin)
        _ (setup-yamlscript-repo opts)
        in-path (if (= in-file "--eval")
                  (let [eval-file (str build-dir "/input.ys")]
                    (spit eval-file code)
                    eval-file)
                  in-file)
        in-path (str (fs/absolutize in-path))
        out-file (or (:out-file info) out-file)
        out-path (str (fs/absolutize out-file))
        out-path (if (= \/ (last out-file))
                   (str out-path "/")
                   out-path)
        env-vars {"YS_VERSION" yamlscript-version
                  "YS_BUILD_TYPE" build-type
                  "YS_INPUT_PATH" in-path
                  "YS_OUTPUT_PATH" out-path
                  "YS_CLI_BINARY" ys-bin}
        env-vars (if (or (:print info) (:quiet info))
                   (assoc env-vars "YS_BUILD_QUIET" "1")
                   env-vars)
        env-vars (if (:verbose info)
                   (assoc env-vars "YS_BUILD_VERBOSE" "1")
                   env-vars)
        env-vars (if @debug?
                   (assoc env-vars "YS_BUILD_DEBUG" "1")
                   env-vars)
        make-cmd (concat
                   [(merge to-tty {:dir build-dir} {:extra-env env-vars})
                    "make" "build"])]
    (o (str "Compiling '" in-file "' to '" out-file "'..."))
    (run make-cmd "Build failed")
    (o (str "Compiled '" in-file "' to '" out-file "'."))))

(defn get-out-file [info extension default]
  (let [{:keys [in-file print quiet]} info
        base-name (when (fs/regular-file? in-file)
                    (fs/strip-ext (fs/file-name in-file)))
        out-file (or (:output info)
                   (cond
                     print "/dev/stdout"
                     base-name (str base-name extension)
                     :else default))
        quiet (or (= out-file "/dev/stdout") quiet)]
    (reset! show-log? (not (or print quiet)))
    (when (or (seq (:debug info)) (env "YS_BUILD_DEBUG"))
      (reset! debug? true))
    out-file))

(defn do-build-go [info]
  (let [out-file (get-out-file info "" "a.out")]
    (do-build info "go" out-file)))

(defn do-build-graal [info]
  (let [out-file (get-out-file info "" "a.out")]
    (do-build info "graal" out-file)))
