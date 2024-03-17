---
title: Wrapped and Ready
date: '2023-12-24'
tags: [blog, advent-2023]
permalink: '{{ page.filePathStem }}/'
author:
  name: Ingy döt Net
  url: /about/#ingydotnet
---

The Elves have everything wrapped up.

Literally!

Tonight's the big night.

It's Time to Deliver!


### Welcome to Day 24 of the YAMLScript Advent Calendar

It's also time to wrap up this year's YAMLScript advent holiday season.
Let's put a bow on it and deliver it to the world.

First we should take a moment to reflect on what we've learned this month.
We can also take a look forward to what's coming up in the New Year.


### The 50,000 Foot View

We'll start with a high level look at the landscape YAMLScript promises to be.

* YAMLScript is new programming language
* You write YAMLScript in YAML
  * JSON is also YAML
  * All JSON and most YAML is valid YAMLScript
* YAMLScript compiles to Clojure
  * Clojure is a Lisp and a Functional Programming (FP) language
  * YAMLScript a functional language with a Lisp model
  * YAMLScript ia a complete language (since Clojure is)
* YAMLScript's engine is a native binary executable
  * No JVM involved; no slow startup
  * YAMLScript starts fast and runs fast
  * YAMLScript programs can compile to binary executables
* YAMLScript has a clean syntax that feels Pythonic
  * YeS-Expressions offer infix operators and prefix function calls
  * Can also be written in a full-Lisp style
  * Or full-non-Lisp or anywhere in between
  * There are many ways to write the same expression in YS
* YAMLScript embeds cleanly into existing YAML files
  * Good for simplifying and enhancing existing YAML files
  * Include parts of other YAML files into any part of a YAML file
  * Map, filter and transform YAML data to more YAML data
* YAMLScript can use libraries written in Clojure or YAMLScript
  * Lots of existing Clojure libraries will work with YAMLScript
  * YAMLScript libraries can be written in YAMLScript
  * YAMLScript can be used as a scripting language
* YAMLScript provides the `ys` command line tool
  * Compile YAMLScript files to Clojure code
  * Compile YAMLScript files to native binary executables
  * Convert between YAML, JSON and EDN
  * Eval YAMLScript expressions from the command line
* YAMLScript provides the `libyamlscript` shared library
  * Bindable to most programming languages
  * Growing number of languages have YS binding modules
  * Load plain old YAML files more correctly
  * Load YAML files with embedded transformation code
  * Run subtasks written in YAMLScript from another language


### The State of the YAMLScript

YAMLScript is a year and a half old from inception, but it really found its
direction in July 2023.

It is now a working programming language with:

* A compiler
* A mature runtime
* A mature standard library
* Binding modules in several programming languages
* Regular binary releases for Linux, Mac (x64 and ARM)
* Simple one-line installer commands
* Example programs on Rosetta Code

Many things remain to be done:

* Windows support
* Finish the v0 compiler
* YAMLScript v0 specification
* Test suite with complete coverage
* User documentation and tutorials
* Binding modules for most modern programming languages
* A YAMLScript module registry

to name a few.


### What Lies Ahead

The complete scope and trajectory of YAMLScript is unknown.
I'm confident that it will be used in ways I can't even imagine.

This notion is built into the design of YAMLScript.
Stating the explicit API version (`v0`) is a mandatory part of the writing and
using YAMLScript code.
Version 1 can completely break away from version 0, and no existing code will
be affected.

I already have exciting plans queued up for the next year:

* IDE support (Calva + source maps)
* The YAMLScript Macro System
* nREPL server support
* Top notch error messages
* Cross language shared library auto-binding
* Use case tutorials and examples
  * Simplifying massive YAML deployment files
  * Using YAMLScript for better CI/CD workflows
  * Task automation with YAMLScript


### The End of an Advent

I hope you've enjoyed this year's YAMLScript Advent Blog.
I enjoyed writing it but I'm relieved that it's finally over.
It's one thing to write a blog post every day and another to be working around
the clock trying to implement everything you're writing about.

Those things I've listed above deserve more explanation.
Perhaps they each deserve their own blog post!
I look forward to telling you about each of them in detail.
But...

I'm going to take the rest of the year off from blogging about YAMLScript.
I'll keep working on the language itself because that's just pure fun for me.
(Usually.)

I'll be back in January and I'll try to put out a blog post every week or two.
From now on I don't have to blog about things that I just barely got working
after burning the midnight oil.

I'm so excited about this language!!!

I knew that doing this advent calendar was going to be a huge challenge, but I
also knew that it was just the thing to drive YAMLScript forward.
The alternative was to wait a full year for Advent 2024.
Thank goodness for my ADD. (Advent Driven Development)

Happy Holidays to you and yours.

I'll see you in the future.

Don't forget your sunglasses.

The future is bright!!!

— Ingy döt Net and the YAMLScript Elves
