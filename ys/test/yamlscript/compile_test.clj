;; Copyright 2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.compile-test
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [yamlscript.cli :as cli]
            [yamlscript.util.compile :as build]
            [yamlscript.util-platform :as platform]))

(defn options [output & [target]]
  (build/resolve-options
    (cond-> {:compile true} output (assoc :output output)
      target (assoc :to target))))

(deftest output-selection
  (doseq [[path target] [["foo" "bin"] ["foo.exe" "bin"] ["foo.go" "go"]
                         ["foo/" "dir"] ["foo.so" "lib"] ["foo.dylib" "lib"]
                         ["foo.dll" "lib"] ["foo.h" "h"] ["foo.clj" "clj"]
                         ["foo.js" "js"] ["foo.html" "html"] ["foo.wasm" "wasm"]]]
    (is (= target (:to (options path)))))
  (is (= "bin" (:to (options "foo.xyz" "bin")))))

(deftest suffix-selection
  (doseq [target ["so" "lib" "dylib" "dll"]]
    (is (= ["./foo.so" "./foo.h"]
          (get-in (build/resolve-options
                    {:to (str target ",h") :output "./foo.so"} "foo.ys")
            [:build :outputs]))))
  (is (= {:target "lib" :platform "darwin/amd64"
          :outputs ["./foo.dylib" "./foo.h"]}
        (:build (build/resolve-options
                  {:to "dylib,h,darwin/amd64"} "foo.ys"))))
  (is (= ["foo.so" "include/foo.h"]
        (get-in (options "foo.so,include/foo.h" "so,h") [:build :outputs])))
  (doseq [target ["bin,h" "so,h,bad" "so,h,linux/amd64,extra"]]
    (is (thrown? Exception (build/resolve-options {:to target} "foo.ys"))))
  (is (= {:target "lib" :platform "darwin/amd64"
          :outputs ["lib/foo.so" "lib/foo.h"]}
        (:build (options "lib/foo.so,.h,darwin/amd64"))))
  (is (= ["lib/foo.so" "include/foo.h"]
        (get-in (options "lib/foo.so,include/foo.h") [:build :outputs])))
  (is (= ["foo.html" "foo.js"] (get-in (options "foo.html") [:build :outputs])))
  (is (= "darwin/amd64"
        (get-in (options "foo.xyz" "bin,darwin/amd64") [:build :platform])))
  (is (= ["foo.js" "../web/foo.html"]
        (get-in (options "foo.js,../web/foo.html") [:build :outputs])))
  (is (= ["foo.js" "foo.html"]
        (get-in (options "foo.js,.html") [:build :outputs])))
  (is (= ["./foo.js" "./foo.html"]
        (get-in (build/resolve-options {:to "js,html"} "foo.ys")
          [:build :outputs]))))

(deftest extension-selection
  (is (= ["-Xprune"]
        (get-in (options "foo,-Xprune") [:build :extensions])))
  (is (= ["-Xprune"]
        (get-in (build/resolve-options {:to "bin,-Xprune"} "foo.ys")
          [:build :extensions])))
  (is (= ["-Xreport=out.md" "-Xprune"]
        (get-in (options "foo,-Xprune" "bin,-Xreport=out.md")
          [:build :extensions])))
  (is (= {:target "js" :platform "js/wasm"
          :outputs ["./foo.js" "./foo.html"]
          :extensions ["-Xprune"]}
        (:build (build/resolve-options
                  {:to "js,html,-Xprune,js/wasm"} "foo.ys"))))
  (is (= {:target "html" :platform nil
          :outputs ["./foo/index.html" "./foo/index.js"]
          :extensions ["-Xserve"]}
        (:build (build/resolve-options
                  {:to "html,-Xserve"} "foo.ys"))))
  (is (= ["./foo/index.js" "./foo/index.html"]
        (get-in (build/resolve-options {:to "js,-Xopen"} "foo.ys")
          [:build :outputs])))
  (is (= ["web/app.html" "web/app.js"]
        (get-in (build/resolve-options
                  {:to "html,-Xserve" :output "web/app.html"} "foo.ys")
          [:build :outputs])))
  (is (= ["web/app.js" "web/app.html"]
        (get-in (build/resolve-options
                  {:to "js,-Xserve" :output "web/app.js"} "foo.ys")
          [:build :outputs])))
  (is (thrown-with-msg? Exception #"name must follow"
        (options "foo,-X")))
  (is (thrown-with-msg? Exception #"Gloat compilation target"
        (options "foo.clj,-Xprune"))))

