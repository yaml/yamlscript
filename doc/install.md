---
title: Installing YS
talk: 0
---

YS has 3 main things you might want to install:

1. The `ys` compiler, loader and runner command.
2. The `libyamlscript.so` shared library needed by a YS loader library for your
   particular programming language.
3. A [YS loader library](bindings.md#currently-available-libraries) for your
   particular programming language, like Python, Rust, NodeJS, etc.


### Quick Install of `ys` and `libyamlscript`

You can install both `ys` and `libyamlscript` with a single CLI command, where:

* `VERSION` defaults to the latest YS version.
* `PREFIX` defaults to `$HOME/.local`.
* `LIB=1` means only install the shared library.
* `BIN=1` means only install the `ys` binary.

Examples:
```bash
$ curl https://yamlscript.org/install | bash
$ curl https://yamlscript.org/install | VERSION=0.1.95 bash
$ curl https://yamlscript.org/install | VERSION=0.1.95 LIB=1 bash
$ curl https://yamlscript.org/install | PREFIX=/tmp/yamlscript bash
```

For the `ys` command you'll need to have `PREFIX/bin` in your `PATH`, but the
install script will tell you that.

For `libyamlscript`, unless you use the default `PREFIX` you'll need to add
`PREFIX/lib` to `LD_LIBRARY_PATH` and export that variable.

!!! note "An even shorter command to install `ys`"

    ```bash
    $ curl -s https://getys.org/ys | bash
    ```

    You can use all the same options as above (before `bash`).


<!--
### Temporary Test Install

If you just want to try out the `ys` command but not install it permanently, you
can run this (in Bash and Zsh only):
```bash
$ source <(curl https://yamlscript.org/try-ys)
```

This will install the `ys` binary under `/tmp/` and add the directory to you
current shell's `PATH`.
It will only be available for the duration of the shell session that you run it
in.
-->


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

!!! note

    On Linux it also requires the `libz-dev` package.


### Install a YS Loader Library

YS loader libraries are intended to be a  drop in replacement for your
current YAML loader.

YS loader libraries are currently available for these programming
languages:

* [Clojure](https://clojars.org/org.yamlscript/clj-yamlscript)
* [Go](https://github.com/yaml/yamlscript-go)
* [Java](https://clojars.org/org.yamlscript/yamlscript)
* [Julia](https://juliahub.com/ui/Packages/General/YAMLScript)
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

!!! note

    Currently when you install a YS loader library for your
    particular programming language you must also install the `libyamlscript`
    shared library of the **exact same version**.
    Fortunately this is simple.
