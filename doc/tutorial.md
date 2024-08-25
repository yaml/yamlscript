---
title: YAMLScript Syntax Basics
---


Let's start learning about YAMLScript syntax by looking at a simple YAMLScript
program.

Here's a program to print some numbers (10 of them by default) from the
fibonacci sequence:

```yaml
!yamlscript/v0

defn main(n=10):
  loop a 0, b 1, i 1:
    say: a
    when i < n:
      recur: b, (a + b), i.++
```

When you run a YAMLScript program using the `ys` interpreter command it:

* Compiles the YAMLScript code to Clojure code
* Evaluates the Clojure code

Let's try it (requesting 8 numbers from the fibonacci sequence):

```sh
$ ys fib.ys 8
0
1
1
2
3
5
8
13
```

Great, but to understand it better let's look at the Clojure code that it
compiled to:

```sh
$ ys -c fib.ys
(defn main
 ([n]
  (loop [a 0 b 1 i 1]
   (say a)
   (when (< i n) (recur b (add+ a b) (inc i)))))
 ([] (main 10)))
(apply main ARGS)
```

> Note: When learning or debugging YAMLScript programs, it's often very helpful
> to look at the generated Clojure code using `ys -c`.

We can see 2 top-level forms in the generated Clojure code.
The first is the `main` function definition, and the second is the call to the
`main` function with the command-line arguments.
That's interesting, we didn't actually call `main` in our ys program.

The YS compiler automatically adds a call to `main` if it detects the `main`
was defined but not called in the program.

The Clojure `main` function has 2 bodies, one with an argument `n` and the other
without any arguments.
In the YAMLScript program there is only one function body, but not that the
`n` argument has a default value of `10`.
Clojure doesn't support default arguments, but the YS compiler can generate a
second body that takes no arguments and calls the first body with the default.


