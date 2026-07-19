;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The ys.v0.manifest namespace is the single source of truth for what the
;; YS runtime injects into user code. It is consumed by ys.v0/init (for
;; babashka and JVM Clojure) and by yamlscript.runtime (for the SCI context
;; inside the ys binary). It is pure data; no code.

(ns ys.v0.manifest)

;; Namespaces whose vars are referred into the user's namespace.
;; :all means every public var. Order matters: later wins on conflict.
(def refers
  '[[ys.v0.std :all]
    [ys.v0.dwim :all]
    [ys.v0.util [condf]]
    [ys.v0.debug [DBG PPP TTT WWW XXX YYY ZZZ]]
    [clojure.pprint [pprint]]])

;; Runtime variables. ys.v0/init refers these from ys.v0.global and binds
;; them; the ys runtime provides SCI dynamic vars of the same names.
(def runtime-vars
  '[_ ARGS ARGV CWD DIR ENV FILE INC PUN RUN VERSION])

;; clojure.core functions that SCI does not provide but babashka and JVM
;; Clojure do. The ys runtime adds these to its clojure.core; ys.v0/init
;; leaves them alone.
(def sci-core-extras
  '[abs file-seq infinite? parse-double parse-long parse-uuid
    random-uuid slurp spit NaN?])

;; clojure.core functions overridden by the ys runtime with SCI-aware
;; implementations (compiler-backed load and use).
(def runtime-overrides
  '[load use])

;; Namespaces that some Clojure runtimes lack (glojure bundles neither
;; the clojure.* ones nor the babashka.fs backend that ys.v0.fs and
;; ys.v0.taptest pull in). ys.v0 requires them via a guarded loop and
;; only aliases/refers the ones that loaded.
(def optional-nses
  '[clojure.java.io
    clojure.math
    clojure.pprint
    clojure.set
    clojure.tools.cli
    ys.v0.fs
    ys.v0.taptest])

;; Namespace aliases available to user code. ys.v0/init sets these up with
;; clojure.core/alias; the ys runtime maps them to SCI namespaces.
(def aliases
  '{std     ys.v0.std          ys.std     ys.v0.std
    clj     ys.v0.clj          ys.clj     ys.v0.clj
    ys      ys.v0.ys           ys.ys      ys.v0.ys
    cli     clojure.tools.cli  ys.cli     clojure.tools.cli
    csv     ys.v0.csv          ys.csv     ys.v0.csv
    ext     ys.v0.ext          ys.ext     ys.v0.ext    x ys.v0.ext
    fs      ys.v0.fs           ys.fs      ys.v0.fs
    http    ys.v0.http         ys.http    ys.v0.http
    io      clojure.java.io    ys.io      clojure.java.io
    json    ys.v0.json         ys.json    ys.v0.json
    math    clojure.math       ys.math    clojure.math
    set     clojure.set        ys.set     clojure.set
    str     clojure.string     ys.str     clojure.string
    walk    clojure.walk       ys.walk    clojure.walk
    yaml    ys.v0.yaml         ys.yaml    ys.v0.yaml
    ys.taptest ys.v0.taptest})

(defn exported-syms
  "Return the set of symbols that init will refer into the user
  namespace. Namespaces this runtime could not load contribute
  nothing."
  []
  (into #{}
    (mapcat (fn [[ns-sym syms]]
              (when (find-ns ns-sym)
                (if (= :all syms)
                  (keys (ns-publics ns-sym))
                  syms))))
    refers))

(comment
  )