(deftest invalid-selections
  (doseq [[output target] [["foo.xyz" nil] ["foo," nil] ["foo.so,.html" nil]
                           ["foo.js,.h" nil] ["foo.so,.h,.h" nil]
                           ["foo,linux/amd64" "bin,darwin/amd64"]
                           ["foo" "bin,bad"] ["foo/" "bin"]
                           ["foo.go,linux/amd64" nil]
                           ["foo.js,wasip1/wasm" nil]]]
    (is (thrown? Exception (options output target)) (str output " " target)))
  (doseq [[output target] [["foo.js;.html" nil]
                           ["foo" "bin;darwin/amd64"]]]
    (is (thrown-with-msg? Exception #"use ',' instead"
          (options output target))))
  (doseq [target ["bin" "dir" "lib" "h" "js" "html" "wasm"]]
    (is (thrown? Exception (options nil target))))
  (doseq [target ["js,-Xhtml=3" "html,-Xserve=a" "js,-Xopen=x"]]
    (is (thrown-with-msg? Exception #"URL query"
          (build/resolve-options {:to target} "foo.ys")))))

(deftest default-binary-output
  (doseq [[source output] [["sample/rosetta-code/99-bottles-of-beer.ys"
                            "./99-bottles-of-beer"]
                           ["/tmp/nested/foo.bar.ys" "./foo.bar"]
                           ["nested/with spaces.ys" "./with spaces"]]]
    (doseq [target ["bin" "bin,darwin/amd64"]]
      (let [opts (build/resolve-options {:to target} source)]
        (is (= output (:output opts)))
        (is (= [output] (get-in opts [:build :outputs])))
        (is (= (when (str/includes? target ",") "darwin/amd64")
              (get-in opts [:build :platform]))))))
  (is (= "explicit.xyz"
        (:output (build/resolve-options
                   {:to "bin" :output "explicit.xyz"} "foo.ys"))))
  (is (nil? (:output (build/resolve-options {:compile true} "foo.ys"))))
  (doseq [source [nil "-" "foo.clj" ".ys"]]
    (is (thrown-with-msg? Exception #"requires --output"
          (build/resolve-options {:to "bin"} source))))
  (doseq [argv [["sample/rosetta-code/99-bottles-of-beer.ys" "-cTbin"]
                ["--file" "nested/99-bottles-of-beer.ys" "-Tbin"]]]
    (let [[opts _ error errors] (cli/get-opts argv)]
      (is (nil? error))
      (is (empty? errors))
      (is (= "./99-bottles-of-beer" (:output opts))))))

(deftest default-artifact-output
  (doseq [[target suffix normalized]
          [["wasm" ".wasm" "wasm"] ["js" ".js" "js"]
           ["html" ".html" "html"] ["h" ".h" "h"] ["dir" "/" "dir"]
           ["so" ".so" "lib"] ["dylib" ".dylib" "lib"] ["dll" ".dll" "lib"]
           ["lib,linux/amd64" ".so" "lib"]
           ["lib,darwin/amd64" ".dylib" "lib"]
           ["lib,windows/amd64" ".dll" "lib"]]]
    (let [opts (build/resolve-options {:to target} "nested/foo.bar.ys")]
      (is (= (str "./foo.bar" suffix) (:output opts)))
      (is (= normalized (:to opts)))))
  (is (= ["./foo.html" "./foo.js"]
        (get-in (build/resolve-options {:to "html"} "foo.ys") [:build :outputs])))
  (doseq [target ["go" "clj" "clj+" "bb"]]
    (is (nil? (:output (build/resolve-options {:to target} "foo.ys")))))
  (doseq [target ["wasm" "so" "dylib" "dll"]]
    (is (= "explicit.xyz"
          (:output (build/resolve-options
                     {:to target :output "explicit.xyz"} "foo.ys")))))
  (doseq [target ["wasm" "so" "lib" "dir" "h" "js" "html"]
          source [nil "-" "foo.clj" ".ys"]]
    (is (thrown? Exception (build/resolve-options {:to target} source)))))

