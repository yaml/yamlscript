---
title: Control Flow
talk: 0
---


YS (iow Clojure) is a functional programming language.
That means that everything is a function.
Running a program is just a calling a function that calls other functions.

Even though YS tries hard to look like an imperative language, you must
keep in mind that it's functional and get familiar with how the flow works.

This document will cover:

* Starting a program
* Variable scope
* Grouping expressions
* Looping functions and recursion
* Conditional expressions

!!! note

    Some of the things that are called "functions" in in this document are
    actually "macros" or "special forms" in Clojure.
    The distinction is not particularly important here, but worth mentioning.


## Starting a Program

A YS file generally is just a bunch of `defn` calls to define functions.

Sometimes there will be assignment expressions at the top level.
Top level variables are global and can be used anywhere in the program.
They belong to the file's namespace which is `main` by default.

If a program defines a function called `main`, it will be called when the
program is run.
Any command line arguments will be passed to the `main` function after being
cast to numbers if they look like numbers.

`program.ys`:

```yaml
!YS-v0

defn main(word='Hello!' times=3):
  each i (1 .. times):
    say: "$i) $word"
```

```sh
$ ys program.ys
1) Hello!
2) Hello!
3) Hello!
```

```sh
$ ys program.ys YS 2
1) YS
2) YS
```

If a program does not define a `main` function, then nothing will happen unless
you've defined a top level function call yourself.
YS files that are meant to be used as libraries will not have a `main` function
or a top level function call.


## Variable Scope

One cool thing about YS (and Clojure) is that you can use any word as a
variable name.
Even things like `if` and `for` which are reserved words in many languages.

For example you might do this:

```yaml
defn foo(list):
  count =: count(list)
  if count > 0:
    say: 'The list is not empty'
  else:
    say: 'The list is empty'
```

Once we bind `count` to the result of the `count` function, we can't use the
`count` function again in that scope.
Often this is just fine.
And it feels nice that you don't have to think up a synonym or alternative
mangling for `count`.


## Grouping Expressions

Some expression contexts allow multiple expressions to be grouped together and
some only allow a single expression.

You can group multiple expressions together with a `do` function call when you
need to to do multiple things in a context that only allows a single expression.

```yaml
if a > b:
  do:
    say: 'a is greater than b'
    say: 'a must be HUGE!'
  say: 'Nothing to see here'
```

!!! note

    The `if` function actually supports the better named `then` and `else`
    words for grouping, but `do` can also be used.


## Looping Functions and Recursion

YS has a few looping functions: `loop`, `reduce`, `for` and `each`.

These will be documented in more detail in the future, but for now you can see
the Clojure documentation for them:

* [loop](https://clojuredocs.org/clojure.core/loop)
* [reduce](https://clojuredocs.org/clojure.core/reduce)
* [for](https://clojuredocs.org/clojure.core/for)
* `each` is a YS function that calls to a `for` expression inside a
  `doall` expression.
  This allows you to print things in the loop body.
  In every other way it's the same as `for`.


## Conditional Expressions

YS has a few common conditional expressions: `if`, `when`, `cond` and `case`.


### The `if` Function

In YS, an `if` expression is a function that takes 3 arguments: a
condition, a then expression and an else expression.

```yaml
if a: b c  # If a is true, return b, else return c
```

The `b` and `c` expressions can also be mapping pairs:

```yaml
if a:
  say: 'yes'
  say: 'no'
```

Sometimes you want to do more than one thing in the then or else expression:

```yaml
if a:
  then:
    say: 'yes'
    say: 'yes'
  else:
    say: 'no'
    say: 'no'
```

If you use `then` you must also use `else`, but `else` can be used without a
`then`:

```yaml
if a:
  say: 'yes'
  else:
    say: 'no'
    say: 'no'
```

Since `if` is a function, it has a return value.

```yaml
say:
  if a: -'yes' 'no'
```

Any variable assigned in the `then` or `else` expression will only apply to that
expression and not to the surrounding scope.

```yaml
x =: 1
if a > b:
  x =: 2
  x =: 3
=>: x    # => 1
```

What you want to do here is capture the result of the `if` expression:

```yaml
x =:
  if a > b:
    then: 2
    else: 3
=>: x    # => 2 or 3
```

Note that `say` returns `nil`, so all the `if` expressions above would also
return `nil`.


### The `when` Function

YS also has a `when` function that is like `if` but without an else expression.

```yaml
when a:
  say: 'yes'
```

The `if` function should only be used wen you have a then and an else
expression.
Otherwise, use `when`.

One thing about `when` is that its body can have multiple expressions.

```yaml
when a:
  say: 'yes'
  say: 'yes'
```


### The `cond` Function

The `cond` function is like a series of `if` expressions but with multiple
conditions.

```yaml
size =:
  cond:
    a < 20:  'small'
    a < 50:  'medium'
    a < 100: 'large'
    else:    'huge'
```


### The `case` Function

The `case` function is like a `cond` but with a single expression to compare
against.

```yaml
count =:
  case a:
    1: 'one'
    2: 'two'
    3: 'three'
    else: 'many'
```
