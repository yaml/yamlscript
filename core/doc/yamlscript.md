YAMLScript
==========

Program in YAML — Code is Data


## Synopsis

A YAMLScript program `99-bottles.ys`:

```
#!/usr/bin/env yamlscript

defn main(number=99):
  map(say):
    map(paragraph):
      (number .. 1)

defn paragraph(num): |
  $(bottles num) of beer on the wall,
  $(bottles num) of beer.
  Take one down, pass it around.
  $(bottles (num - 1)) of beer on the wall.

defn bottles(n):
  ???:
    (n == 0) : "No more bottles"
    (n == 1) : "1 bottle"
    :else    : "$n bottles"
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

user=> nums =: (1 .. 3)
user/nums
user=> nums
(1 2 3)
user=> map(inc nums)
(2 3 4)
user=> <CTL-D>         # to exit
$
```


## Status

This is ALPHA software.
Expect things to change.


## Description

**YAMLScript** is a programming language that uses YAML as a base syntax. It
feels like a *YAML encoded Lisp*, but with fewer parentheses.

In fact, YAMLScript *is* a Lisp.
It's a YAML-based specialized syntax reader for the **Clojure** programming
language.
Clojure is a Lisp hosted by the Java JVM and also by JavaScript where it's
called **ClojureScript**.

YAMLScript (in its current early stages) is only available in Perl where it
targets a *Clojure Platform for Perl* called **Lingy**.

