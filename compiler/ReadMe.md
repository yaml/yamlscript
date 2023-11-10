yamlscript/compiler
===================

The YAMLScript compiler written in Clojure


## Synopsis

```clj
(do
  (require '[yamlscript.core :as ys])
  (-> "foo: bar baz"
    ys/compile))
=> "(foo bar baz)\n"
```


## Description

This directory builds the YAMLScript compiler written in Clojure.

The `yamlscript.core/compile` function takes a YAMLScript input string and
compiles it to a Clojure code string.


## Makefile usage

* `make test`

  This runs the test suite.
  Run with `make test v=1` for verbose output.
  Run with `w=1` to show reflection warnings.
