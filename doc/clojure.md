---
title: Clojure Basics
---

YS as a technology has many goals.
YS as a programming language is essentially a different syntax for
Clojure.
However, YS is certainly not an attempt to replace Clojure.

In theory YS could have been written in any language.
But in reality, Clojure was the best choice for many reasons including:

1. Clojure is a Lisp and Lisps are "code as data". Since YS is YAML and YAML is
   data, Clojure is a natural fit.
2. GraalVM's native-image compiler and Clojure's SCI runtime make it possible to
   to use YS without Java or the JVM.
3. Clojure's core libraries are extensive, robust and well-documented.

Since YS code always translates to Clojure code, it's important to have a good
understanding of Clojure to write good YS code.

Again, Clojure is a Lisp dialect.
Lisps work entirely with parenthesized expressions containing a function
followed by its arguments (S-Expressions).

For example:

```clojure
(+ 1 2 3)  ; `+` is a function that adds its arguments
(str "Hello " name "!")  ; `str` is a function that concatenates its arguments
(println (str "The answer is " (+ 2 3 7) "!"))  ; Multiple nested expressions
```

The basic clojure syntactic forms are:

* Lists - `(a b c)`
* Vectors - `[a b c]`
* Maps - `{a b, c d}`
* Symbols - `a`, `b`, `c`
* Quoted forms - `'(...)`, `'[...]`, `'{...}`, `'abc`
* Strings - `"abc"`
* Numbers - `123`, `3.14`
* Characters - `\a`, `\b`, `\c`
* Keywords - `:a`, `:b`, `:c`
* Anonymous functions - `#(+ %1 %2)`
* Sets - `#{a b c}`
* Regex - `#"abc"`
* Comments - `;` to end of line
* Commented out forms - `#_(+ a b c)`

See [Introduction to Clojure](
https://clojure-doc.org/articles/tutorials/introduction/) for more about
Clojure.
