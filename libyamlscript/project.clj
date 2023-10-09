(defproject yamlscript/libyamlscript "0.1.0"
  :description "Shared Library for YAMLScript"

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
   [org.babashka/sci "0.8.40"]
   [org.clojure/data.json "2.4.0"]
   [yamlscript/core "0.1.0"]]

  :plugins
  [[lein-exec "0.3.7"]
   [io.github.borkdude/lein-lein2deps "0.1.0"]]

  :prep-tasks [["compile"] ["javac"]]

  :java-source-paths ["src"]

  :profiles
  {:uberjar
   {:aot [libyamlscript.core]
    :main libyamlscript.core
    :global-vars
    {*assert* false
     *warn-on-reflection* true}
    :jvm-opts
    ["-Dclojure.compiler.direct-linking=true"
     "-Dclojure.spec.skip-macros=true"]}}

  :repositories [["public-github" {:url "git://github.com"}]]

  :repl-options {:init-ns libyamlscript.core}

  :global-vars {*warn-on-reflection* true})
