;; This code is licensed under MIT license (See License for details)
;; Copyright 2023-2024 Ingy dot Net

(defproject org.yamlscript/clj-yamlscript "0.1.41"
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

  #_#_:resource-paths ["../java/target/yamlscript-0.1.41.jar"]

  #_#_:managed-dependencies [[org.yamlscript/yamlscript "0.1.41"]]

  :dependencies
  [[org.clojure/clojure "1.11.1"]
   [org.clojure/data.json "2.4.0"]
   [org.json/json "20240205"]
   [net.java.dev.jna/jna "5.14.0"]
   [org.yamlscript/yamlscript "0.1.41"]]

  :deploy-repositories
  [["releases"
    {:url "https://repo.clojars.org"
     :username :env/clojars_username
     :password :env/clojars_password
     :sign-releases false}]])
