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

vector [{"name" ["abs-path" {"str" "relative-path"} {"str" "absolute-path"}], "desc" "Returns the absolute path of a file."} {"name" ["call" [{"fn" "function"} {"any" "argument*"}] {"any" "value"}], "desc" "Calls a function with the given arguments.", "more" "This function is useful with dot chaining when a function returns\nanother function and you want to call it immediately.\n\n```yaml\n!yamlscript/v0\nx =: 39\nsay: x.fn([n] \\(n + %)).call(3)\n```\n"} {"name" ["cwd" [] {"str" "path"}], "desc" "Returns the current working directory path as a string."}]

## See Also


