---
title: The YAMLScript Standard Library
---

<!-- DO NOT EDIT — THIS FILE WAS GENERATED -->

The YAMLScript standard library is a collection of functions that are available
to all YAMLScript programs.
This document describes the functions in the standard library and how to use
them.

YAMLScript exposes most of the functions available in the [Clojure Core](
https://clojuredocs.org/core-library) standard library, and Clojure has great
documentation, so we won't be documenting those functions here.

However, sometimes YAMLScript provides a different interface to a standard
Clojure function.
We'll certainly cover those cases here.


## Standard Library Functions

This is a list of the functions available in the YAMLScript Standard Library
(`ys::std').

You do not need to fully qualify them my namespace to use them.
You can simply call them my name.

All of these calls are the same:

```yaml
say: 'Hello'
std/say: 'Hello'
ys::std/say: 'Hello'
```

### Functions
<div class="func-list">

* **abspath** — Returns the absolute path of a file. The `basepath` is `CWD` by default.
  * abspath(relpath) -> abspath
  * abspath(relpath basepath) -> abspath

* **call** — Calls a function with the given arguments.
  * call(funcref any*) -> any

  <details>
  <summary
  style="font-size:smaller;
         font-weight:bold;
         color:#888">More</summary>
  This function is useful with dot chaining when a function returns
  another function and you want to call it immediately.

  ```yaml
  !yamlscript/v0
  x =: 39
  say: x.fn([n] \(n + %)).call(3)
  ```
  </details>

* **cwd** — Returns the current working directory path as a string. Also available as the `CWD` global variable.
  * cwd() -> abspath
</div>

## See Also


* [`ys::clj`](/doc/ys-clj) — Original Clojure functions shadowed by YS
* [`ys::ys`](/doc/ys-ys) — Special YS Functions
* [`ys::yaml`](/doc/ys-yaml) — Standard YAML processing library
* [`ys::json`](/doc/ys-json) — Standard JSON processing library
* [`clojure::core`](/doc/clj-core) — Clojure Core Library Essentials
