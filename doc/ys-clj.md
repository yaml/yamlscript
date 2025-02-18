---
title: Replaced Clojure Functions
---

A number of standard functions from Clojure's standard library `clojure::core`
have been replaced by functions in the `ys::std` library.
In general the replacements have the same intent as the original functions, but
from a YS perspective.

When you absolutely need the original Clojure functions, they are available in
this `ys::clj` library.
These functions are automatically available in YAMLSCript by using the `clj/`
prefix.


## Functions

The documentation for these functions is available in the Clojure documentation
web site, so we include a link to there in each definition.


* `clj/compile` — [In Clojure](https://clojuredocs.org/clojure.core/compile) the
  `compile` function compiles a namespace into a set of class files.
  In YS it converts a YS source code string into a
  Clojure source code string.

* `clj/load` — [In Clojure](https://clojuredocs.org/clojure.core/load) the
  `load` function loads a file from the classpath.
  In YS it's an alias for `load-file`(below).

* `clj/load-file` — [In Clojure](https://clojuredocs.org/clojure.core/load-file)
  the `load-file` function loads a Clojure file from a given file path.
  In YS it loads a YS file from a given file path.

* `clj/num` — [In Clojure](https://clojuredocs.org/clojure.core/num) the `num`
  function converts a Java number to a Clojure number.
  In YS it converts a numeric string to a number.

* `clj/use` — [In Clojure](https://clojuredocs.org/clojure.core/use) the `use`
  function is used to refer a namespace into the current namespace.
  In YS it loads a YS or Clojure library from `YSPATH`.