(deftest native-default-binary-output
  (when-let [binary (System/getenv "YS_COMPILE_TEST_BIN")]
    (let [dir (str (fs/create-temp-dir {:prefix "ys-default-output-"}))
          source (str dir "/nested/with spaces.v1.ys")
          output (str dir "/with spaces.v1")
          run (fn [& args]
                (apply process/shell
                  {:dir dir :out :string :err :string :continue true
                   :extra-env {"YS_GLOAT"
                               (str (fs/absolutize "test/fixtures/gloat.bash"))}}
                  binary args))]
      (try
        (fs/create-dirs (fs/parent source))
        (spit source
          (slurp "../sample/rosetta-code/99-bottles-of-beer.ys"))
        (doseq [args [["-cTbin" "nested/with spaces.v1.ys"]
                      ["--file" source "--to=bin,darwin/amd64"]]]
          (let [result (apply run args)]
            (is (zero? (:exit result)) (:err result)))
          (is (= "bin artifact\n" (slurp output)))
          (is (str/includes? (:err (apply run args)) "already exists"))
          (is (= "bin artifact\n" (slurp output)))
          (fs/delete output))
        (doseq [[target suffix] [["wasm" ".wasm"] ["so" ".so"]
                                ["dylib,h" ".dylib"] ["dll" ".dll"]
                                ["html" ".html"] ["dir" "/"]]]
          (let [result (run (str "-cT" target) source)]
            (is (zero? (:exit result)) (:err result)))
          (is (fs/exists? (str output suffix))))
        (is (fs/exists? (str output ".js")))
        (is (fs/exists? (str output ".h")))
        (finally (fs/delete-tree dir))))))

(deftest existing-cli-behavior
  (is (not (build/code-target? "star")))
  (is (build/code-target? "clj+"))
  (let [[_ _ _ errors] (cli/get-opts ["-Tstar" "-e" "say: 42"])]
    (is (seq errors)))
  (is (= {:output "foo"} (build/resolve-options {:output "foo"})))
  (is (nil? (:to (options nil))))
  (doseq [target ["bb" "clj" "clj+" "go"]]
    (is (= target (:to (options nil target)))))
  (doseq [[argv target] [[["-c" "-o" "foo" "input.ys"] "bin"]
                         [["-T" "bin,darwin/amd64"
                           "-o" "foo.xyz" "input.ys"] "bin"]
                         [["-T" "bin,-Xprune" "input.ys"] "bin"]
                         [["-T" "js,html" "input.ys"] "js"]
                         [["-c" "-o" "foo.clj" "input.ys"] "clj"]]]
    (let [[opts _ error errors] (cli/get-opts argv)]
      (is (nil? error)) (is (empty? errors)) (is (= target (:to opts))))))

