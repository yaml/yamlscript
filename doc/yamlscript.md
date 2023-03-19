YAMLScript
==========

Programming in YAML


## Synopsis

A YAMLScript program `99-bottles.ys`:
```
#!/usr/bin/env yamlscript

main(number=99):
  map:
  - println
  - map(paragraph, range(number, 0, -1))

paragraph(num): |
  $(bottles, num) of beer on the wall,
  $(bottles, num) of beer.
  Take one down, pass it around.
  $(bottles, (num - 1)) of beer on the wall.

bottles(n):
  cond: [
    (n == 0), "No more bottles",
    (n == 1), "1 bottle",
    :else,    "$n bottles" ]
```

Run: `yamlscript 99-bottles.ys 3`

```
3 bottles of beer on the wall,
3 bottles of beer.
Take one down, pass it around.
2 bottles of beer on the wall.

2 bottles of beer on the wall,
2 bottles of beer.
Take one down, pass it around.
1 bottle of beer on the wall.

1 bottle of beer on the wall,
1 bottle of beer.
Take one down, pass it around.
No more bottles of beer on the wall.
```

Use the YAMLScript REPL:
```
$ yamlscript
Welcome to YAMLScript [perl]

user=> nums =: range(1 4)
user/nums
user=> nums
(1 2 3)
user=> map: [ println, nums ]
1
2
3
(nil nil nil)
user=> <ctrl-D>         # to exit
$
```


## Status

This is very ALPHA software.
Expect things to change.


## Description

YAMLScript is a programming language that uses YAML as a base syntax.
It feels like a YAML encoded Lisp, but with fewer parentheses.

In fact YAMLScript *is* a Lisp.
It's a YAML-based specialized syntax reader for the [Lingy](
https://metacpan.org/dist/Lingy/view/lib/Lingy.pod) programming language.
**Lingy** is a port of the **Clojure** language to other languages (like Perl).
Clojure is a Lisp hosted by the Java JVM.


## Installation

YAMLScript is currently only available as a Perl CPAN module.
You install it like so:

```
$ cpanm YAMLScript
```

Once installed you will have access to the `yamlscript` CLI command.
You will also have library support to invoke YAMLScript directly from Perl.


## YAMLScript Language Capabilities

* Variable binding

  ```
  name =: 'world'
  ```

  Variable names use lowercase letters `a-z`, digits `0-9` and must start with
  a letter.
  Name parts may be separated by a dash `-`.

* Variable dereferencing

  ```
  the-value =: name
  ```

  Unquoted words are treated as Lingy symbols.

* Lingy Expressions

  Plain (unquoted) scalars are treated as Lingy syntax.
  Scalars starting with `(` are Lingy expressions.

  ```
  answer =: (2 * 3 * 7)
  ```

  You can use a backslash to indicate turn YAML syntax into a Lingy syntax:

  ```
  my-vector =: \[1 2 3]
  ```

  Without the `\` it would be read by YAML as `[ "1 2 3" ]`.

* String interpolation

  YAMLScript strings need to be quoted, since plain (unquoted) strings are seen
  as Lingy symbols (variables) or syntax.

  Lingy symbols or expressions preceded by a `$` are interpolated into double
  quoted and literal style YAML scalars.

  ```
  # Double quoted strings are interpolated
  - greeting =: "Hello, $name!"
  # Multiline literal scalars are interpolated
  - hi-bye =: |
    Hello, $name.
    Goodbye, $name.
  # Single quoted strings are NOT interpolated
  - string =: 'Hello, $name!'
  ```

* Fixity:

  In Lingy (a Lisp) you say things like:
  ```
  (println (* 3 7))
  ```

  YAMLScript lets you say:
  ```
  println(3 * 7)
  ```

  That is function symbols can be placed before the opening paren.
  And prefix math operations can be made infix.

* Function calls
  ```
  say:
    join: [' ', "Hello", "world!"]
  ```

  or

  ```
  say(join(' ' ["Hello" "world!"]))
  ```

* Define functions
  ```
  square-and-add(x,y):
    ((x ^ 2) + y)
  ```

* Define multiple arity functions
  ```
  sum:
    (): 0
    (x): x
    (x, y): (x + y)
    (x, y, z*): (x + (y + (sum z*)))
  ```

* Conditionals
  ```
  if:
  - (x > 50)            # condition
  - say("$x wins :)")   # then
  - say("$x loses :(")  # else
  ```

* Try / Catch
  ```
  - try: (42 / 0)
    catch(e):
      say: "Caught error '$e'"
  ```

* Iteration
  ```
  for (name):
  - [Alice, Bob, Curly]
  - say: Hello, $name!
  ```

* Namespacing and Importing Modules

  ```
  ns My::Package:
    use:
    - Foo::Bar
    - Another::Module: [this, that]
  ```

  YAMLScript modules may be written in YAMLScript, Lingy or Perl.


## Authors

* Ingy döt Net <ingy@ingy.net>


## Copyright and License

Copyright 2022-2023 by Ingy döt Net

This is free software, licensed under:

The MIT (X11) License
