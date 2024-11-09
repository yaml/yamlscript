---
title: The YAMLScript Standard Library
---

The YAMLScript standard library is a collection of functions that are available
to all YAMLScript programs.
This document describes the functions in the standard library and how to use
them.

YAMLScript exposes most of the functions available in the [Clojure Core](
https://clojuredocs.org/core-library) standard library.
See [Core Library Essentials](/doc/core) for an overview of those functions.

The YAMLScript standard library replaces some Clojure functions with a version
more suited to YAMLScript.
In those cases, the original Clojure function is still available in the
[`ys::clj`](/doc/ys-clj) namespace.


## Special functions

* source(*) - Run a YAMLScript file as a Bash script


## Shorter named alias functions

* `a` - `clojure.core/identity` alias
* `len` - `clojure.core/count` alias


## Quoting functions

* `q(form)` - `clojure.core/quote` alias
* `qr(str)` - `clojure.core/re-pattern` alias
* `qw(symbols)` - Turn symbols into a vector of strings


## Alternate truth functions

* `falsey?(x)` - True if x is falsey - 0, nil, false, empty
* `truey?(x)` - True if x is not falsey
* `or?(x *xs)` - Return first truey value or nil
* `and?(x *xs)` - Return last truey value or nil
* `|||` - `or?` operator
* `&&&` - `and?` operator


## Named function aliases for infix operators

* `eq` - `clojure.core/=`
* `ne` - `clojure.core/not=`
* `gt` - `clojure.core/>`
* `ge` - `clojure.core/>=`
* `lt` - `clojure.core/<`
* `le` - `clojure.core/<=`


## Common type conversion functions

* `to-bool(x)` - Convert x to a boolean
* `to-booly(x)` - Convert truey x to a boolean
* `to-float(x)` - Convert x to a float
* `to-int(x)` - Convert x to an integer
* `to-list(x)` - Convert x to a list
* `to-map(x)` - Convert x to a map
* `to-num(x)` - Convert x to a number
* `to-set(x)` - Convert x to a set
* `to-vec(x)` - Convert x to a vector


## Math functions

* `add(*)` - `clojure.core/+` alias
* `sub(*)` - `clojure.core/-` alias
* `mul(*)` - `clojure.core/*` alias
* `div(*)` - Division function that returns a float if needed
* `sum(nums)` - Sum a sequence of numbers
* `pow(x *ys)` - Raise a number to a power - right associative
* `sqr(x)` - Square a number
* `cube(x)` - Cube a number
* `sqrt(x)` - Square root of a number
* `abs(x)` - Absolute value of a number
* `add+(x *xs)` - Polymorphic addition function
* `sub+(x *xs)` - Polymorphic subtraction function
* `div+(x *xs)` - Polymorphic division function
* `mul+(x *xs)` - Polymorphic multiplication function


## Document result stashing functions

* `$` - Mapping of document results
* `$#` - Index of last document result
* `$$` - Get the last document result


## Control functions

* `value(x)` - Get var value from var, symbol or string.
  Often used to convert a string to a function.
* `call(f *args)` - Call a function or value.
  Function `f` can be a string, symbol or function.
* `die(msg)` - Idiomatic error function
* `each(bindings *body)` - Non-lazy for loop
* `err(*xs)` - Print to stderr
* `exit(rc=0)` - Exit the program
* `if(cond then else)` - Functional `if` used in dot chaining
* `sleep(secs)` - Sleep for a number of seconds


## String functions

* `chomp(s)` - Remove trailing newlines
* `index(s, sub)` - Find index of substring
* `join(s, *xs)` - Join strings or seqs with a separator
* `lc(s)` - Lowercase a string
* `lines(s)` - Split a string into lines
* `pretty(x)` - Pretty print a value
* `replace(s, old, new)` - Replace all occurrences of old with new
* `replace1(s, old, new)` - Replace first occurrence of old with new
* `split(s, re)` - Split a string by a regex
* `trim(s)` - Trim whitespace from both ends
* `triml(s)` - Trim whitespace from left end
* `trimr(s)` - Trim whitespace from right end
* `uc(s)` - Uppercase a string
* `uc1(s)` - Capitalize a string
* `words(s)` - Split a string into words


## Regex functions

* `=~` - Infix re-find operator
* `!~` - Infix re-find complement operator


# Collection functions

* `get+(C, K)` - Get a string, keyword or symbol from a map or sequence
* `grep(P, C)` - Filter a collection by a predicate
* `has?(C, x)` - True if collection has x
* `in?(x, C)` - True if x is in collection
* `omap(*xs)` - Create an ordered map
* `reverse(x)` - Reverse a string, vector or sequence
* `rng(x, y)` - Create a range of numbers or characters, y is inclusive
* `..` - Infix rng operator


## I/O functions

* `out(*xs)` - Print to stdout
* `pp(x)` - Pretty print a value
* `say(*xs)` - Print to stdout with newline
* `warn(*xs)` - Print to stderr with newline


## File system functions

* `fs-d(path)` - True if path is a directory
* `fs-e(path)` - True if path exists
* `fs-f(path)` - True if path is a regular file
* `fs-l(path)` - True if path is a symbolic link
* `fs-r(path)` - True if path is readable
* `fs-s(path)` - True if path is not empty
* `fs-w(path)` - True if path is writable
* `fs-x(path)` - True if path is executable
* `fs-z(path)` - True if path is empty
* `fs-abs(path)` - Get the absolute path
* `fs-abs?(path)` - True if path is absolute
* `fs-dirname(path)` - Get the directory name of a path
* `fs-filename(path)` - Get the file name of a path
* `fs-glob(path)` - Glob a path
* `fs-ls(dir)` - List a directory
* `fs-mtime(file)` - Get the modification time of a file
* `fs-rel(path)` - Get the relative path
* `fs-rel?(path)` - True if path is relative
* `fs-which(name)` - Find the path of an executable


## IPC functions

See [`babashka.process`](https://github.com/babashka/process#readme) for more
information about these functions.

* `exec(cmd, *xs)` - Execute a command
* `process(cmd, *xs)` - Execute a command
* `sh(cmd, *xs)` - Execute a command
* `shell(cmd, *xs)` - Execute a command
* `shout(cmd, *xs)` - Execute a command and return the output


## External library functions

* `use-pod(pod-name, version)` - Load an external library pod


## HTTP functions

* `curl(url)` - Get a URL and return the body
