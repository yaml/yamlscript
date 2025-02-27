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


## Macro Wrapper Functions

* `for` — An eager version of Clojure's lazy `for` macro
* `if` — Wrapper around the Clojure `if` special form
* `when` — Wrapper around the Clojure `when` macro
