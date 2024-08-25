---
title: YAMLScript Syntax
---


The `ys` YAMLScript interpreter command runs YAMLScript programs by compiling
them to Clojure code and evaluating that Clojure code.

To fully understand YAMLScript you need to know:

* [How YAML works](/doc/yaml)
* [How Clojure works](/doc/clojure)

This document will show you the basics of YAMLScript syntax and how they
translate to Clojure code.

> Note: You can play with all the concepts here by putting example code into a
file like `example.ys` and running `ys -c example.ys` which will print the
Clojure code that the YAMLScript code compiles to.


## First Steps

```yaml
!yamlscript/v0
(say "Hello, world!")
```

This is a simple YAMLScript program that prints "Hello, world!".

An interesting point about YAMLScript is that is always valid YAML, and the
YAMLScript compiler (`ys -c`) is really just a fancy YAML loader.
Almost all YAML files are valid YAMLScript and the compiler turns them into the
expected data structure.

This is where the `!yamlscript/v0` tag comes in.
It tells the YS compiler to "load" the YAML into a YAMLScript AST which prints
naturally to Clojure code.

The point is that every YAMLScript program needs to start with the
`!yamlscript/v0` tag, or else it just compiles to a regular data structure.

The second line is a YAMLScript function call that happens to be a Clojure
function call.

Let's play around with that function call syntax a bit.

```yaml
!yamlscript/v0
say("Hello, world!")
```

This compiles to the same Clojure code as the first example.
We moved the `say` function name outside the parentheses.
In YAMLScript this is called a [YeS Expression](/doc/yes).

Note that from a YAML perspective, the entire YAML document is a single scalar
value.

YAMLScript code uses YAML scalars and YAML block mappings for code.
Generally a mix of the two where the top (file level) structure is a nested
mapping and the leaf values are scalar expressions.

Let's change our code to use a mapping instead:

```yaml
!yamlscript/v0
say: "Hello, world!"
```

Again, this compiles to the same Clojure code as the first two examples.

```yaml
!yamlscript/v0
say: 'Hello, world!'
```

This is the same as the previous example, but the string is single-quoted.
Single quotes aren't used for strings in Clojure, but they are in YAML.

In YAMLScript, like Perl and Ruby, double quoted strings
support variable interpolation and character escaping while single quoted
strings do not.

Let's get a bit fancier with our string:

```yaml
!yamlscript/v0
name =: 'world'
say: "Hello, $name!"
```

That's a variable assignment and a string interpolation.

```yaml
!yamlscript/v0
name =: 'world'
say: str('Hello, ', name, '!')
```

Clojure as a `str` function that concatenates strings together.

See the commas between the `str` call arguments?
In Clojure, commas are whitespace and are completely ignored.
This is also true in YAMLScript!

In general Clojure and YAMLScript only use commas in places where the code is
hard to follow without them.

```yaml
!yamlscript/v0
name =: 'world'
say: -'Hello, ' + name + '!'
```

Here we are doing something that you don't see in Clojure.
We're using `+` to concatenate strings.

When YAMLScript operators are infix they compile to polymorphic functions that
work on types of data other than numbers.

But what about the `-` in front of the string?

Without the `-` this would be invalid YAML because YAML does not allow text on
the same line after a quoted string.

The `-` causes YAML to see everything after it as the scalar value:
`-'Hello, ' + name + '!'`.
This is like an escape character for situations where you want to write an
expression but the first character is a syntax character in YAML.
The `-` is removed and the rest of the scalar is compiled as an expression.


## Basic Function Definition

```yaml
defn greet(name):
  say: "Hello, $name!"

greet: 'Bob'
```

In Clojure we'd write this as:

```clojure
(defn greet [name]
  (say "Hello, " name "!"))

(greet "Bob")
```

It's pretty easy to see what's going on here.

Note how we use indentation nesting where Clojure uses parentheses.
That's just the natural way to do things in YAML.
Most of YAMLScript's syntax design is about making code look natural in YAML.
It works out surprisingly well!


## Variable Assignment (def and let)

In the remaining examples we'll assume the `!yamlscript/v0` tag is present.

> What we are calling variable assignment is known as symbol binding in Clojure.
Clojure differentiates between symbols and variables but the distinction is not
so important for YAMLScript.

Assignments are done by using `name =: expression`.

```yaml
foo =: bar() + 17
```

It looks like `=:` is some syntax added to YAML, but it's actually just a plain
scalar value that ends with a space and an equals sign!

These would work just as well:

```yaml
foo   =   :
  bar()
  + 17

? foo
  =
: bar()
  + 17
```

> The second form above uses YAML's rarely seen explicit key syntax.
It can be useful sometimes in YAMLScript when you need spread the key portion of
a key/value mapping pair over multiple lines.
Without it mapping keys are required by YAML to be a single line.
The 'value' side can always be multiline and can start on the next line too.
This is a very common pattern in YAMLScript to make code more readable.

Assignment statements written at the file level compile to `def` forms in
Clojure, while those written inside a function compile to `let` forms.

```yaml
defn f1():
  a =: this()
  b =: that()
  =>: a + b
```

This compiles to:

```clojure
(defn f1 []
  (let [a (this)
        b (that)]
    (+ a b)))
```

Notice how multiple consecutive assignments are compiled to a single `let` form.
This is the preferred Clojure style and YAMLScript tries to compile to idiomatic
Clojure code whenever possible.

What's with the `=>`?
The special token `=>` can be used as a placeholder key for when you want to use
a single expression but being inside a mapping requires you use a key/value
pair.
The `=>` is removed during compilation and the expression is left as the value.


## Destructuring Assignment

Many modern languages have destructuring assignment, where the LHS of an
assignment looks like a data structure instead of a single variable.
This quasi-data-structure is a collection of variables that are assigned values
from the RHS of the assignment.

Clojure and YAMLScript have destructuring assignment support for both sequences
and mappings.

```yaml
-[a b c] =: foo()
-{d :d e :e} =: bar()
```

This would assign the first three values of the sequence returned by `foo` to
`a`, `b`, and `c`, and the values of the `:d` and `:e` keys of the mapping to
`d` and `e`.

This can also be done in function arguments:

```yaml
defn f(a [b c] d):
  =>: (a + (b * c)) / d
f: 2 [3 4] 7  # => 2
```

Here you would call `f` with a single sequence argument and the first three
values of that sequence would be assigned to `a`, `b`, and `c` respectively.

For some reason Clojure does not support destructuring assignment in `def` forms
but YAMLScript makes it work just fine.


### Function Arguments

Like Clojure, all YAMLScript functions must be defined with the number of
arguments they take.
This is know as the function's arity.
Functions can be written to take different specific numbers of arguments, where
each arity has its own definition body.
Functions can also be written to take a variable number of arguments.

Multi-arity functions are called with the same name but the number of arguments
used determines which body is evaluated.

Note that unlike some other languages with multi-arity functions, the type of
the arguments is not used to determine which body to evaluate.

For example to define a function that takes two or more arguments:

```yaml
defn foo(a b *more): ...
```

To define a function that can take 0, 1 or 3+ arguments:

```yaml
defn foo:
  (): ...
  (a): ...
  (a b c *more): ...
```

It would be an error to call the above function with 2 arguments.


## Default Values

Clojure does not support default values for function arguments but YAMLScript
does.

```yaml
defn foo(a b=10 c='horse'): ...
```


## Work in Progress

This document is a work in progress.
More content will be added soon.


## See Also

* [YAMLScript Modes](/doc/modes) - Understanding code mode vs data mode
* [YeS Expressions](/doc/yes)
