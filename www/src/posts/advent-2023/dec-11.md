---
title: The YeS Express
date: '2023-12-11'
tags: [blog, advent-2023]
permalink: '{{ page.filePathStem }}/'
author:
  name: Ingy döt Net
  url: /about/#ingydotnet
---


Santa's got a lot of ground to cover in a short amount of time.
He doesn't have time to deal with confusing maps and directions.

Lisp has other-worldly powers of abstraction, but when it comes to reading
syntax, most people prefer the familiarity of this world.

YAMLScript fully embraces all that Clojure has to offer, but syntax-wise it
also offers a more familiar face.


### Welcome to Day 11 of the YAMLScript Advent Calendar!

Today we'll look at YAMLScript Expressions, aka YeS-Expressions.
Lisp has S-Expressions, YAMLScript has YeS-Expressions!

When writing YAMLScript instructions in "code mode" you are always working with
3 basic YAML elements:

* YAML Block Mappings - The indented style `key: value` pairs
* YAML Quoted Scalars - String literals
* YAML Plain Scalars - The unquoted `value` scalars

YAMLScript treats all YAML plain scalars as YeS-Expressions (ysexprs).

Let's look at some examples.


### Infix Expressions

```yaml
x =: (y * 2)
z =: (x + 5 + y)
```

Compiles to Clojure (Lisp) like this:

```clojure
(def x (* y 2))
(def z (+ x 5 y))
```

YeS-Expressions support infix operators for very simple expressions.
If you put exactly 3 forms in parentheses, and the second one is an operator,
then the first and third forms are operands.
The compiler will swap the first and second forms making it work like Lisp wants
it to.

If you have a parenthesized expression with more than 2 operands and the
operators are all the same, then the compiler will make a Lisp form starting
with the operator and followed by the operands.

> Note: In rare cases in Lisp `(a + 1)` is valid when `a` is a function that
takes two arguments, an operator function add a number in this case.
There are ways to specifiy this in YAMLScript, but we won't cover them here.

In simple cases where the infix expression is the only thing in the YAML scalar,
you can omit the parentheses.

```yaml
x =: y * 2
z =: x + 5 + y
```

Note that YAMLScript has no support for operator precedence.
You must use parentheses to group expressions and group operations in triplets.

```yaml
x =: ((y * 2) + 5) / 2
```


### Prefix Expressions

In Lisp you might see a function call like this:

```clojure
(abspath (join "/" ["foo" "bar"]))
```

Many non-Lisp languages would write this as:

```javascript
abspath(join("/", ["foo", "bar"]))
```

YAMLScript supports this style of function call as well.

YAMLScript is very flexible about how you can write function calls.
You could do the above in all the ways below:

```yaml
path1 =: abspath(join("/" ["foo" "bar"]))
path2 =: (abspath (join "/" ["foo" "bar"]))
path3 =:
  abspath:
    join "/": ["foo" "bar"]
=>: (def path4 abspath(join("/" ["foo" "bar"])))
```

Look closely at the last example (`path4`).
It looks like a Clojure form, but it actually makes use of prefix function
calls.


### Special Operators

YAMLScript has a few special operators that are not part of Clojure.

The `..` operator is used to create a range of consectutive integers.
The expression `(1 .. 3)` is equivalent to the Clojure form `(range 1 4)` and
evaluates to the list `(1 2 3)`.
The expression `(3 .. 1)` is equivalent to the Clojure form `(range 3 0 -1)` and
evaluates to the list `(3 2 1)`.

The `+` and `*` operators are polymorphic.

* `"x" * 3` -> `"xxx"`
* `"x" + "y" + "z"` -> `"xyz"`
* `{a: 1} + {b: 2}` -> `{a: 1, b: 2}`
* `[1 2] + [3 4]` -> `[1 2 3 4]`


### Special Thanks

I'd like to say thank you to David A. Wheeler for his web page [Curly infix,
Modern-expressions, and Sweet-expressions: A suite of readable formats for
Lisp-like languages](
https://dwheeler.com/readable/sweet-expressions.html).

I adapted many of those ideas to make YeS-Expressions.


### Conclusion

As I've said before, YAMLScript is very flexible, allowing you to decide how
much Lisp or YAML style you want to use at any point.
YeS-Expressions allow you to lean to the Lisp but keep it readable for your
non-Lisper friends.

Join me tomorrow for Day 12 of the YAMLScript Advent Calendar.


{% include "../../santa-secrets.md" %}
