---
title: Running Compiled YS on Other Clojure Runtimes
talk: 0
---

YS code compiles to Clojure code.
Normally the `ys` command line tool compiles and runs it for you in one step,
but you can also compile it once with `ys -c` and run the result on another
Clojure runtime like [Babashka](https://babashka.org) or JVM Clojure.

To do that, the compiled code needs the YS standard library.
It is published to Clojars as
[`org.yamlscript/ys.v0`](https://clojars.org/org.yamlscript/ys.v0).

Use the `-T` / `--to` option with a code target (`bb`, `clj` or `compat`)
and ys will compile (no `-c` needed; a code target implies it) with this
header added to the output:

```clojure
(ns main (:require ys.v0))
(ys.v0/init)
```

plus a target-specific form that resolves the ys.v0 dependency at run
time.

The `ys.v0/init` call sets up the namespace to work like the ys runtime:

* All of the YS standard library functions and macros are referred in
* The YS namespace aliases (`str/`, `json/`, `fs/`, etc) are set up
* The YS runtime variables (`ARGS`, `ENV`, `FILE`, `CWD`, etc) are bound

The `bb` and `clj` compiled files still run under ys itself
(`ys -C file.clj`), where their dependency headers are no-ops.
The `compat` header requires a runtime that supplies `clojurestar.deps`.


## Babashka

```bash
$ ys -T bb program.ys | bb /dev/stdin
```

The `-T bb` header form (active only under Babashka; it does nothing on
other runtimes) puts the `ys.v0` jar on the classpath from `~/.m2` with
`babashka.classpath/add-classpath`.
It does not run a dependency resolver and needs no `java`.

Installing a ys release also installs this jar into your `~/.m2/repository`,
so the java free path works out of the box.
The jar is bundled in the release archive; `make install` skips it when run as
root (system installs), and you can install it per user at any time with:

```bash
$ ys-sh-0.2.29 --install-m2
```

(Homebrew installs print this as a caveat, since brew cannot write to
your home directory.)

With `-o` / `--output`, the compiled file also gets a `#!/usr/bin/env bb`
shebang line and the executable bit, so it is a ready to run script:

```bash
$ ys -T bb program.ys -o program
$ ./program
```

(The shebang line is a comment to Clojure readers, so the same file still
runs under `ys -C` and JVM Clojure.)


## JVM Clojure

```bash
$ ys -T clj program.ys > program.clj
$ clojure -M program.clj
```

The `-T clj` header form is a no-op when `ys.v0` is already on the
classpath (or under the ys, Babashka and jolt runtimes); otherwise it
resolves the dependency at run time with `clojure.repl.deps/add-libs`,
which needs Clojure 1.12 or later.
You can also skip the runtime resolution by providing the dependency
yourself:

```bash
$ clojure -Sdeps '{:deps {org.yamlscript/ys.v0 {:mvn/version "0.2.29"}}}' \
    -M program.clj
```


## Compatible Clojure Runtimes

The `compat` target generates one program for Jolt, Glojure and Gobb.

```bash
$ ys -T compat program.ys > program.clj
$ jolt program.clj
$ glj program.clj
$ gobb program.clj
```

The generated dependency header is:

```clojure
(require '[clojurestar.deps :as deps])
(deps/add-deps
 '{:deps {org.yamlscript/ys.v0 {:mvn/version "0.2.29"}}})
```

These runtimes supply the dialect-neutral `clojurestar.deps` API.
It resolves ys.v0 and its transitive Maven dependencies, then adds the
required source roots to the running dialect.


## Limitations

A few standard library functions need the YS compiler and only work under
the ys runtime.
On other runtimes they fail with a clear error message:

* `eval` — evaluates YS source code
* `load` — loads a YS file
* `use` — loads YS modules
* `load-url` — loads YS code from a URL
* `new` — reflective constructor calls (works on JVM Clojure but not
  Babashka)

The yaml, json, http, shell and ordered map functions resolve their backend
libraries (clj-yaml, data.json, http-client, process, flatland ordered) lazily
at first call.
The library still loads on runtimes that cannot load those backends; the
functions themselves fail with a clear message there.

Data documents (which ys prints as YAML output) are evaluated but not
printed when running compiled code on other runtimes.
