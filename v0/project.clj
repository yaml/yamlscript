;; This code is licensed under MIT license (See License for details)
;; Copyright 2023-2026 Ingy dot Net

(defproject org.yamlscript/ys.v0 "0.2.28"
  :description
  "The YS (YAMLScript) v0 standard library for Clojure runtimes.
  Code compiled with `ys -T bb|clj|jolt|glj` runs on babashka, JVM
  Clojure, jolt and glojure with this library on the classpath."

  :url "https://yamlscript.org"

  :license
  {:name "MIT"
   :url "https://opensource.org/license/mit/"}

  :scm
  {:name "git"
   :url "https://github.com/yaml/yamlscript"
   :tag "v0"
   :dir ".."}

  ;; The sources live in core/src; only the portable ys.v0 namespaces go
  ;; in this jar. META-INF stays: consumers like jolt read the pom.xml
  ;; inside the jar for transitive dependencies.
  :source-paths ["../core/src"]
  :jar-exclusions [#"^(?!ys/v0|META-INF)"]

  :dependencies
  [[org.clojure/clojure "1.12.0"]
   [org.clojure/data.csv "1.1.0"]
   [org.clojure/data.json "2.4.0"]
   [clj-commons/clj-yaml "1.0.27"]
   [org.flatland/ordered "1.15.11"]
   ;; Optional: only needed for load-pod on JVM Clojure (built into
   ;; babashka). Optional keeps its bencode/cheshire/transit/jackson
   ;; transitive cluster out of consumers' dependency resolution.
   [babashka/babashka.pods "0.2.0" :optional true]
   [babashka/fs "0.5.26"]
   [babashka/process "0.6.23"]
   [org.babashka/http-client "0.4.23"]
   [org.clojure/tools.cli "1.0.219"]]

  :deploy-repositories
  [["releases"
    {:url "https://repo.clojars.org"
     :username :env/clojars_username
     :password :env/clojars_password
     :sign-releases false}]]

  :plugins
  [[io.github.borkdude/lein-lein2deps "0.1.0"]]

  :prep-tasks [["lein2deps" "--write-file" "deps.edn" "--print" "false"]])
