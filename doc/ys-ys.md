---
title: YS Internals Library
talk: 0
---

This library serves 2 purposes.
It provides functions for working with YS code from within a YS program/file.

It also provides functions that are wrappers around common Clojure functions so
that they can be used in places where functions are not allowed; like in [dot
chaining operations](chain.md).

You can use these functions with the `ys/` (or `ys::ys/`) prefix.


## YS Functions

* `compile` — Compile a YS string to a Clojure string

* `eval` — Evaluate a YS string

* `load-file` — Load a YS file path

* `load-pod` — Load a Babashka Pod

* `unload-pods` — Unload all loaded pods

* `use` — Use a YS or Clojure library found in `YSPATH`.
  Normally called as `use`, not `ys/use`.


## Using Modules

The `use` form loads a module and optionally aliases or refers its names.
Without a source option, YAMLScript searches the directories in `YSPATH`.
The `:path` option searches one directory, and `:file` loads one exact `.ys`,
`.clj`, or `.cljc` file.
The `:url` option loads an actual HTTP or HTTPS source-file URL.

```yaml
use foo::bar: :path './lib'
use foo::bar: :file './lib/foo/bar.ys'
use foo::bar: :url 'https://example.com/foo/bar.ys'
```

The `:deps` option accepts one coordinate supported by Grenadine's
`clojurestar.deps` library.
Maven coordinates use normal dotted group names.
Gist coordinates may name a file and full commit SHA.
GitHub coordinates name one source file at a branch, tag, or commit.

```yaml
use clojure::math::combinatorics:
  :deps 'mvn:org.clojure/math.combinatorics@0.3.0/clojure.math.combinatorics'
use mathy:
  :deps 'gist:ingydotnet/f70409675d234aa4f2fe379cd975a4f5/mathy.clj'
use grenadine::require-deps:
  :deps 'github:clojurestar/grenadine/v0.1.7/src/grenadine/require_deps.cljc'
```

The module name on the left must match the namespace provided by `:deps`.
Generic Git repositories and dependency coordinate maps are not accepted.

Use `:as` to add an alias, `:get` to refer selected names, `:all` to refer all
names, `:none` to refer none, and `:not` to refer all except selected names.
A slash in a `:get` name renames it, as in `old-name/new-name`.
With no selection option, `use` refers all public names.

```yaml
use foo::bar: :as bar :get one two/second
use foo::baz: :all :not internal
use foo::quux: :as quux :none
```


## Macro Wrapper Functions

* `for` — An eager version of Clojure's lazy `for` macro
* `if` — Wrapper around the Clojure `if` special form
* `when` — Wrapper around the Clojure `when` macro
