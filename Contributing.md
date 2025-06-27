Contributing to YAMLScript
==========================

Welcome to the YAMLScript project.
We're glad you're here!

YAMLScript is a project that involves every programming language that can make
use of YAML.
We need lots of people with varying backgrounds to make this project a success.

This document is a set of guidelines for contributing to the YAMLScript project.
The content will mostly be technical, but there are some social guidelines as
well.

Expect this document to grow and evolve over time.


## Our Code of Conduct

Please read the [YAMLScript project code of conduct](
https://github.com/yaml/yamlscript/#coc-ov-file) document.

The YAMLScript project is about making something valuable to a wide audience
of communities.

Bring your best ideas and expect others to do the same.

Be excellent to each other.


## YAMLScript Project Overview

YAMLScript is a programming language that uses YAML as its syntax and compiles
to Clojure code which is in turn interpreted by the YAMLScript runtime or
compiled to native binary executables and shared libraries.

It can be used to write applications, libraries, and other programs.
It can also be used to enhance existing YAML files with functional capabilities.

The YAMLScript runtime is based on [SCI](
https://github.com/borkdude/sci), and compiled to a native binary executables
and shared libraries using [GraalVM](https://www.graalvm.org/)'s `native-image`.

The project provides FFI binding modules/packages/libraries for all the
programming languages that it can.
These bindings act like normal YAML loader modules, but recognize the YAMLScript
parts and evaluate them as part of the loading process.

The YAMLScript compiler and runtime are written in Clojure.
This is both necessary and awesome.
Clojure compiles to Java jar files, and native-image compiles Java jar files to
native binary executables and shared libraries.

Clojure is a great language for writing compilers and interpreters.
It has a great ecosystem of libraries and tools.
It also has a fabulous community of people who are willing to help others.

> NOTE: Since YAMLScript compiles to Clojure, eventually parts of YAMLScript
> will likely be written in YAMLScript.

All the code, documentation, website and other resources are hosted in this
[single GitHub repository](https://github.com/yaml/yamlscript).


## The YAMLScript Community

YAMLScript is a new language but it already has a budding community.
We currently hang out in 2 places:

* [The YAMLScript Matrix Chat Room](
https://matrix.to/#/#chat-yamlscript:yaml.io)
* [The #yamlscript Channel in the Clojurians Slack Workspace](
https://clojurians.slack.com/archives/C05HQFMTURF)

We're a friendly bunch, so feel free to drop by and say hello! :-)

If you want to start a public discussion about YAMLScript, please use the
[GitHub Discussions](https://github.com/yamls/yamlscript/discussions) feature.


## Repository Structure

Each language binding has a directory named after the language.
For example, the Python binding is in the `python` directory.

Here's what's in the non-binding directories:

* `common/`

  Files that are used by multiple project parts.
  At the moment this is mostly Makefile and Dockerfile related.

* `core/`

  This is where the YAMLScript compiler and runtime code live.
  It is the primary directory involved in creating the language.

* `libyamlscript/`

  This directory is responsible for building the `libyamlscript.so` shared
  library.
  The shared library is what all the language binding modules bind to.

* `sample/`

  This directory contains various example YAMLScript programs.
  They are organized into subdirectories.
  The example programs get used in YAMLScript documentation and blog posts.
  Some are also cross posted to [Rosetta Code](
  https://rosettacode.org/wiki/Category:YAMLScript).

* `test/`

  This directory is meant to be used for common test code.
  Eventually all the bindings should pass the same tests.

* `util/`

  A directory for project maintenance bin scripts to live in.

* `www/`

  The [YAMLScript Website](https://yamlscript.org) website.

* `yamltest/`

  A Clojure testing framework where tests are written in YAML.
  Used by the tests in `core/` and `ys/`.

* `ys/`

  The YAMLScript CLI bin util `ys` is built here.


## Makefiles

We use GNU `make` extensively for task automation in this project.
Almost every directory has a `Makefile`.
Common Makefile code is refactored into `common/*.mk` files.

To find the make targets available for a Makefile, type `make <TAB><TAB>` in the
directory containing the Makefile.
Or use `make -C <directory> <TAB><TAB>` to find the make targets available for
some other directory.

Here are some of the most common make targets.
Remember, you can use `make -C <directory> <target>` to run a make target in a
different directory.
By default, make targets are run in the current directory.

* `make build`

  Build the project in the current directory.

* `make test`

  Run the tests for the current directory.

* `make install [PREFIX=<prefix>]`

  Install the project.

  The `PREFIX` variable can be used to specify the installation prefix.
  For example, `make install PREFIX=./foo` will install stuff in `./foo/bin`,
  `./foo/lib`, etc.


### Clean Targets

* `make clean`

  Remove simple build artifacts.

* `make realclean`

  Remove all build artifacts, including those that require a build step to
  recreate.

* `make distclean`

  Remove all known generated artifacts that are untracked by git, including ones
  created by editors.

* `make sysclean`

  Works like `make realclean` but also removes all the cached build
  dependencies.


## Coding Guidelines

This is a growing list of coding guidelines for contributing to YAMLScript.

* Use the existing code as a guide

  The existing code is the best guide for how to write new code.
  If you're not sure how to do something, look at how it's done in the existing
  code.

* 80 character line length limit

  We try to keep the line length to 80 characters or less.
  Sometimes this is not possible, but try to do it.

  In addition, we prefer even shorter lines (around 50 characters) in general.

* New sentence, new line

  For documentation and comments, start each sentence on a new line.
  This makes for less noisy diffs.

* 2 space indentation

  Use the indentation style of the existing code, or the indentation style of
  the language you're writing bindings for.
  If in doubt, use 2 spaces.

* Bindings should strive to be literal ports

  The Python binding is currently the reference implementation binding.

  If you're writing a new language binding, try to make it as close to the
  Python binding as possible.
  If you are fixing an existing binding, also keep it in sync with the Python
  binding.

  This makes it easier to maintain the bindings and to keep them in sync.

* Commit message subject format

  Our commit subject lines start with the primary subdirectory involved,
  followed by `: `, then a short subject phrase.
  The phrase should begin with a capital letter and not end with a period.

  Also the commit subject line is added to the `Changes` change log file for
  each release.
  The `Changes` file is a valid YAML file.
  Thus the subject line you write must not cause the `Changes` file to be
  invalid YAML when it is added for a release.
