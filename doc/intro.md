---
title: Introducing YS
talk: 0
---

YS is a new approach to providing the extra capabilities that YAML users have
been asking for (or adding themselves) for years.
YS embeds cleanly into existing YAML files and adds new capabilities such as:

* Getting data from other YAML files
* Assigning variables
* Referencing other parts of YAML (without anchorsi/aliases)
* Interpolating variables and function calls into strings
* Transforming data structures
* Defining and calling functions
* Using external libraries
* Running shell commands
* And much more...

All of YS is (and must be) valid YAML syntax, even though it might seem
surprising from time to time.
Also all YAML config files are valid YS files and YS will treat them as such (no
code execution), unless you explicitly tell it to do more (by adding a `!YS-v0`
tag to the start).

```yaml
!YS-v0
say: "Welcome to YS!"
```

YS is also a complete, mature, functional, performant programming language.
That's because under the hood, YS code is compiled to [Clojure](
https://clojure.org/) code and evaluated by a Clojure runtime.
For most day to day YS use, you won't need to know anything about Clojure, but
when you need to do something more advanced, [all of Clojure](
https://clojuredocs.org/) is available for you to use.

Even though Clojure is a Lisp, YS code looks a lot more like Python, Ruby, Perl
or JavaScript.
And even though Clojure is a JVM (Java) language, YS doesn't use the JVM at all.
YS is a fast standalone native binary, as is the `libyamlscript` shared library
that it is used by all [YS loader libraries](loaders.md).