[Lingy](https://metacpan.org/pod/Lingy) intends to be ported to many other host
programming languages.
YAMLScript intends to work anywhere there is a Clojure Platform available
(including Lingy, Clojure and ClojureScript).

For the remainder of this document when we say **Clojure** it means any
**Clojure Platform** (including **Lingy**).


## Installation

YAMLScript is currently only available as a Perl CPAN module.
You install it like so:

```
$ cpanm YAMLScript
```


## Command Line Usage

Once installed you will have access to the `yamlscript` CLI command.

* Run a YAMLScript program (with arguments):

  ```
  $ yamlscript my-prog.ys foo 42
  ```

* Start the YAMLScript REPL:

  ```
  $ yamlscript
  ```

* Run a YAMLScript one line evaluation:

  ```
  $ yamlscript -e 'println: "Hello"'
  ```


## REPL Usage

YAMLScript has REPL that is a modified version of the Lingy REPL.

The main difference is that YAML and thus YAMLScript are line oriented.
It is likely you'll want to enter multiple lines to complete one expression.

Here is an example of how to do that:

```
user=> ---
  #_=> say:
  #_=> - "Hello"
  #_=> - "world"
  #_=> ...
Hello world
nil
user=>
```

You enter multi-line mode with the line `---` and end it with `...`.
After you end it, the entire input will be evaluated.

The REPL considers the entire entry to be a single value.
In other words, when you press up-arrow, the entire multi-line value will come
up for editing and re-evaluation.


## YAMLScript Standard Library

YAMLScript has it's own `ys.std` library with a small set of functions that
differ from `clojure.core`.
This library is automatically required by the YAMLScript Runtime.

The YAMLScript Standard Functions are:

* `read-file-ys`

  Reads and evaluates a YAMLScript `.ys` file.

* `read-string-ys`

  Reads and evaluates a YAMLScript code string.

* `say`

  An alias for `println`.

* `yamlscript-version`

  Returns the YAMLScript version mapping.

* `..`

  The YAMLScript infix range operator.
  Described below.


## YAMLScript Documentation

Since the YAMLScript programming language is just a different syntax for the
Clojure programming language, you should read the Clojure documentation to see
what it can do.

And since Lingy is just a port of the Clojure programming language, you can
read the Lingy documentation to see how Lingy currently works or differs from
Clojure.

To be good at programming in YAMLScript you need to be fairly well versed in 3
things:

1. **YAML**

  Many people see YAML as obvious and trivial, but YAMLScript takes advantage
  of some aspects of YAML that you might not be aware of.
  We'll cover many of those things in the following sections.

2. **Clojure**

  Since YAMLScript is really just a friendly Clojure syntax, you need to know
  what you're really saying and how to do things in Clojure.
  We'll cover Clojure basics and things to know about it.

3. **YAMLScript to Clojure Transformation**

  The key to learning YAMLScript is knowing how to best say the things you
  want, using the YAMLScript syntax forms.
  The main part of this documentation is describing each syntax feature.


## YAML Basics

Again, YAMLScript programs are encoded in YAML.
It follows that every valid YAMLScript program must also be valid YAML.

The YAML data model consists of a graph composed by using 3 kinds of nodes:

* Mappings (hashes, dictionaries, associative arrays)
* Sequences (arrays, lists)
* Scalars (single atomic values)

Most YAML documents in the wild are top level mapping or sequence nodes, but a
YAML document can also be a top level scalar (just a big string).

Consider this (valid) YAML document (also a YAMLScript program):

```
; A YAMLScript Program

(def name "YAMLScript")
(println (str "Hello, " name))
```

That doesn't look like a valid YAML file (no colons or dashes) but it is.
It's just a single string:

```
"; A YAMLScript Program\n(def name \"YAMLScript\") (println (str \"Hello, \" name))"
```

Notice how the lines got joined together. 2 consecutive newlines got turned
into 1 newline, and 1 newline got turned into a space.
If there was no blank line after the first (comment) line, then the entire
program would be read as a single comment line.

Writing YAMLScript programs as top level scalars (though possible) is not a
great idea, but understanding how YAML scalars work in YAMLScript is very
important.

In YAMLScript, at any structural level, expressions can be written either as a
YAML data structure or just a Clojure s-expression written as a YAML plain
(unquoted) scalar.

Let's write the above YAMLScript program in a different, more idiomatic style:

```
## A YAMLScript Program
name =: 'YAMLScript'
println: "Hello, $name"
```

This probably looks more like a YAML file to you.
There's a few interesting things to notice.

* We changed the `;` comment to a YAML `#` comment.
  No blank line is needed after it.
* What is the `=:` token about?
  It's not a token at all.
  The `=` is just the last character of the key string `name =`.
  But that is what signals YAMLScript to generate a `def` expression.
* The scalar `'YAMLScript'` is quoted even though it doesn't need to be in
  YAML.
  In YAMLScript all scalars that are meant to be Clojure strings are quoted.
  If `'YAMLScript'` were unquoted it would be recognized as a Clojure symbol.
* YAMLScript supports string interpolation in double quoted strings.
  See the `$name` variable in `"Hello, $name"`.
* The program is written as one mapping but it represents 2 Clojure statements.
  Also key/pair order must be honored here, obviously.
  Mappings loaded into the YAML data model don't guarantee key order, but
  YAMLScript mappings do.
  This will be explained more below in "YAMLScript Implementation Details".

Here's the same program written differently:

```
- comment: 'A YAMLScript Program'
- name =: 'YAMLScript'
- println: str("Hello, ", name)
```

Notes:

* Instead of a top level mapping, this is written as a top level sequence (of
  single pair mappings).
* The comment was written as a function.
  The `comment` function is a core Clojure function and often used to comment
  out sections of code.
* The Clojure expression `(str "Hello, " name)` was written as
  `str("Hello, ", name)`.
  This is a ysexpr ("Yes" Expression) which is documented below.
  In Clojure a comma is whitespace, so `str("Hello, " name)` would also work.

There are many ways to write almost any expression in YAMLScript.
YAMLScript tries to offer lots of syntax variants and alternatives to help make
your code read and feel more natural, while still being valid YAML (as is
required).

One thing you might run into is when you need to use a scalar or a sequence
line when you are working in a mapping structure.

Consider this invalid YAML:

```
x =: 5                              # Start a mapping
(println "$x + $x = $(x + x)")      # Err, a scalar line
- println: "$x * $x = $(x * x)"     # Err, a sequence line
```

You can easily fix this by using the Clojure `do` function which evaluates a
list of expressions in order:

```
x =: 5                              # Start a mapping
do:                                 # `do` is a mapping key here
- (println "$x + $x = $(x + x)")    # OK, a sequence line
- println: "$x * $x = $(x * x)"     # OK, another sequence line
```

Another thing you'll likely run into is places where Clojure syntax collides
with YAML syntax.

```
- (def vec1 [5 7 9])                # Define a variable bound to a vector
- vec2: [5 7 9]                     # Bad. YAML sees it as `["5 7 9"]`
- vec3: .[5 7 9]                    # Good. The period makes value a YAML scalar
```

Use a period at the start of a value so that YAML will consider the value to be
a scalar, thus interpreted as a Clojure expression.

What if you want to use YAML to define an actual data structure in your
YAMLScript program?

You can use the YAML tag `!` to indicate that the particular data structure is
just YAML data.

```
array =: ! [ one, two, three ]
dict =: !
  name: Pat
  age: 42
  colors:
  - blue
  - green
```


## Clojure Basics

Syntactically Clojure is a Lisp.
This means that all code expressions are written as lists in parentheses.

```
(println (str "2 + 2 = " (+ 2 2)))
```

In each of the nested expression lists here, the function name comes first
(`println`, `str`, `+`) followed by its arguments.
When an expression is evaluated, its arguments are evaluated first.

Note: Some Clojure expressions are "Macros" or "Special Forms" rather than
functions, and evaluation happens differently.
That's a more advanced Clojure topic and not covered here.
But macros and special forms look the same as function call expressions and you
can mostly think of them as the same thing.

Mostly Clojure code is not affected by whitespace; a program can possibly be
joined together onto a single line and still work.
An exception is comments which start with a semicolon and consume the rest of
the line.

```
; A full line comment

(say "ok")  ; A comment after code
```

A logical unit in Clojure is called a "form".
Clojure forms include:

* Lists - Parenthesized sets
* Tokens - Single values
* Structures - Vectors (arrays) and HashMaps (hashes / dictionaries)

Here's a list of the common Clojure tokens:

* Symbol

  A word like `foo` that acts like a variable and is bound to other values.
  Symbol words can contain many non-word characters like `-`, `.`, `/`, `:` and
  `?`.
  For instance `user/is-boolean?` is a valid Clojure symbol.
  Also math operators like `+` and `-` are symbols.
  YAMLScript and Lingy are more strict about symbols and use a subset of the
  combinations that are valid in Clojure.
  Essentially all the symbols you'll see in real world Clojure are allowed in
  YAMLScript and Lingy.

* Number

  Things like `123` and `-1.23`.

* String

  Like `"foo"`. Always double quoted.

* Character

  A character is expressed by using a backslash followed by the character, like
  `\a \b \c`.

  A few special characters like tab and newline have the forms `\tab` and
  `\newline`.

  Strings are sequences of characters (using the `yamlscript` REPL):

  ```
  user=> seq: "Hello\n"
  (\H \e \l \l \o \newline)
  ```

* Keyword

  A keyword starts with a colon like `:foo` and is commonly used for HashMap
  keys.

* Regular Expression (Regex)

  A Clojure regex is written like `#"^foo.*bar"`; a string preceded by a hash
  mark.
  YAMLScript lets you write them like `/^foo.*bar/` as described later.


Common Clojure data structures are:

* Vector

  An array using square brackets: `[1 foo :whee]`.
  Commas (whitespace) can be used for clarity: `[1, foo, :whee]`.

* HashMap

  A hashmap is a set of pairs in curly braces: `{:foo 1, :bar x}`.

* HashSet

  A hashset is like a hashmap with only the keys: `#{:foo :bar}`.

* Quoted list

  A single quote before a list causes is to not be evaluated, and thus can be
  used like a vector as a collection of things: `'(1 foo :whee)`.

As was just said single quote is used (in all Lisps) to cause whatever follows
to not be immediately evaluated.

```
user=> (def x (+ 1 2)) x
user/x
3
user=> (def x '(+ 1 2))
user/x
user=> x
(+ 1 2)
user=> 'x
x
```

We won't go deeper into Clojure here but it has lots of great documentation
online.

See:

* https://clojure.org/api/cheatsheet
* https://clojuredocs.org/
* https://clojure-doc.org/
* https://www.braveclojure.com/



## YAMLScript to Clojure Transformations

The final piece to understanding how to program in YAMLScript is the learning
how various YAML things are transformed into Clojure code.

This section covers all the various transforms.
Each item below shows a code snippet containing a YAMLScript form followed by
its transformation to Clojure code.

* Tokens

```
list:
- 'string'
- "another\nstring"
- /^a.*regex$/
- :keyword
- symbol
- 1337
- 3.1415

(list "string" "another\nstring" #"a.*regex$" :keyword symbol 1337 3.1415)
```

* Variable Binding

  ```
  name =: 'world'

  (def name "world")
  ```

  Variable names use lowercase letters `a-z`, digits `0-9` and must start with
  a letter.
  Name parts may be separated by a dash `-`.

* Variable Dereferencing

  ```
  the-value =: name

  (def the-value name)
  ```

  Unquoted words are treated as Clojure symbols.

* Clojure Expressions

  Plain (unquoted) scalars are treated as Clojure syntax.
  Scalars starting with `(` are Clojure expressions.

  ```
  say: (+ (* 2 3) 4)

  (say (+ (* 2 3) 4))
  ```

* Yes Expressions

  YAMLScript allows you to write many Clojure expressions in forms more
  familiar in common non-Lisp languages.

  ```
  say: abs(inc(41) * 9)

  (say (abs (* (inc 41) 9)))
  ```

  This includes function symbol before opening paren, and infix math operators.

  "Yes Expressions" are descibed more completely in their own section below.

* String interpolation

  YAMLScript strings need to be quoted, since plain (unquoted) strings are seen
  as Clojure symbols (variables) or syntax.

  Clojure symbols or expressions preceded by a `$` are interpolated inside double
  quoted and literal style YAML scalars.

  ```
  # Double quoted strings are interpolated
  - say: "Hello, $name!"
  # Multiline literal scalars are interpolated
  - say: |
      Hello, $name.
      Goodbye, $name.
  # Single quoted strings are NOT interpolated
  - say: 'Hello, $name!'

  (say (str "Hello, " name "!"))
  (say (str "Hello, " name ".\nGoodbye, " name ".\n"))
  (say "Hello, $name!")
  ```

* Function Calls

  ```
  say:
    join: [' ', "Hello", "world!"]

  (say (join " " ["Hello" "world!"]))
  ```

  A YAML mapping pair with a symbol for a key (unquoted word) and a sequence of
  arguments.
  If a single argument is used then it doesn't need to be in a sequence.

  Below are 3 different ways to call a function with no arguments.

  ```
  - foo: []
  - bar()
  - baz():

  (foo) (bar) (baz)
  ```

* YAMLScript has many styles to write the semantically equivalent function
  calls.

  These all do the same thing:

  ```
  - (say "Hello world!")
  - say("Hello world!")
  - say("Hello world!"):
  - say:
    - "Hello world!"
  - say: ["Hello world!"]
  - say: "Hello world!"
  - say("Hello"): "world!"
  - say: ["Hello", "world!"]
  - say: ."Hello", "world!"
  ```

* Function Definition

  ```
  defn double-and-add(x, y): ((x * 2) + y)

  (defn double-and-add [x y] (+ (* x 2) y))
  ```

* Define multiple arity functions

  ```
  defn sum:
    (): 0
    (x): x
    (x, y): (x + y)
    (x, y, *z): (x + (y + apply(sum z)))

  (defn sum
    ([] 0)
    ([x] x)
    ([x y] (+ x y))
    ([x y & z] (+ x y (apply sum z))))
  ```

* Conditional Forms

  `if` expressions:

  ```
  if (x > 50):          # condition
  - say("$x wins :)")   # then
  - say("$x loses :(")  # else

  (if (> x 50)
    (say (str x " wins :)"))
    (say (str x " loses :(")))
  ```

  `when` and `when-not` expressions:

  ```
  - (x > 50) ?: say("big")
  - (x > 50) |: say("small")

  (when (> x 50) (say "big"))
  (when-not (> x 50) (say "small"))
  ```

  `cond` expressions:

  ```
  ???:
    (x > 50) : "big"
    (x < 50) : "small"
    :else    : "just right"

  (cond
    (> x 50) "big"
    (< x 50) "small"
    :else    "just right")
  ```

* Try / Catch

  ```
  - try: (42 / 0)
    catch(Exception e):
      say: "Caught error '$e'"

  (try (/ 42 0)
    (catch Exception e
      (say (str "Caught error '" e "'"))))
  ```

* Iteration

  ```
  for (name):
  - ! [Alice, Bob, Curly]
  - say: "Hello, $name!"

  (for [name ["Alice", "Bob", "Curly"]]
    (say (str "Hello, " name "!")))
  ```

* Looping

  ```
  loop [x 1]:
    say: x
    if (x < 5):
      ^^^: (x + 1)

  (loop [x 1]
    (say x)
    (if (< x 5)
      (recur (+ x 1))))
  ```

* Namespacing and Importing Modules

  ```
  ns My::Package:
    use:
    - Some::Module
    - Another::Module: [this, that]
    require: A::Module
    import: A::Class

  (ns My.Package
    (:use
      [Some.Module]
      [Another.Module this that])
    (:require [A.Module])
    (:import [A.Class]))
  ```

  Perl YAMLScript modules may be written in YAMLScript, Lingy or Perl.

* Method Invocation

  ```
  obj =: Foo::Bar->new()
  say: obj->method(42)

  (def obj (.new Foo.Bar))
  (say (. obj (method 42))
  ```

### ysexprs - "Yes Expressions"

Coming from non-Lisp programming languages, Lisp "sexpr" (S Expression) syntax
can feel awkward with the function name going inside the parens and the math
operators coming first.

YAMLScript has an optional "ysexpr" (Yes Expression) form for many common Lisp
patterns that may feel more natural to you.

They are just a set of simple transformations that we'll describe here.

* Function calls

  ```
  foo(123 "xyz")

  (foo 123 "xyz")
  ```

  The function word can come before the opening paren instead of after it.
  It's a simple switcheroo.
  Note: there can be no space between the function name and the `(`.

* Nested calls

  ```
  foo(123, bar(456), baz())

  (foo 123, (bar 456), (baz))
  ```

  Just as you would expect.
  Commas were added for readability in this example; but they are just
  whitespace characters as we said above.

* Infix Operators

  ```
  (a + b)

  (+ a b)
  ```

  If the second token in a 3 element list is an operator, then it gets swapped
  with the first.

  This doesn't work for longer expressions:

  ```
  (a + b * c)       # Err

  (a + (b * c))     # OK

  (+ a (* b c))     # Clojure
  ```

* Infix Range Operator

  There is a special operator `..` that only works infix.
  It auto-detects descending ranges and includes the terminating number in the
  range.

  ```
  r =: (1 .. 10)
  s =: (10 .. 1)

  (def r (range 1 11))
  (def s (range 10 0 -1))
  ```

* Keep Prefix

  In the very rare case you actually want the operator to be second you can:

  ```
  (, a + b)

  (a + b)
  ```

* Method Calling

  In clojure you can call a host object method:

  ```
  (.method object (1 2))
  ; or (the above expands to this)
  (. object (method 1 2))
  ```

  They both are hard for non-Lisp programmers to read.

  YAMLScript provides:

  ```
  object->method(1, 2)
  ```

Note that in any situation you are free to use either a regular Clojure sexpr
or a YAMLScript ysexpr and you can even use both in nested expressions.


## YAMLScript Implementation Details

Most people use YAML to `load` YAML files or strings into native data
structures.
The code to do this is something simple like:

```
data = yaml.load-file("foo.yaml")
```

But the YAMLScript load process is far from simple.
It goes something like this:

* Read YAMLScript text from a file
* Parse YAMLScript text into a stream of events
* Compose the events into a graph
* Assign a tag to every node in the graph (Tag Resolution)
* Build a YAMLScript AST (Abstract Syntax Tree) from the graph
* Transform / optimize the AST
* Construct a native data structure by applying the functions associated with
  each tag to the node, in a depth first order
* Send the AST to Clojure code
* Evaluate the Clojure code
* Return the resulting value

Although it may seem like YAMLScript is loading a program into memory and then
applying various tricks to make it do what it wants, that's not really what's
happening.

The Perl module YAMLScript::Reader is actually a special YAML Loader module
that follows all the steps above.
It's the tag resolution part that is vastly different than the typical rules
used by a generic YAML loader.

YAMLScript::Reader uses the Perl module YAML::PP to turn YAML into an event
stream, but then it takes control from that point.

First it composes the YAML into a graph which is very simple.

Here it is important to note that since we don't intend to load the YAMLScript
as a native language data structure, we can make use of YAML properties that
ordinary YAML loaders are not supposed to.

These include:

* Mapping Key Order

  The YAML parser reports all the info (parse events) it creates in the same
  order as it was parsed from the YAML source, including mapping keys.
  Since YAMLScript is not trying to turn this info into a normal mapping, it is
  ok for the reader to make use of and preserve this order in the Clojure AST
  it is making.

* Mapping Key Duplication

  In the same regard, YAMLScript doesn't care if you use the same key, as long
  as it makes sense to YAMLScript.

  ```
  # Execute in order:
  say: "one"
  say: "two"
  ```

* Scalar Quoting Style

  YAML has 5 syntax forms to represent scalars:

  1. Plain (unquoted)
  2. Single Quoted
  3. Double Quoted
  4. Literal (like a heredoc)
  5. Folded

  A typical YAML loader only considers (for tag resolution) whether a scalar
  was plain or not-plain.
  In other words it should never treat a scalar differently if it used
  single-quoted as opposed to double-quoted.
  The default rule is that all non-plain scalars are loaded as strings, where
  plain scalars might load as numbers, dates, booleans etc.

  YAMLScript on the other hand treats scalars differently depending on quoting
  style (and several other things).

After YAMLScript creates the compostion graph, it analyzes each node in the
graph and assigns it a unique YAMLScript YAML tag (tag resolution).

```
name =: 'YAMLScript'
println: "Hello, $name"
```

becomes something like this fully tagged YAML structure:

```
--- !program
!def  "name"    : !str  "YAMLScript"
!call "println" : !vstr "Hello, $name"
```

The construction phase of turning this into a Lingy AST is just applying the
functions associated with these tags.

The result is a data structure of the same form that Lingy::Reader would
produce form Clojure code.
It is fed directly into the Lingy evaluation loop.


## YAMLScript Programs

The YAMLScript source repository contains [example YAMLScript programs](
https://github.com/yaml/yamlscript/tree/main/perl/eg).

These programs are also available on RosettaCode.org [here](
https://rosettacode.org/wiki/Category:YAMLScript).


## Test::More::YAMLScript

YAMLScript (like Clojure) is designed to both use the host language and be used
by the host language.

A great example is the CPAN module [Test::More::YAMLScript](
https://metacpan.org/pod/Test::More::YAMLScript).
This module lets Perl programmers write their unit tests in YAMLScript.

The [module itself](
https://metacpan.org/dist/Test-More-YAMLScript/source/lib/Test/More/YAMLScript.ys)
is also written in YAMLScript!

And of course, its tests are written in YAMLScript.


## YAMLTest

[YAMLTest](https://metacpan.org/pod/YAMLTest) is another CPAN module that
extends the basic functionality of Test::More::YAMLScript.


## See Also

* [YAML](https://yaml.org)
* [Clojure](https://clojure.org)
* [Lingy](https://metacpan.org/pod/Lingy)
* [Test::More::YAMLScript](https://metacpan.org/pod/Test::More::YAMLScript)
* [YAMLTest](https://metacpan.org/pod/YAMLTest)


## Authors

* [Ingy döt Net](https://github.com/ingydotnet) - Creator / Lead
* [Ven de Thiel](https://github.com/vendethiel) - Language design


## Copyright and License

Copyright 2022-2024 by Ingy döt Net

This is free software, licensed under:

The MIT (X11) License
