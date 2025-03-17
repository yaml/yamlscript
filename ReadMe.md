YS / YAMLScript
===============

Program in YAML — Code is Data


## About YS

> See https://yamlscript.org for the most/latest/best information about YS.

YS is a functional programming language with a stylized YAML syntax.

YS can be used for:

* Extending YAML config files with functional transformations, external data
  access, string interpolation; anything a programming language has access to
* Writing new programs, applications, automation scripts
  * Run with `ys file.ys`
  * Or compile to binary with `ys -C file.ys`
* Writing reusable shared libraries
  * Bindable to almost any programming language
* As a YAML loader module in many programming languages
  * Load plain / existing YAML (or JSON) files
  * Load YAML files with embedded YS functionality

Most existing YAML files in the wild are already valid YS.

> YS is now an official language on the [Exercism](
  https://exercism.org/tracks) (free) language learning site!
  It's a great way to learn how to program in YS.


### Run or Load?

YS programs can either be "run" or "loaded".
When a YS program is run, it is executed as a normal program.
When a YS program is loaded, it evaluates to a JSON-model data structure.

If you have a valid YAML ([1.2 Core Schema](
https://yaml.org/spec/1.2.2/#103-core-schema)) file that doesn't use custom
tags, and loads to a value expressible in JSON, then it is a valid YS program.
The YS `load` operation will evaluate that file exactly the same in any
programming language / environment.

These existing YAML files obviously can't use the YS functional programming
features since that would be ambiguous.
For example, what is the JSON value when "loading" this YS program?

```yaml
foo: inc(41)
```

Is it `{"foo": "inc(41)"}` or `{"foo": 42}`?

YS programs must start with a special YAML tag `!YS-v0` to indicate
that they have functional capabilities.

```yaml
!YS-v0:
foo:: inc(41)
```

> Note: The `-v0` in the tag indicates the YS API version.
This is so that future versions of YS can run programs written to an older API
version, and also so that older versions of YS don't try to run programs
written to a newer API version.


### Using YS

There are two primary ways to use YS:

* Using the `ys` command line runner / loader / compiler / installer
* Using a YS library in your own programming language

The `ys` command line tool is the easiest way to get started with YS.
It has these main modes of operation:

* `ys <file>` - Run a YS program
* `ys --load <file>` - Load a YS program
* `ys --compile <file>` - Compile a YS program to Clojure
* `ys --binary <file>` - Compile YS to a native binary executable
* `ys --eval '<expr>'` - Evaluate a YS expression string
* `ys --install` - Install the latest libyamlscript shared library
* `ys --upgrade` - Upgrade ys and libyamlscript
* `ys --help` - Show the `ys` command help

You can also use YS as a library in your own programming language.
For example, in Python you can use the `yamlscript` module like this:

```python
import yamlscript
ys = yamlscript.YAMLScript()
text = open("foo.yaml").read()
data = ys.load(text)
```


### Supported Operating Systems

YS is supported on these operating systems:

* Linux
* macOS
* Windows  (work in progress)

YS is supported on these architectures:

* Intel/AMD (`x86_64`)
* ARM (`aarch64`)

For now other systems cannot be supported because `ys` and `libyamlscript` are
compiled by GraalVM's `native-image` tool, which only supports the above
systems.


### Supported Programming Language Bindings

YS wants to be the best YAML loader for both static and dynamic YAML usage in
every programming language where YAML is used.

It will have the same API, same features, same bugs and same bug fixes in every
language, giving you a great and consistent YAML experience everywhere.

At this early stage, YS has bindings for these programming languages:

* [Clojure](https://clojars.org/org.yamlscript/clj-yamlscript)
* [Go](https://github.com/yaml/yamlscript-go),
* [Java](https://clojars.org/org.yamlscript/yamlscript)
* [Julia](https://juliahub.com/ui/Packages/General/YAMLScript)
* [NodeJS](https://www.npmjs.com/package/@yaml/yamlscript)
* [Perl](https://metacpan.org/pod/YAMLScript)
* [Python](https://pypi.org/project/yamlscript/)
* [Raku](https://raku.land/zef:ingy/YAMLScript)
* [Ruby](https://rubygems.org/gems/yamlscript)
* [Rust](https://crates.io/crates/yamlscript)


### Is YS a Lisp?

Even though YS often has the look of an imperative programming language, it
actually is just a (YAML based) syntax that *compiles* to
[Clojure](https://clojure.org/) code.
The resulting Clojure code is then run by a native-machine-code Clojure runtime
called [Small Clojure Interpreter (SCI)](https://github.com/babashka/sci).

Clojure is a functional programming language with its own Lisp syntax.
Therefore it is fair to say that YS is a (functional) Lisp, even though it
commonly doesn't look like one syntactically.

Typically Clojure produces Java bytecode that is run on the JVM, but for YS
there is no Java or JVM involved.
In testing so far, YS programs tend to run as fast or faster than
equivalent Perl or Python programs.

For getting started with YS, you don't need to know anything about Lisp or
Clojure.
You can use it with as much or as little Lisp-ness as you want; the
syntax is quite flexible (*and even programmable!*).
As your YS programming requirements grow, you can rest assured that you have
the full power of Clojure at your disposal.


## Try the YS `ys` Command

You can try out the latest version of the `ys` command without actually
"installing" it.

If you run this command in Bash or Zsh:

```
. <(curl https://yamlscript.org/try-ys)
```

it will install the `ys` command in a temporary directory (under `/tmp/`) and
then add the directory to your current `PATH` shell variable.

This will allow you to try the `ys` command in your current shell only.
No other present or future shell session will be affected.

Try it out!


## Installing YS

You can install the YS `ys` interpreter and/or its `libyamlscript.so` shared
library from pre-built binaries or building from source.
Both are very easy to do.


### Installing YS Pre-built Binary Releases

YS ships pre-built binaries for each release version [here](
https://github.com/yaml/yamlscript/releases).

To install a latest release for your machine platform, try:

```bash
$ curl https://yamlscript.org/install | bash
```

Make sure `~/.local/bin` is in your `PATH` environment variable.

You can use the following environment variables to control the installation:

* `PREFIX=...` - The directory to install to. Default: `~/.local`
* `VERSION=...` - The YS version to install. Default: `0.1.95`
* `BIN=1` - Only install the `PREFIX/bin/ys` command line tool.
* `LIB=1` - Only install the `PREFIX/lib/libyamlscript` shared library.
* `DEBUG=1` - Print the Bash commands that are being run.

Once you have installed the `ys` command you can upgrade to a bin binary
version with `ys --upgrade`.


### Installing YS from Source

This is very easy to build and install YS from its source code because the YS
build process has very few dependencies:

* `bash` (your interactive shell can be any shell)
* `curl`
* `git`
* `make`

To install the `ys` command line tool, and `libyamlscript` shared library,
run these commands:

```bash
git clone https://github.com/yaml/yamlscript
cd yamlscript
make build
make install
```

That's it!

The `make install` command will install `ys` and `libyamlscript` to
`~/.local/bin` and `~/.local/lib` respectively, by default.
If run as root they will default to `/usr/local/bin` and `/usr/local/lib`.

To install to a different location, run `make install PREFIX=/some/path`.

> Notes:
> * `make install` triggers a `make build` if needed, but...
> * You need to run `make build` not as root
> * The build can take several minutes (`native-image` is slow)
> * If you install to a custom location, you will need to add that location to
>   your `PATH` and `LD_LIBRARY_PATH` environment variables


### Installing a YS Binding for a Programming Language

YS ships its language binding libraries and the `libyamlscript.so` shared
library separately.

Currently, each binding release version requires an exact version of the shared
library, or it will not work.
That's because the YS language is still evolving quickly.

The best way to install a binding library is to use your programming language's
package manager to install the latest binding version, and the YS installer to
install the latest shared library version.

So for Python you would:

```bash
$ pip install yamlscript
$ ys --install
```

The Perl installation process can automatically install the shared library, so
you can just do:

```bash
cpanm YAMLScript
```


## The YS Repository

The [YS source code repository](https://github.com/yaml/yamlscript)
is a mono-repo containing:

* The YS compiler code
* The YS shared library code
* A YS binding module for each programming language
* The YS test suite
* The YS documentation
* The yamlscript.org website (with docs, blog, wiki, etc)


### `make` It So

The YS repository uses a `Makefile` system to build, test and install its
various offerings.
There is a top level `Makefile` and each repo subdirectory has its own
`Makefile`.
When run at the top level, many `make` targets like `test`, `build`, `install`,
`clean`, `distclean`, etc will invoke that target in each relevant subdirectory.

Given that this repository has so few dependencies, you should be able to clone
it and run `make` targets (try `make test`) without any problems.


### Contributing to YS

To ensure that YS libraries work the same across all languages, this project
aims to have a binding implementation for each programming language.

If you would like to contribute a new YS binding for a programming language,
you are encouraged to
[submit a pull request](https://github.com/yaml/yamlscript/pulls) to this
repository.

See the YS [Contributing Guide](
https://github.com/yaml/yamlscript/tree/main/Contributing.md) for more details.


## YS Resources

* [YS Documentation](https://yamlscript.org/doc/)
* [YS Blog](https://yamlscript.org/blog/)
* [Learn YS at Exercism](http://exercism.org/tracks/yamlscript)
* [Example YS Programs on RosettaCode.org](
  https://rosettacode.org/wiki/Category:YAMLScript)


## Authors

* [Ingy döt Net](https://github.com/ingydotnet) - Creator / Lead
* [Ven de Thiel](https://github.com/vendethiel) - Language design
* [tony-o](https://github.com/tony-o) - Raku binding
* [Ethiraric](https://github.com/Ethiraric) - Rust binding
* [José Joaquín Atria](https://github.com/jjatria) - Perl binding
* [Delon R.Newman](https://github.com/delonnewman) - Clojure, Java, Ruby bindings
* [Andrew Pam](https://github.com/xanni) - Go binding
* [Kenta Murata](https://github.com/mrkn) - Julia binding


## Copyright and License

Copyright 2022-2025 by Ingy döt Net

This is free software, licensed under:

The MIT (X11) License
