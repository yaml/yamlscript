(defproject org.yaml/yamlscript-clj "0.1.36"
  :description "YAMLScript is a functional programming language whose syntax is encoded in YAML.
    YAMLScript can be used for writing new software applications and libraries."
  :url "https://yamlscript.org"
  :resource-paths ["../java/target/yamlscript-java-0.1.36.jar"]
  :managed-dependencies [[org.yaml/yamlscript-java "0.1.36"]]
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/data.json "2.4.0"]
                 [org.json/json "20240205"]
                 [net.java.dev.jna/jna "5.14.0"]])
