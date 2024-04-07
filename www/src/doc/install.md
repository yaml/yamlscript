---
title: Installing YAMLScript
---

YAMLScript has 3 main things you might want to install:

1. The `ys` compiler, loader and runner command.
2. The `libyamlscript.so` shared library needed by a YAMLScript loader library
   for your particular programming language.
3. The YAMLScript loader library itself, for your particular programming
   language.

Currently when you install a YAMLScript loader library for your particular
programming language you must also install the `libyamlscript` shared library of
the **exact same version**.
Fortunately this is simple.


### Quick Install of `ys` and `libyamlscript`

You can install both `ys` and `libyamlscript` with a single CLI command, where:

* `VERSION` defaults to the latest YAMLScript version.
* `PREFIX` defaults to `$HOME/.local`.
* `LIB=1` means only install the shared library.
* `BIN=1` means only install the `ys` binary.

Examples:
```bash
$ curl -sSL yamlscript.org/install | bash
$ curl -sSL yamlscript.org/install | VERSION=0.1.47 bash
$ curl -sSL yamlscript.org/install | VERSION=0.1.47 LIB=1 bash
$ curl -sSL yamlscript.org/install | PREFIX=/tmp/yamlscript bash
```

You'll need to have `PREFIX/bin` in your `PATH`.
Unless you use the default `PREFIX` you'll need to add `PREFIX/lib` to
`LD_LIBRARY_PATH` and export that variable.


### Temporary Test Install

If you just want to try out the `ys` command but not install it permanently, you
can run this (in Bash and Zsh only):
```bash
$ source <(curl -sSL yamlscript.org/try-ys)
```

This will install the `ys` binary under `/tmp/` and add the directory to you
current shell's `PATH`.
It will only be available for the duration of the shell session that you run it
in.


### Download and Install

All the binary pre-built release files are
[here](https://github.com/yaml/yamlscript/releases).

* Download the appropriate release file.
* Expand the file with `$ tar xf <release-file>`.
* Use `cd` to enter the release directory.
* Run `make install` or `make install PREFIX=...`.
  * Or just copy the binary file to the place where you want it.


### Build a Release from Source

You can also easily build and install `ys` and `libyamlscript` from source:

* Download the "Source code" release file.
* Expand the file with `$ tar xf <release-file>`.
* Use `cd` to enter the release directory.
* Run `make install` or `make install PREFIX=...`.

This will take a few minutes but it requires no dependencies besides `bash`,
`make` and `curl`.

> On linux it also requires the `libz-dev` package.


### Install a YAMLScript Loader Library

YAMLScript loader libraries are intended to be a  drop in replacement for your
current YAML loader.

YAMLScript loader libraries are currently available for these programming
languages:

* [Clojure](https://clojars.org/org.yamlscript/clj-yamlscript)
* [Java](https://clojars.org/org.yamlscript/yamlscript)
* [NodeJS](https://www.npmjs.com/package/@yaml/yamlscript)
* [Perl](https://metacpan.org/pod/YAMLScript)
* [Python](https://pypi.org/project/yamlscript/)
* [Raku](https://raku.land/zef:ingy/YAMLScript)
* [Ruby](https://rubygems.org/gems/yamlscript)
* [Rust](https://crates.io/crates/yamlscript)

Install the library you want using the normal library installer for your
language.
Then install the matching version of the `libyamlscript` shared library as
described above.
