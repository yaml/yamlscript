YAMLScript
==========

Program in YAML


## About YAMLScript

YAMLScript is a functional programming language with a stylized YAML syntax.

YAMLScript can be used for:

* Writing new programs and applications
  * Run with `ys file.ys`
  * Or compile to binary with `ys -C file.ys`
* Writing reusable shared libraries
  * Bindable to almost any programming language
* Using as a YAML loader module in many programming languages
  * Plain / existing YAML files
  * YAML files with new functional magics


### Run or Load?

YAMLScript programs can either be "run" or "loaded".
When a YAMLScript program is run, it is executed as a normal program.
When a YAMLScript program is loaded, it evaluates to a JSON-model data
structure.

Most existing YAML files in the wild are already valid YAMLScript programs.
If you have a valid YAML ([1.2 Core Schema](
https://yaml.org/spec/1.2.2/#103-core-schema)) file that doesn't use custom
tags, and loads to a value expressible in JSON, then it is a valid YAMLScript
program.
YAMLScript's `load` operation will evaluate that file exactly the same in any
programming language / environment.

These existing YAML files obviously can't use YAMLScript's functional
programming features since that would be ambiguous.
For example, what is the JSON value when "loading" this YAMLScript program?

```yaml
foo: inc(41)
```

Is it `{"foo": "inc(41)"}` or `{"foo": 42}`?

YAMLScript programs must start with a special YAML tag `!yamlscript/v0` to
indicate that they have functional capabilities.

```yaml
!yamlscript/v0/data
foo: ! inc(41)
```

> Note: The `/v0` in the tag indicates the YAMLScript API version.
This is so that future versions of YAMLScript can run programs written to an
older API version, and also so that older versions of YAMLScript don't try to
run programs written to a newer API version.


### Using YAMLScript

There are two primary ways to use YAMLScript:

* Using the `ys` command line runner / loader / compiler
* Using it a library in your own programming language

The `ys` command line tool is the easiest way to get started with YAMLScript.
It has these main modes of operation:

* `ys --run <file>` - Run a YAMLScript program
* `ys --load <file>` - Load a YAMLScript program
* `ys --compile <file>` - Compile a YAMLScript program to Clojure
* `ys --eval '<expr>'` - Evaluate a YAMLScript expression string
* `ys --repl` - Start an interactive YAMLScript REPL session
* `ys --help` - Show the `ys` command help

You can also use YAMLScript as a library in your own programming language.
For example, in Python you can use the `yamlscript` module like this:

```python
import yamlscript
ys = yamlscript.new(v=0)
text = open("foo.yaml").read()
data = ys.load(text)
```


### Supported Operating Systems

YAMLScript is supported on these operating systems:

* Linux
* macOS
* Windows  (work in progress)

YAMLScript is supported on these architectures:

* x86-64
* ARM64

For now other systems cannot be supported because `ys` and `libyamlscript` are
compiled by GraalVM's `native-image` tool, which only supports the above
systems.


### YAMLScript is a Lisp

Even though YAMLScript often has the look of an imperative programming language,
it actually is just a (YAML based) syntax that *compiles* to
[Clojure](https://clojure.org/) code.
The resulting Clojure code is then run by a native-machine-code Clojure runtime
called [Small Clojure Interpreter (SCI)](https://github.com/babashka/sci).

Clojure is a functional programming language with its own Lisp syntax.
Therefore it is fair to say that YAMLScript is a (functional) Lisp, even though
it commonly doesn't look like one syntactically.

Typically Clojure produces Java bytecode that is run on the JVM, but for
YAMLScript there is no Java or JVM involved.
In testing so far, YAMLScript programs tend to run as faster or faster than
equivalent Perl or Python programs.

For getting started with YAMLScript, you don't need to know anything about Lisp
or Clojure.
You can use it with as much or as little Lisp-ness as you want; the
syntax is quite flexible (*and even programmable!*).
As your YAMLScript programming requirements grow, you can rest assured that you
have the full power of Clojure at your disposal.


## Installing YAMLScript

At the moment, the best way to install YAMLScript is to build it from source,
but see the section "YAMLScript Releases" below.

This is very easy to do because YAMLScript has very few dependencies:

* `bash` (your interactive shell can be any shell)
* `curl`
* `git`
* `make`
* `zlib-dev` (need this installed on Linux)

To install the `ys` command line tool, and `libyamlscript` shared library,
run these commands:

```bash
git clone https://github.com/yaml/yamlscript
cd yamlscript
make build
sudo make install
```

The `make install` command will install `ys` and `libyamlscript` to
`/usr/local/bin` and `/usr/local/lib` respectively, by default.
This means that you will need to run `make install` with `sudo` or as root.
To install to a different location, run `make install PREFIX=/some/path`.

> Notes:
> * `make install` triggers a `make build` if needed, but...
> * You need to run `make build` not as root
> * The build can take several minutes (`native-image` is slow)
> * If you install to a custom location, you will need to add that location to
>   your `PATH` and `LD_LIBRARY_PATH` environment variables


### Installing YAMLScript Releases

YAMLScript now ships binary releases for a couple platforms [here](
https://github.com/yaml/yamlscript/releases).

To install a latest release for your machine platform, try one of these:

```
curl https://yamlscript.org/install-ys | bash
curl https://yamlscript.org/install-libyamlscript | bash
```

depending on what you are installing, `ys` or `libyamlscript`.

This will attempt to install stuff under `/usr/local` so you probably need to
use `... | sudo bash` to install as root.

To install to some other place use `... | PREFIX=other/place bash`.


### Installing a YAMLScript Binding for a Programming Language

> Note: This is still a work in progress.

For Python you can simply install the `yamlscript` module from
[PyPI](https://pypi.org/search/?q=yamlscript), like any other Python module:

```bash
pip install yamlscript
```

**But** you will also need to install the `libyamlscript` shared library as
detailed above.

The shared library is not installed along with the Python module for many
reasons, but the good news is that you can install it once and use it with any
YAMLScript binding for any programming language.


## The YAMLScript Repository

The [YAMLScript source code repository](https://github.com/yaml/yamlscript)
is a mono-repo containing:

* The YAMLScript compiler code
* The YAMLScript shared library code
* A YAMLScript binding module for each programming language
* The YAMLScript test suite
* The YAMLScript documentation
* The yamlscript.org website (with docs, blog, wiki, etc)


### `make` It So

The YAMLScript repository uses a `Makefile` system to build, test and install
its various offerings.
There is a top level `Makefile` and each repo subdirectory has its own
`Makefile`.
When run at the top level, many `make` targets like `test`, `build`, `install`,
`clean`, `distclean`, etc will invoke that target in each relevant subdirectory.

Given that this repository has so few dependencies, you should be able to clone
it and run `make` targets (try `make test`) without any problems.


### Contributing to YAMLScript

To ensure that YAMLScript libraries work the same across all languages, this
project aims to have a binding implementaion for each programming language.

If you would like to contribute a new YAMLScript binding for a programming
language, you are encouraged to
[submit a pull request](https://github.com/yaml/yamlscript/pulls) to this
repository.


## YAMLScript Resources

> Note: The documentation linked to below is out of date, but should give you a
decent idea of what YAMLScript is about.
It will be rewritten soon.

* [YAMLScript Documentation](
  https://metacpan.org/pod/Test::More::YAMLScript)
* [Example YAMLScript Programs](
  https://rosettacode.org/wiki/Category:YAMLScript)
* [YAMLScript Presentation Video](
  https://www.youtube.com/watch?v=9OcFh-HaCyI)


## Authors

* Ingy döt Net <ingy@ingy.net>


## Copyright and License

Copyright 2022-2024 by Ingy döt Net

This is free software, licensed under:

The MIT (X11) License
