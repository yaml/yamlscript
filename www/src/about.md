---
title: About YS
talk: 0
---

**YS** (aka YAMLScript) is a functional programming language that uses
[YAML](https://yaml.org) as its syntax.
Its primary intent is to provide YAML with the capabilities of a general purpose
programming language for all its users and use cases.
YS does this in a seamless fashion, that feels like a very natural extension of
the YAML you know and use today.

!!! note "All YS files are guaranteed to be valid YAML files"

    Furthermore, all[^1] YAML files (and thus all JSON files) are valid YS, and
    YS will load them as they were intended.

YS enables an extensive amount of programming capabilities that can be used at
any level and to any degree.
The more commonly needed capabilities include:

* Loading data from external sources: files, databases, web, APIs, etc.
* Assigning values to variables, and using them in expressions
* Interpolating variables into strings
* Data path queries, conditionals, loops, transformations, etc
* Defining your own functions and libraries
* Builtin libraries with over 1000 functions for all common tasks
* Using external libraries and dependencies
* Making system calls, and accessing environment variables

Here's file that demonstrates some of the capabilities of YS:
```yaml
!YS-v0:  # Allow functional capabilities in this YAML document

# Define a local function inline:
::
  defn greet(name='World'): |
    Hi, I'm $name,
    Nice to meet you!

vars =: load('vars.yaml')     # Load another YAML file

# A YAML mapping:
key: value                    # A normal YAML key value pair of strings

# Get a random name and anchor it:
name:: &name vars
       .names.rand-nth()

# System call to get the date:
date:: "Today is
        $(sh('date')
        .out:chomp)"
me =: ENV.USER                # Assign USER env var to 'me' var
user:: uc(me)                 # Uppercase me
:when (rand(10) ** 2) > 1::   # Add mapping pair conditionally
  maybe: this pair

# Various ways to call a function:
greeting 1:: greet()
greeting 2:: lc(me):uc1:greet
greeting 3::
  greet: -'<' + *name + '>'
```

> This file goes a bit overboard from what you would typically see in a YAML
> file using YS, but it's just to show off the capabilities listed above.

Use the `ys` command to evaluate the file to YAML or JSON:
```
$ ys -Y file.yaml
key: value
name: Alice
date: Today is Thu Feb 13 11:49:09 AM EST 2025
user: INGY
maybe: this pair
greeting 1: |
  Hi, I'm World,
  Nice to meet you!
greeting 2: |
  Hi, I'm Ingy,
  Nice to meet you!
greeting 3: |
  Hi, I'm <Alice>,
  Nice to meet you!
```


YS was created by one of the original YAML authors, [Ingy döt Net](
ingydotnet.md).
It adheres to the [YAML 1.2 specification](https://yaml.org/spec/1.2.2/) and is
implemented very much as the spec describes.
The YS compiler (YS compiles to the Clojure Lisp for subsequent evaluation) is
implemented as a YAML loader.

[Clojure](https://clojure.org) is a JVM based functional language that known for
its concise, powerful syntax.
YS offers you these powers but without the need for Lisp syntax or a JVM.

YS is compiled to the `ys` standalone native binary executable, and to the
`libys` shared library, for [common operating systems / architectures](
https://github.com/yaml/yamlscript/releases/).

This shared library can be bound to almost all modern  programming languages as
a YAML Loader module.
It currently ships to these 10 languages:

* [Clojure](https://clojars.org/org.yamlscript/clj-yamlscript)
* [Go](https://github.com/yaml/yamlscript-go)
* [Java](https://clojars.org/org.yamlscript/yamlscript)
* [Julia](https://juliahub.com/ui/Packages/General/YAMLScript)
* [NodeJS](https://www.npmjs.com/package/@yaml/yamlscript)
* [Perl](https://metacpan.org/dist/YAMLScript/view/lib/YAMLScript.pod)
* [Python](https://pypi.org/project/yamlscript/)
* [Raku](https://raku.land/zef:ingy/YAMLScript)
* [Ruby](https://rubygems.org/search?query=yamlscript)
* [Rust](https://crates.io/crates/yamlscript)

with many more on the way.


[^1]: There are ways to write YAML that is not valid YS, but they are highly
unlikely to be seen in places where YAML is used as a configuration language.
If a YAML file can be converted to JSON and then back to YAML, without changing
semantics, then it is valid YS.
