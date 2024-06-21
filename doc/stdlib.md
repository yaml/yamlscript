---
title: The YAMLScript Standard Library
---

The YAMLScript standard library is a collection of functions that are
available to all YAMLScript programs.
This document describes the functions in the standard library and how to use
them.

YAMLScript exposes most of the functions available in the [Clojure Core](
https://clojuredocs.org/core-library) standard library, and Clojure has great
documentation, so we won't be documenting those functions here.

However, sometimes YAMLScript provides a different interface to a standard
Clojure function.
We'll certainly cover those cases here.


## Standard Library Functions

* abs-path — Returns the absolute path of a file.

* call — Calls a function with the given arguments.

  <details><summary><strong>More</strong></summary>
  This function is useful with dot chaining when a function returns
  another function and you want to call it immediately.
  
  ```yaml
  !yamlscript/v0
  x =: 39
  say: x.fn([n] \(n + %)).call(3)
  ```
  </details>

* cwd — Returns the current working directory path as a string.


## See Also

