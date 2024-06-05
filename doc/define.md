---
title: Defining Variables and Functions
---

Two of the most common things you'll want to do in YAMLScript code is to define
variables and functions.

It's very simple.
Here's an example:

```yaml
!yamlscript/v0

name =: 'world'

defn main():
  greeting =: 'Hello'
  say: "$greeting, $name!"
```

To define a variable with a value we specify a symbol name, followed by one or
more spaces, followed by `=:`. for the YAMLScript key (the LHS).
The variable will be set to the result of the evaluation of the mapping pair's
value (the RHS).

To define a function we use `defn`, followed by the function name followed by
the parenthesized arguments for the LHS.
The RHS is the function body.

Let's see how this compiles to Clojure internally using `ys -c file.ys`:

```
(def name "world")
(defn main [] (let [greeting "Hello"] (say (str greeting ", " name "!"))))
(apply main ARGS)
```

In our YAMLScript code we defined 2 variables: `name` and `greeting`.
But in the Clojure code one became a `def` expression and the other used `let`.
Using `=:` outside a function uses `def` and it's a file scope variable.
Using `=:` inside a function uses `let` and the scope is the remainder of the
function.
This is idiomatic Clojure.
