---
title: The YAMLScript Standard Library
---

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


```markys:func-list
- - abspath
  - - relpath abspath
    - relpath basepath abspath
  - Returns the absolute path of a file.
    The `basepath` is `CWD` by default.

- - call
  - - funcref any* any
  - Calls a function with the given arguments.
  - more: |
      This function is useful with dot chaining when a function returns
      another function and you want to call it immediately.

      ```yaml
      !yamlscript/v0
      x =: 39
      say: x.fn([n] \(n + %)).call(3)
      ```

- - cwd
  - - abspath
  - Returns the current working directory path as a string.
    Also available as the `CWD` global variable.
```


## See Also

```markys:include
file: library-list.md
yank: ys::std
```
