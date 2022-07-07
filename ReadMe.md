YAMLScript
==========

Programming in YAML

## Synopsis

A YAMLScript program `greet.ys` to greet someone 6 times:
```
#!/usr/bin/env yamlscript

# Defined a function called 'main' (entry-point):
main(name):       # main args come from ARGV
- min =: 1        # Assign 1 to variable 'min'
- max =:          # max = 6
    (-): [10, 4]  # 10 - 4 = 6
- for:            # Loop over array, calling a function for each
  - (..): [$min, $max]     # Range operator (1-6)
  - greet: [ $_, $name ]   # Call the local 'greet' function

# Define another function:
greet(num,name):  # Takes 2 arguments
- greeting =: Hello, $name!
- say: $num) $greeting
```

Run:
```
$ yamlscript greet.ys YAMLScript
1) Hello, YAMLScript!
2) Hello, YAMLScript!
3) Hello, YAMLScript!
4) Hello, YAMLScript!
5) Hello, YAMLScript!
6) Hello, YAMLScript!
```

Use the YAMLScript REPL:
```
$ ys    # or 'ys --repl'
ys> {range: [1, 3]}
---
- 1
- 2
- 3
ys> {say: Hello!}
Hello!
--- null
ys> ---
..> say: Goodbye!
..> ...
Goodbye!
--- null
ys> exit  # (ctrl-D)
$
```
See "Using the REPL" below.

## Description

YAMLScript is a programming language that uses YAML as a base syntax.
It feels like a YAML encoded Lisp, but with fewer parentheses.
It takes inspiration from Clojure, Haskell and Perl.

YAMLScript adds various scalar (valid YAML) syntax forms to make coding it
clean and flexible.

For instance you could use any of the following syntax forms to define and
initialize the `x` variable to the number 42:
```
- x =: 42           # Special DSL for the `def` function
- def: [x, 42]      # Literal function call of `def`
- (=): [x, 42]      # Use the operator alias of `def`
- !def [x, 42]      # Use YAML tag instead of simgle pair mapping
- != [x, 42]        # Operator alias tag
- !expr [def,x,42]  # Tagged expr(ession)
- (def x 42)        # Lisp/Clojure expression form (as YAML scalar)
- (x = 42)          # Operator expr using Haskell style
- ((=) x 42)        # Alternate Haskell style #1
- (x `def` 42)      # Alternate Haskell style #2
```

Take your pick.
They are all valid YAML, they all compile to the same AST, they all evaluate to
the same result.

YAMLScript's runtime engine is whatever programming language you are using it
from.
If you are using the `yamlscript` or `ys` CLI binaries, it picks one for you.
The current prototypes are written in Perl, Python and JavaScript.

YAMLScript functions can be defined in the runtime language or in YAMLScript
proper.
YAMLScript has module support and they also can be written in either.

This is a powerful concept because it lets you have clean, multilanguage front
end code, that can do absolutely anything the runtime language is capable of.

A good usage is writing tests.
All of the YAMLScript tests are written in YAMLScript.
The exact same test files are run, regardless of the runtime language they are
testing, or the specific test framework in that language.

## Installation

YAMLScript can be installed in several ways.
Once installed you will have access to the `yamlscript` and `ys` CLI commands.
You will also have library support to invoke YAMLScript directly from Perl,
Python or JavaScript.

* From the source repository
  ```
  $ git clone https://github.com/ingydotnet/yamlscript ~/.yamlscript
  $ source ~/.yamlscript/.rc    # Add this line to your shell rc file
  yamlscript --version
  ```

* From CPAN
  ```
  cpanm YAMLScript
  ```

* From PyPI
  ```
  pip install yamlscript
  ```

* From NPM
  ```
  npm install @yaml/yamlscript
  ```

## YAMLScript Language Capabilities

* Variable binding
  ```
  name =: world
  ```
  Variable names use lowercase letters `a-z`, digits `0-9` and must start with
  a letter.
  Name parts may be separated by a dash `-`.

* Variable dereferencing
  ```
  the-value =: $name
  ```
  The `$` sigil prefix is used to dereference a variable.

* String interpolation

  Variable derefs are expanded in 'plain' (unquoted) YAML strings:
  ```
  - greeting =: Hello, $name!
  # Quoted strings are not interpolated
  - string =: 'Hello, $name!'
  # Unless tagged with '!'
  - greeting =: ! 'Hello, $name!'
  # Multiline strings need the tag
  - hi-bye =: ! |
    Hello, $name.
    Goodbye, $name.
  ```

* Function calls
  ```
  say:
    join: [' ', Hello, world!]
  ```

* Define functions
  ```
  square-and-add(x,y):
  - !expr [[$x, ^, 2], +, y]
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
  - (>): [x, 50         # condition
  - say: $x wins :)    # then
  - say: $x loses :(   # else
  ```

  or:
  ```
  if:
    (>): [x, 50         # condition
  then:
    say: $x wins :)    # then
  else:
    say: $x loses :(   # else
  ```

* Try / Catch
  ```
  - try:
      (/): [42, 0]
    catch(e):
      say: Caught error '$e'
  ```

* Iteration
  ```
  for(name):
  - [Alice, Bob, Curly]
  - say: Hello, $name!
  ```

* Import Modules
  ```
  use:
  # Import 'foo.bar' namespace only:
  - Foo-Bar
  # Import all exported functions from module:
  - Some-Module: [+]
  # Import functions `this` and `that`
  - Another-Module: [this, that]
  # Import all except:
  - That-Module: [-one, -two]
  # Import a namespace using an alternate name
  - Your-String: my.str
  ```
  YAMLScript modules may be written in YAMLScript or in the runtime language.

* Define YAMLScript Modules

  Modules are referred to (`use`d) with the naming style `Foo-Bar`.
  They typically define a namespace `foo.bar`.
  Their file names depend on the language they are written in:
  * YAMLScript — `foo/bar.ys`
  * Perl — `Foo/Bar.pm`
  * Python — `foo/bar.py` or `foo/bar/__init__.py`
  * JavaScript — `foo/bar.js` or `foo/bar/index.js`

  To write a `Foo-Bar` module in YAMLScript:
  ```
  name: foo.bar
  # Define public functions
  f1(): ...     # Fully qualified as foo.bar/f1
  f2(x,y): ...
  f3(+):            # Multi-arity function
    (): ...         # 0 args
    (x): ...        # 1 arg
    (x,y): ...      # 2 args
    (x,y,z*): ...   # 3 or more args
  hide:             # Private/local functions. Not exportable.
    p1(): ...
    p2(): ...
  ```

## Future Plans

* Write an implementation spec for YAMLScript
* Port YAMLScript to many languages
* Write a Test Suite for YAMLScript (in YAMLScript)
* Write a multi-language test framework in YAMLScript
* Generate native programming language code from the YAMLScript AST
  * In multiple target programming languages
* Write the YAML Reference Parser in YAMLScript
* Write YAMLScript in YAMLScript
* Spec the YAMLSchema (YAML Processor Config) language as a dialect of
  YAMLScript

## Authors

* Ingy döt Net <ingy@ingy.net>

## Copyright and License

Copyright 2022 by Ingy döt Net

This is free software, licensed under:

  The MIT (X11) License
