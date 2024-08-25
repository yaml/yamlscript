---
title: Chaining Function Calls
---

One of the most powerful features of YAMLScript is the ability to chain function
calls together.
Functions are chained together using the `.` operator.

Here are some examples:

```yaml
x.5                 # -> (nth x 5)
x.abc               # -> (get+ x 'abc)
x.foo()             # -> (foo x)
x.foo(abc 123)      # -> (foo x abc 123)
x.foo(abc _ 123)    # -> (foo abc x 123)
x.foo(abc 123 _)    # -> (foo abc 123 x)
x.foo(_ _ _)        # -> (foo x x x)
x.?                 # -> (truey? x)
x.!                 # -> (falsey? x)
x.++                # -> (inc x)
x.--                # -> (dec x)
x.#                 # -> (count x)
x.#?                # -> (not (empty? x))
x.#!                # -> (empty? x)
x.#++               # -> (inc (count x))
x.#--               # -> (dec (count x))
x.>                 # -> (DBG x)
x.abc.5.foo(22).#++ # -> (inc (count (foo (nth (get+ x 'abc) 5) 22)))
x.>.abc.>.foo()     # -> (foo (DBG (get+ (DBG x) 'abc)))
```

The `get+` looks up a key in a map (like the `get` function in Clojure) but
given `x.y` looks for the string key `"y"` or the keyword `:y` or the symbol
`'y` in the map `x`.

Instead of needing to write `(:k y)` or `(get x "y")` or `(get x 'y)` depending
on the type of the key, you can just write `x.y`.

When `.` is used to call a function, the value of the LHS is passed as the first
argument to the function on the RHS.
When this is not the desired behavior, you can use `_` to indicate the position
that the value should be passed as.

Some core functions like `take` and `drop` (when called used the `.` operator)
will automatically put the collection argument as the second argument.
