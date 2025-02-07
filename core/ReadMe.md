yamlscript/core
===============

The YS compiler and runtime written in Clojure


## Synopsis

```clojure
(do
  (require '[yamlscript.compiler :as ys])
  (-> "foo: bar baz"
    ys/compile))
=> "(foo bar baz)\n"
```


## Description

This directory builds the YS language written in Clojure.

The `yamlscript.compiler/compile` function takes a YS input string and compiles
it to a Clojure code string.


## Makefile usage

* `make test`

  This runs the test suite.
  Run with `make test v=1` for verbose output.
  Run with `w=1` to show reflection warnings.
