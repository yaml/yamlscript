;; This code is licensed under MIT license (See License for details)
;; Copyright 2023-2024 Ingy dot Net

(defproject org.yamlscript/clj-yamlscript "0.1.43"
  :description
  "YAMLScript is a functional programming language whose syntax is encoded in
  YAML."

  :url "https://yamlscript.org"

  :license
  {:name "MIT"
   :url "https://opensource.org/license/mit/"}

  :scm
  {:name "git"
   :url "https://github.com/yaml/yamlscript"
   :tag "clojure"
   :dir ".."}

  :dependencies
  [[org.clojure/clojure "1.11.1"]
   [org.clojure/data.json "2.4.0"]
   [org.json/json "20240205"]
   [net.java.dev.jna/jna "5.14.0"]
   [org.yamlscript/yamlscript "0.1.43"]]

  :deploy-repositories
  [["releases"
    {:url "https://repo.clojars.org"
     :username :env/clojars_username
     :password :env/clojars_password
     :sign-releases false}]]

  :plugins
  [[lein-exec "0.3.7"]
   [reifyhealth/lein-git-down "0.4.1"]
   [dev.weavejester/lein-cljfmt "0.11.2"]
   [io.github.borkdude/lein-lein2deps "0.1.0"]]

  :prep-tasks [["lein2deps" "--write-file" "deps.edn" "--print" "false"]])
