;; Copyright 2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.util.compile
  (:require [clojure.string :as str]
            [yamlscript.util.install :as install]))

(def targets #{"bb" "clj" "clj+" "bin" "go" "dir" "lib" "so" "dylib" "dll" "h"
               "js" "html" "wasm"})
(def gloat-targets #{"bin" "go" "dir" "lib" "h" "js" "html" "wasm"})
(def extensions {"bb" "bb" "clj" "clj" "go" "go" "exe" "bin"
                 "so" "lib" "dylib" "lib" "dll" "lib" "h" "h"
                 "js" "js" "html" "html" "wasm" "wasm"})
(def target-list "bb, clj, clj+, bin, go, dir, lib, so, dylib, dll, h, js, html, wasm")

(def library-aliases #{"so" "dylib" "dll"})

(defn default-extension [target platform]
  (case target
    "bin" "" "dir" "/" "wasm" ".wasm" "js" ".js" "html" ".html" "h" ".h"
    "so" ".so" "dylib" ".dylib" "dll" ".dll"
    "lib" (let [os (str/lower-case
                     (or platform (System/getProperty "os.name") ""))]
            (cond
              (str/starts-with? os "windows") ".dll"
              (or (str/starts-with? os "darwin")
                  (str/starts-with? os "mac")) ".dylib"
              :else ".so"))
    nil))

(defn target-name [value] (first (str/split (or value "") #";" -1)))
(defn code-target? [value] (contains? targets (target-name value)))
(defn fail [message] (install/fail message))
(defn basename [path] (last (str/split path #"/")))
(defn extension [path] (second (re-find #"\.([^.\/]+)$" (basename path))))
(defn stem [path] (str/replace path #"\.[^./]+$" ""))
(defn platform? [value]
  (boolean (re-matches #"[a-z][a-z0-9]*/[a-z0-9]+" value)))

(defn infer-target [output]
  (cond
    (nil? output) nil
    (str/ends-with? output "/") "dir"
    (not (extension output)) "bin"
    :else (or (extensions (extension output))
            (fail "Unknown compilation output extension; specify --to."))))

(defn companion-path [output companion]
  (if (re-matches #"\.[a-z]+" companion)
    (str (stem output) companion)
    companion))

(defn resolve-options
  ([opts] (resolve-options opts (:file opts)))
  ([opts source]
  (if-not (or (:compile opts) (code-target? (:to opts)))
    opts
    (let [out-parts (when (:output opts)
                      (str/split (:output opts) #";" -1))
          to-parts (when (:to opts) (str/split (:to opts) #";" -1))
          output (first out-parts)
          suffixes (vec (rest out-parts))
          out-platform (when (and (seq suffixes) (platform? (last suffixes)))
                         (last suffixes))
          companions (if out-platform (vec (butlast suffixes)) suffixes)
          to-header? (= "h" (second to-parts))
          to-platform (nth to-parts (if to-header? 2 1) nil)
          target (or (first to-parts) (infer-target output))
          platform (or to-platform out-platform)
          default-ext (default-extension target platform)
          output (or output
                   (when (and default-ext source
                           (str/ends-with? source ".ys")
                           (> (count (basename source)) 3))
                     (str "./" (subs (basename source)
                                 0 (- (count (basename source)) 3)) default-ext)))
          target (if (library-aliases target) "lib" target)]
      (when (or (some str/blank? out-parts) (some str/blank? to-parts)
                (> (count to-parts) (if to-header? 3 2))
                (> (count companions) 1))
        (fail "Invalid compilation output specification."))
      (when (and target (not (targets target)))
        (fail (str "Compilation target must be one of: " target-list)))
      (when (and to-header? (not= target "lib"))
        (fail "The ;h target suffix requires a shared library target."))
      (when (and to-platform (not (platform? to-platform)))
        (fail "Platform must have the form OS/ARCH."))
      (when (and to-platform out-platform (not= to-platform out-platform))
        (fail "Conflicting compilation platforms."))
      (when (and (contains? #{"bin" "lib" "dir" "h" "js" "html" "wasm"}
                   target) (not output))
        (fail (str "--to=" target " requires --output.")))
      (when (and output (str/ends-with? output "/") (not= target "dir"))
        (fail "A directory output requires --to=dir."))
      (when (and platform
                 (not (contains? #{"bin" "lib" "h"} target))
                 (not (and (#{"js" "html"} target) (= platform "js/wasm")))
                 (not (and (= target "wasm") (= platform "wasip1/wasm"))))
        (fail "Platform is incompatible with the compilation target."))
      (let [companion (when-let [part (or (first companions)
                                       (when to-header? ".h"))]
                        (companion-path output part))]
        (when (and companion
                   (not (or (and (= target "lib") (= "h" (extension companion)))
                            (and (= target "js") (= "html" (extension companion))))))
          (fail "Only library headers and browser HTML companions are supported."))
        (assoc opts :compile true :to target :output output
          :build {:target target :platform platform
                  :outputs (cond
                             (= target "html") [output (str (stem output) ".js")]
                             companion [output companion]
                             output [output]
                             :else [])}))))))

(defn normalized-path [ctx path]
  ((:absolute ctx) (str/replace path #"/+$" "")))

(defn check-outputs! [ctx opts]
  (let [paths (mapv #(normalized-path ctx %) (get-in opts [:build :outputs]))]
    (when-not (= (count paths) (count (distinct paths)))
      (fail "Compilation outputs must have different paths."))
    (doseq [path paths]
      (when ((:exists? ctx) path)
        (fail (str "Output already exists: " path)))
      (when (some #(str/starts-with? path (str % "/")) paths)
        (fail "Compilation outputs may not contain one another.")))))

(defn which [ctx command]
  (let [result ((:run ctx) ["bash" "-c" "command -v -- \"$1\"" "ys" command])]
    (when (= 0 (:exit result)) (str/trim (:out result)))))

(defn find-gloat [ctx]
  (let [override (install/setting ctx "YS_GLOAT")
        bin (install/parent (:exe ctx))
        adjacent (str bin "/gloat")]
    (or (when override
          (or (which ctx override) (fail "YS_GLOAT is not executable.")))
        (when (install/test-path ctx "-x" adjacent) adjacent)
        (which ctx "gloat")
        (do
          (when-not (and (= "bin" (basename bin))
                        (install/test-path ctx "-d" bin)
                        (install/test-path ctx "-w" bin))
            (fail "Installing Gloat requires ys under a writable PREFIX/bin/."))
          ;; Positional arguments keep prefix names out of shell source.
          (install/run! ctx "bash" "-c"
            (str "bootstrap=$(mktemp) || exit; "
              "trap 'rm -f -- \"$bootstrap\"' EXIT; "
              "curl -fsSL https://in-1.cc -o \"$bootstrap\" || exit; "
              "source \"$bootstrap\" gloat \"PREFIX=$1\"")
            "ys" (install/parent bin))
          (when-not (install/test-path ctx "-x" adjacent)
            (fail "Gloat installation did not produce PREFIX/bin/gloat."))
          adjacent))))

(def compiler-adapter "#!/usr/bin/env bash\ncat -- \"$YS_GLOAT_COMPILED\"\n")

(defn gloat-source [code version]
  ;; Gloat 0.1.81 consumes this four-form transport preamble. Keep it
  ;; independent of the public --to=clj+ dependency-loading bootstrap.
  (str "(require '[clojurestar.deps :as deps])\n"
    "(deps/add-deps '{:deps {org.yamlscript/ys.v0 {:mvn/version \""
    version "\"}}})\n"
    "(ns main (:require ys.v0))\n(ys.v0/init)\n\n" code))

(defn run-gloat! [ctx argv]
  (let [{:keys [exit out err]} ((:run ctx) argv)]
    (when-not (= exit 0)
      (fail (str "Gloat compilation failed: "
              (str/trim (if (seq err) err out)))))))

(defn html-reference [ctx html wasm]
  ;; The template uses a single-quoted JavaScript string containing a URL.
  (-> ((:relative ctx) (install/parent (normalized-path ctx html))
        (normalized-path ctx wasm))
    (str/replace "%" "%25") (str/replace " " "%20")
    (str/replace "#" "%23") (str/replace "?" "%3F")
    (str/replace "'" "%27") (str/replace "\\" "%5C")
    (str/replace "\n" "%0A") (str/replace "\r" "%0D")
    (str/replace "<" "%3C") (str/replace ">" "%3E")))

(defn publish! [ctx file output]
  (let [output (normalized-path ctx output)]
    (install/run! ctx "mkdir" "-p" (install/parent output))
    (when (or (install/test-path ctx "-e" output)
              (install/test-path ctx "-L" output))
      (fail (str "Output already exists: " output)))
    (let [stage (install/temp-dir ctx (install/parent output))
          payload (str stage "/payload")]
      (try
        (install/run! ctx "cp" "-pR" file payload)
        (install/run! ctx "mv" "-n" payload output)
        (when (install/test-path ctx "-e" payload)
          (fail (str "Output already exists: " output)))
        (finally (install/cleanup ctx stage))))))

(defn write-source! [ctx opts text]
  (if-let [output (:output opts)]
    (do
      (check-outputs! ctx opts)
      ((:write-source ctx) output text (= "bb" (:to opts))))
    (print text)))

(defn compile-artifacts! [ctx opts portable source]
  (check-outputs! ctx opts)
  (let [gloat (find-gloat ctx)
        {:keys [target platform outputs]} (:build opts)
        stage (install/temp-dir ctx (or (install/setting ctx "TMPDIR") "/tmp"))]
    (try
      (let [adapter (str stage "/compiler")
            compiled (str stage "/compiled.clj")
            input (if (and source (not= source "-")
                           (install/test-path ctx "-f" source))
                    (normalized-path ctx source) (str stage "/input.ys"))
            _ (when (= input (str stage "/input.ys"))
                ((:write ctx) input "!ys-0\n"))
            format (case target "h" "lib" "html" "js" target)
            ext (case format "lib" (if (str/starts-with? (or platform "") "windows/")
                                     ".dll" ".so")
                  "dir" "/" "js" ".js" "go" ".go" "wasm" ".wasm" "")
            artifact (str stage "/result" ext)
            html? (or (= target "html") (and (= target "js") (= 2 (count outputs))))
            argv (vec (concat ["env" (str "GLOAT_YS=" adapter)
                               (str "YS_GLOAT_COMPILED=" compiled)
                               gloat "--engine=glj" "--to" format "--out" artifact]
                        (when platform ["--platform" platform])
                        (when html? ["--ext=html"])
                        [input]))]
        ((:write ctx) adapter compiler-adapter)
        ((:write ctx) compiled portable)
        (install/run! ctx "chmod" "755" adapter)
        (run-gloat! ctx argv)
        (let [header (str stage "/result.h")
              html (str stage "/result.html")
              files (cond
                      (= target "h") [header]
                      (= target "html") [html artifact]
                      (and (= target "lib") (= 2 (count outputs))) [artifact header]
                      html? [artifact html]
                      :else [artifact])]
          (when html?
            (let [[html-out wasm-out] (if (= target "html") outputs (reverse outputs))]
              ((:write ctx) html
                (str/replace ((:read ctx) html) "fetch('result.js')"
                  (str "fetch('" (html-reference ctx html-out wasm-out) "')")))))
          (doseq [file files]
            (when-not (install/test-path ctx "-e" file)
              (fail (str "Gloat did not generate: " file))))
          (if (empty? outputs)
            (print ((:read ctx) artifact))
            (do
              (check-outputs! ctx opts)
              (doseq [[file output] (map vector files outputs)]
                (publish! ctx file output))))))
      (finally (install/cleanup ctx stage)))))

(def target-descriptions
  {"bin" "native binary" "lib" "shared library" "h" "FFI header"
   "dir" "Go code directory" "go" "Go code" "js" "browser Wasm"
   "html" "browser HTML" "wasm" "WASI Wasm"})

(defn compile! [ctx opts portable source]
  (let [target (get-in opts [:build :target])
        output (some-> (:output opts) (str/replace #"^\./" ""))
        description (str (target-descriptions target) ": '" output "'")
        finish (when (and output (:start-progress ctx))
                 ((:start-progress ctx)
                   (str "Compiling to " description)
                   (str "Compiled to " description)
                   (str "Failed to compile to "
                     (if (= target "bin") "binary" (target-descriptions target))
                     ": '" output "'")))
        success (atom false)]
    (try
      (compile-artifacts! ctx opts portable source)
      (reset! success true)
      (finally (when finish (finish @success))))))
