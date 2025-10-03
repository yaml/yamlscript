(defproject NAME "build"
  :description "Compile a YS program to native GraalVM binary executable"

  :dependencies
  [[org.clojure/clojure "1.12.0"]
   [org.babashka/sci "0.8.41"]
   [yamlscript/core "0.2.4"]]

  :main ^:skip-aot main-99-bottles-of-beer

  :target-path "target/%s"

  :prep-tasks
  [["compile"] ["javac"]]

  :java-source-paths ["src"]

  :profiles
  {:uberjar
   {:aot [NAME]
    :main NAME
    :global-vars
    {*assert* false
     *warn-on-reflection* true}
    :jvm-opts
    ["-Dclojure.compiler.direct-linking=true"
     "-Dclojure.spec.skip-macros=true"]}}

  :global-vars {*warn-on-reflection* true})
