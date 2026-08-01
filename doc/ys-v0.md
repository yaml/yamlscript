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

Use the `-T` / `--to` option with a code target (`bb`, `clj`, `jolt` or
`glj`)
and ys will compile (no `-c` needed; a code target implies it) with this
header added to the output:

```clojure
(ns main (:require ys.v0))
(ys.v0/init)
```

plus a target specific form that resolves the ys.v0 dependency at run
time.

The `ys.v0/init` call sets up the namespace to work like the ys runtime:

* All of the YS standard library functions and macros are referred in
* The YS namespace aliases (`str/`, `json/`, `fs/`, etc) are set up
* The YS runtime variables (`ARGS`, `ENV`, `FILE`, `CWD`, etc) are bound

The same compiled file still runs under ys itself (`ys -C file.clj`),
where the header is a no-op.


## Babashka

```bash
$ ys -T bb program.ys | bb /dev/stdin
```

The `-T bb` header form (active only under Babashka; it does nothing on
other runtimes) puts the `ys.v0` jar and its `data.json` dependency on the
classpath with `babashka.classpath/add-classpath` when they are already in
`~/.m2`, and otherwise fetches them with `babashka.deps/add-deps`.
The `add-classpath` path needs no `java` at all; the `add-deps` fallback
runs `java` once to resolve the dependencies from Clojars.

Installing a ys release also installs those two jars into your
`~/.m2/repository`, so the java free path works out of the box.
The jars are bundled in the release archive; `make install` skips them
when run as root (system installs), and you can install them per user at
any time with:

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


## Jolt

Jolt is a new Clojure dialect hosted on Chez Scheme.

```bash
$ ys -T jolt program.ys > program.clj
$ jolt program.clj
```

The `-T jolt` header form resolves the ys.v0 dependency with
`jolt.deps/add-deps` when running under jolt. Jolt uses Grenadine to build
the effective Maven POM and resolve transitive dependencies without Java.
The header also adds jolt's own
libyaml based yaml library so the YS yaml functions work natively.


## Glojure

[Glojure](https://github.com/glojurelang/glojure) is a Clojure dialect
implemented in Go.
This needs a Glojure release newer than 0.6.8 (where `ns-unmap` was
broken).

```bash
$ ys -T glj program.ys > program.clj
$ glj program.clj
```

Glojure loads plain Clojure source from a load path (no jars), so the
`-T glj` header form adds the extracted ys.v0 sources to the load path
at run time: the `YS_V0_PATH` env var if set, else the
`~/.m2/repository/org/yamlscript/ys.v0/<version>/ys.v0-<version>.jar.d`
directory that the ys installers and `ys-sh --install-m2` maintain.

Backend libraries (json, yaml, shell, http, fs) don't exist on glojure
yet, so those YS functions fail with a clear message there; ordered
maps fall back to array-map (insertion order is kept for literals of
any size, and lost when assoc grows a map past eight entries).


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

The yaml, json, http, shell and ordered map functions resolve their
backend libraries (clj-yaml, data.json, http-client, process, flatland
ordered) lazily at first call, so the library loads even on runtimes
that can't load those backends (like jolt, currently); the functions
themselves fail with a clear message there.

Data documents (which ys prints as YAML output) are evaluated but not
printed when running compiled code on other runtimes.
