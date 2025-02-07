---
title: YS Operators
---

YS has a number of operators that you can use in your code.

Review [YeS Expressions](yes.md) to see how YS supports infix operator
expressions, whereas Clojure requires the operator to precede the operands.

When YS operators are used with infix notation, they often become
polymorphic and do things according to the types of the operands.


```yaml
!yamlscript/v0
s =: ('foo' + 'bar')  # => 'foobar'
s =: (+ 'foo' 'bar')  # ERROR - Clojure + only works on numbers
```

You can see why by looking at the `ys -c` output:

```sh
$ ys -c -e "s =: ('foo' + 'bar')" -e "s =: (+ 'foo' 'bar')"
(def s (add+ "foo" "bar"))
(def s (+ "foo" "bar"))
```

See how the infix `+` operator compiles to the `add+` function?
The `add+` function works on numbers, strings, sequences, and mappings!

!!! note

    If you absolutely need the Clojure `+` function for performance reasons,
    you can simply use the prefix form: `(+ a b)`.


## Arithmetic Operators

* `+` - Addition - Infix works on strings, sequences, and mappings or else casts
  arguments to numbers.
* `-` - Subtraction - Works on numbers.
* `*` - Multiplication - Infix works on numbers or `str * num`, `num * str`,
  `seq * num`, `num * seq`.
* `/` - Division - Works on numbers. Infix returns a double in the cases where
  Clojure would return a ratio.
* `%` - Remainder - Works on numbers. Compiles to `rem` in Clojure.
* `%%` - Modulus - Works on numbers. Compiles to `mod` in Clojure.
* `**` - Exponentiation - Works on numbers and has right associativity.


## Comparison Operators

* `==` - Equal To - Works on any comparable values.
* `!=` - Not Equal To - Works on any comparable values.
* `>` - Greater Than - Works on numbers. Supports `a > b > c`.
* `>=` - Greater Than or Equal To - Works on numbers.
* `<` - Less Than - Works on numbers.
* `<=` - Less Than or Equal To - Works on numbers.

These operators have the respective named functions: `eq`, `ne`, `gt`, `ge`,
`lt`, `le` for use in places where a function makes more sense than an operator.


## Conditional Operators

In Clojure `false` and `nil` are treated as "false" and everything else is
treated as "true".

YS adds the concept of "truey" and "falsey" values.
Empty strings, empty collections, `0`, `false`, and `nil` are "falsey" and
everything else is "truey".

This concept applies to some operators.

* `&&` - Logical And
* `||` - Logical Or
* `&&&` - Truey And
* `|||` - Truey Or


## Other Operators

* `.` - Function Chaining -
  `a.b.3.c(d).e(f)` -> `(e (c (nth (get+ a 'b) 3) d) f)`.
* `..` - Range - `1 .. 3` -> `(1 2 3)`, `3 .. 1` -> `(3 2 1)`.
  Differs from Clojure's [`range`](https://clojuredocs.org/clojure.core/range)
  function.
* `=~` - Regex Find. Compiles to [`re-find`](
  https://clojuredocs.org/clojure.core/re-find) in Clojure.
  Returns what `re-find` returns.
