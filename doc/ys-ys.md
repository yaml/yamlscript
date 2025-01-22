---
title: YAMLScript YAMLScript Library
---

This library serves 2 purposes.
It provides functions for working with YAMLScript code from within a YAMLScript
program/file.

It also provides functions that are wrappers around common Clojure functions so
that they can be used in places where functions are not allowed; like in [dot
chaining operations](chain.md).

You can use these functions with the `ys/` (or `ys::ys/`) prefix.


## YAMLScript Functions

* `compile` — Compile a YAMLScript string to a Clojure string

* `eval` — Evaluate a YAMLScript string

* `load-file` — Load a YAMLScript file path

* `load-pod` — Load a Babashka Pod

* `unload-pods` — Unload all loaded pods

* `use` — Use a YAMLScript or Clojure library found in `YSPATH`.
  Normally called as `use`, not `ys/use`.


## Macro Wrapper Functions

* `for` — An eager version of Clojure's lazy `for` macro
* `if` — Wrapper around the Clojure `if` special form
* `when` — Wrapper around the Clojure `when` macro
