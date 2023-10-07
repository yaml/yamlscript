(defproject yamlscript/core "0.1.0"
  :description "Program in YAML"
  :url "https://github.com/yaml/yamlscript"

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
   [clj-commons/clj-yaml "1.0.27"]
   [zprint "1.2.8"]
   [org.snakeyaml/snakeyaml-engine "2.6"]
   [pjstadig/humane-test-output "0.11.0"]]

  :plugins
  [[reifyhealth/lein-git-down "0.4.1"]
   [dev.weavejester/lein-cljfmt "0.11.2"]
   [lein-exec "0.3.7"]]

  :injections [(require 'pjstadig.humane-test-output)
               (pjstadig.humane-test-output/activate!)]

  :repositories [["public-github" {:url "git://github.com"}]]

  :repl-options
  {:init-ns yamlscript.core
   :init
   (do
     (require 'pjstadig.humane-test-output)
     (pjstadig.humane-test-output/activate!)
     (require 'yamlscript.test-runner))}
  :global-vars {*warn-on-reflection*
                (some? (System/getenv "WARN_ON_REFLECTION"))})