(defmacro with-context [[dir ctx] & body]
  `(let [~dir (str (fs/create-temp-dir {:prefix "ys compile test "}))
         ~ctx (assoc (platform/context)
                :start-progress nil
                :exe (str ~dir "/prefix/bin/ys")
                :env {"YS_GLOAT" (str (fs/absolutize "test/fixtures/gloat.bash"))
                      "TMPDIR" ~dir})]
     (try ~@body (finally (fs/delete-tree ~dir)))))

(deftest collisions
  (with-context [dir ctx]
    (let [file (str dir "/file") directory (str dir "/dir")]
      (spit file "original")
      (fs/create-dirs directory)
      (fs/create-sym-link (str dir "/link") (str dir "/missing"))
      (doseq [path [file (str file "/") directory (str directory "/")
                   (str dir "/link")]]
        (is (thrown-with-msg? Exception #"already exists"
              (build/check-outputs! ctx (options path "dir")))))
      (is (thrown? Exception
            (build/check-outputs! ctx (options (str dir "/f.h,.h") "lib"))))
      (is (= "original" (slurp file))))))

(deftest selected-artifacts
  (with-context [dir ctx]
    (doseq [path ["program" "code.go" "project/" "lib.so" "header.h"
                  "pair.so,.h" "web.js" "page.html" "module.wasm"
                  "assets/main.js,pages/runner.html"]]
      (let [opts (options (str dir "/" path))
            opts (if (= path "assets/main.js,pages/runner.html")
                   (options
                     (str dir "/assets/main.js," dir "/pages/runner.html"))
                   opts)]
        (build/compile! ctx opts "portable source" nil)
        (doseq [output (get-in opts [:build :outputs])]
          (is (fs/exists? output)))))
    (is (not (fs/exists? (str dir "/lib.h"))))
    (is (not (fs/exists? (str dir "/header.so"))))
    (is (= "fetch('../assets/main.js')\n" (slurp (str dir "/pages/runner.html"))))
    (is (empty? (fs/glob dir ".ys-install.*")))))

(deftest forwards-gloat-extensions
  (with-context [dir ctx]
    (let [calls (atom [])
          run (:run ctx)
          ctx (assoc ctx :run
                (fn [argv]
                  (swap! calls conj argv)
                  (run argv)))
          opts (options (str dir "/program,-Xprune")
                 "bin,-Xfuture=value")]
      (build/compile! ctx opts "portable source" nil)
      (let [argv (some #(when (= "env" (first %)) %) @calls)]
        (is (= ["-Xfuture=value" "-Xprune"]
              (filterv #(str/starts-with? % "-X") argv)))))))

(deftest bootstrap-policy
  (with-context [dir ctx]
    (with-redefs [build/which (fn [_ _] nil)]
      (is (thrown-with-msg? Exception #"YS_GLOAT"
            (build/find-gloat ctx)))
      (is (thrown-with-msg? Exception #"writable PREFIX/bin"
            (build/find-gloat (assoc ctx :env {} :exe (str dir "/ys"))))))))

(deftest native-cli
  (when-let [binary (System/getenv "YS_COMPILE_TEST_BIN")]
    (with-context [dir ctx]
      (let [run (fn [& args]
                  (apply process/shell
                    {:out :string :err :string :continue true
                     :extra-env {"YS_GLOAT" ((:env ctx) "YS_GLOAT")}}
                    binary args))
            output (str dir "/program")]
        (is (zero? (:exit (run "-ce" "say: 42" "-o" output))))
        (is (fs/exists? output))
        (is (not (zero? (:exit (run "-ce" "say: 42" "-o" output)))))
        (is (= "bin artifact\n" (slurp output)))
        (doseq [args [["-ce" "say: 42" "-o" (str dir "/bad.xyz")]
                      ["-Tstar" "-e" "say: 42"]
                      ["-Tbin" "-e" "say: 42"]
                      ["-c" "-J" "-e" "say: 42" "-o" (str dir "/bad")]]]
          (is (not (zero? (:exit (apply run args))))))))))

(deftest failed-build-cleans-up
  (with-context [dir ctx]
    (let [run (:run ctx)
          ctx (assoc ctx :run
                (fn [argv]
                  (if (= "env" (first argv))
                    {:exit 1 :out "" :err "intentional build failure"}
                    (run argv))))
          opts (options (str dir "/library.so,.h"))]
      (is (thrown-with-msg? Exception #"intentional build failure"
            (build/compile! ctx opts "source" nil)))
      (is (not (fs/exists? (str dir "/library.so"))))
      (is (not (fs/exists? (str dir "/library.h"))))
      (is (empty? (fs/glob dir ".ys-install.*"))))))

(deftest compilation-progress
  (doseq [fails? [false true]]
    (let [events (atom [])
          ctx {:start-progress
               (fn [& labels]
                 (swap! events conj (vec labels))
                 #(swap! events conj %))}]
      (with-redefs [build/compile-artifacts!
                    (fn [& _]
                      (swap! events conj :build)
                      (when fails? (throw (Exception. "build failed"))))]
        (try (build/compile! ctx (options "./program") "source" nil)
          (catch Exception e (is (= "build failed" (.getMessage e))))))
      (is (= [["Compiling to native binary: 'program'"
               "Compiled to native binary: 'program'"
               "Failed to compile to binary: 'program'"]
              :build (not fails?)] @events))))
  (let [out (java.io.StringWriter.)]
    (binding [*err* out]
      (build/run-gloat! {:run (constantly {:exit 0 :out "build chatter"})} []))
    (is (= "" (str out))))
  (is (thrown-with-msg? Exception #"compiler diagnostic"
        (build/run-gloat!
          {:run (constantly {:exit 1 :out "compiler diagnostic"})} []))))

(deftest serving-progress
  (let [events (atom [])
        err (java.io.StringWriter.)
        ctx {:run-observed
             (fn [_ observe]
               (observe "Now serving http://localhost:8000/web/index.html")
               (observe "Now serving http://localhost:8000/ignored/index.html")
               {:exit 0 :out "" :err ""})
             :start-progress
             (fn [& labels]
               (swap! events conj (vec labels))
               #(swap! events conj %))}]
    (with-redefs [build/compile-artifacts!
                  (fn [ctx _ _ _]
                    (build/run-gloat! ctx []
                      (fn [line]
                        (when (str/starts-with? line "Now serving ")
                          ((:server-ready ctx) line)))))]
      (binding [*err* err]
        (build/compile! ctx (options "./web/index.js" "js,-Xserve")
          "source" nil)))
    (is (= true (last @events)))
    (is (= 2 (count @events)))
    (is (= "Now serving http://localhost:8000/web/index.html\n" (str err)))))

(deftest serving-writes-final-artifacts
  (with-context [dir ctx]
    (let [output (str dir "/web/index.js")
          opts (options output "js,-Xserve")
          err (java.io.StringWriter.)]
      (binding [*err* err]
        (build/compile! ctx opts "source" nil))
      (is (= "js artifact\n" (slurp output)))
      (is (fs/exists? (str dir "/web/index.html")))
      (is (str/includes? (str err) "/web/index.html"))
      (is (empty? (fs/glob dir ".ys-install.*"))))))

(deftest serving-requires-sibling-artifacts
  (with-context [dir ctx]
    (let [opts (options
                 (str dir "/assets/app.js," dir "/pages/app.html")
                 "js,-Xserve")]
      (is (thrown-with-msg? Exception #"same directory"
            (build/compile! ctx opts "source" nil))))))

(deftest serving-extensions
  (doseq [extension ["-Xserve" "-Xopen"]]
    (is (build/serving-extension? extension)))
  (doseq [extension ["-Xserve=x" "-Xserver" "-Xopenly" "-Xprune"]]
    (is (not (build/serving-extension? extension)))))

(deftest native-compilation-progress
  (when-let [binary (System/getenv "YS_COMPILE_TEST_BIN")]
    (with-context [dir ctx]
      (doseq [fails? [false true]]
        (let [path (str dir (if fails? "/failure" "/success"))
              result (process/shell
                       {:out :string :err :string :continue true
                        :extra-env {"YS_GLOAT" ((:env ctx) "YS_GLOAT")
                                    "YS_COMPILE_FAIL" (if fails? "1" "0")}}
                       binary "-ce" "say: 42" "-o" path)]
          (is (= (not fails?) (zero? (:exit result))))
          (is (= "" (:out result)))
          (is (str/includes? (:err result) "… Compiling to native binary:"))
          (is (not (str/includes? (:err result) "\u001b")))
          (if fails?
            (do
              (is (str/includes? (:err result) "X Failed to compile to binary:"))
              (is (str/includes? (:err result) "Gloat compilation failed:"))
              (is (not (fs/exists? path))))
            (do
              (is (str/includes? (:err result) "√ Compiled to native binary:"))
              (is (re-find #"\([0-9]+\.[0-9]s\)" (:err result))))))))))

(deftest native-serving-progress
  (when-let [binary (System/getenv "YS_COMPILE_TEST_BIN")]
    (with-context [dir ctx]
      (let [output (str dir "/web.js")
            result (process/shell
                     {:out :string :err :string :continue true
                      :extra-env {"YS_GLOAT" ((:env ctx) "YS_GLOAT")}}
                     binary "-cTjs,html,-Xserve" "-e" "say: 42"
                     "-o" output)]
        (is (zero? (:exit result)) (:err result))
        (is (fs/exists? output))
        (is (fs/exists? (str dir "/web.html")))
        (is (str/includes? (:err result)
              "√ Compiled to browser Wasm: '"))
        (is (= 1 (count (re-seq #"Now serving " (:err result)))))
        (is (str/includes? (:err result)
              "Now serving http://localhost:8000/web.html"))))))

(deftest bootstrap-installation
  (with-context [dir ctx]
    (let [bin (str dir "/prefix/bin")
          executable (str bin "/gloat")
          run (:run ctx)
          calls (atom [])
          ctx (assoc ctx :env {}
                :run (fn [argv]
                       (if (and (= "bash" (first argv))
                                (str/includes? (nth argv 2 "") "https://in-1.cc"))
                         (do
                           (swap! calls conj argv)
                           (spit executable "#!/usr/bin/env bash\nexit 0\n")
                           (fs/set-posix-file-permissions executable "rwxr-xr-x")
                           {:exit 0 :out "" :err ""})
                         (run argv))))]
      (fs/create-dirs bin)
      (with-redefs [build/which (fn [_ _] nil)]
        (is (= executable (build/find-gloat ctx)))
        (is (= executable (build/find-gloat ctx)))
        (is (= 1 (count @calls)))
        (is (= (str dir "/prefix") (last (first @calls))))))))

(deftest real-gloat-artifacts
  (when-let [gloat (System/getenv "YS_COMPILE_REAL_GLOAT")]
    (let [original (or (System/getenv "YS_COMPILE_TEST_BIN")
                      (throw (Exception. "YS_COMPILE_TEST_BIN is required")))
          dir (str (fs/create-temp-dir {:prefix "ys-real-compile-"}))
          binary (str dir "/ys")
          compile (fn [input output]
                    (process/shell
                      {:out :string :err :string
                       :extra-env {"YS_GLOAT" gloat}}
                      binary input "-c" "-o" output))]
      (try
        (fs/copy original binary {:copy-attributes true})
        (let [program (str dir "/compile")
              prune-source (str (fs/absolutize
                                  "test/fixtures/compile-prune.ys"))
              pruned-output (str dir "/pruned-output")
              pruned-target-source (str dir "/pruned-target.ys")
              pruned-target (str dir "/pruned-target")
              library (str dir "/answer.so")
              header (str dir "/answer.h")
              alone (str dir "/alone.h")
              browser (str dir "/assets/browser.js")
              html (str dir "/pages/browser.html")]
          (process/shell
            {:dir dir :out :string :err :string
             :extra-env {"YS_GLOAT" gloat}}
            binary "-cTbin" (str (fs/absolutize "test/fixtures/compile.ys")))
          (is (= "[7,7]\n"
                (:out (process/shell {:out :string} program "7"))))
          (compile prune-source (str pruned-output ",-Xprune"))
          (fs/copy prune-source pruned-target-source)
          (process/shell
            {:dir dir :out :string :err :string
             :extra-env {"YS_GLOAT" gloat}}
            binary "-cTbin,-Xprune" pruned-target-source)
          (doseq [path [pruned-output pruned-target]]
            (is (= "7\n"
                  (:out (process/shell {:out :string} path "7")))))
          (compile "test/fixtures/compile-lib.ys" (str library "," header))
          (compile "test/fixtures/compile-lib.ys" alone)
          (is (str/includes? (slurp header) "answer("))
          (is (= (slurp header) (slurp alone)))
          (is (not (fs/exists? (str dir "/alone.so"))))
          (process/shell (or (System/getenv "YS_COMPILE_PYTHON") "python3") "-c"
            "import ctypes,sys; assert ctypes.CDLL(sys.argv[1]).answer() == 42"
            library)
          (compile "test/fixtures/compile.ys" (str dir "/source.go"))
          (is (str/includes? (slurp (str dir "/source.go")) "package"))
          (compile "test/fixtures/compile.ys" (str dir "/project/"))
          (process/shell {:dir (str dir "/project") :out :string :err :string}
            "make")
          (compile
            "test/fixtures/compile.ys" (str dir "/cross,darwin/amd64"))
          (is (str/includes?
                (:out (process/shell {:out :string} "file" (str dir "/cross")))
                "Mach-O"))
          (compile "test/fixtures/compile-lib.ys" (str browser "," html))
          (is (str/includes? (slurp html) "../assets/browser.js"))
          (when-let [node (System/getenv "YS_COMPILE_NODE")]
            (is (zero? (:exit (process/shell node "test/fixtures/browser-smoke.cjs" html)))))
          (compile "test/fixtures/compile.ys" (str dir "/program.wasm"))
          (is (str/includes?
                (:out (process/shell {:out :string} "file" (str dir "/program.wasm")))
                "WebAssembly"))
          (when-let [wasmtime (System/getenv "YS_COMPILE_WASMTIME")]
            (is (= "[8,8]\n"
                  (:out (process/shell {:out :string} wasmtime
                          (str dir "/program.wasm") "8"))))))
        (finally
          ;; Go module directories are read-only; fs 0.5.26's force option
          ;; has a JDK 25 EnumSet cast bug, so make this test tree writable.
          (process/shell {:out :string :err :string} "chmod" "-R" "u+wx" dir)
          (fs/delete-tree dir))))))

(deftest compilation-errors-are-reported
  (with-context [dir ctx]
    (let [path (str dir "/existing")
          errors (java.io.StringWriter.)]
      (spit path "original")
      (with-redefs [cli/exit (constantly nil)]
        (binding [*err* errors]
          (cli/do-compile (options path) [])))
      (is (str/includes? (str errors) "Output already exists:"))
      (is (not (str/includes? (str errors) "Exception in thread")))
      (is (= "original" (slurp path))))))

(deftest source-output-without-subprocesses
  (with-context [dir ctx]
    (let [ctx (assoc ctx :run (fn [_] (throw (Exception. "Unexpected subprocess"))))]
      (doseq [target ["clj" "bb" "clj+"]]
        (let [path (str dir "/nested/source." target)]
          (build/write-source! ctx (options path target) "source\n")
          (is (= "source\n" (slurp path)))
          (when (= target "bb") (is (fs/executable? path)))
          (is (thrown? Exception
                (build/write-source! ctx (options path target) "replacement"))))))))

(deftest native-source-without-build-tools
  (when-let [binary (System/getenv "YS_COMPILE_TEST_BIN")]
    (with-context [dir ctx]
      (let [path (str dir "/source.clj")
            result (process/shell
                     {:out :string :err :string :continue true
                      :extra-env {"PATH" (str dir "/empty")}}
                     binary "-ce" "say: 42" "-o" path)]
        (is (zero? (:exit result)) (:err result))
        (is (str/includes? (slurp path) "(say 42)"))))))
