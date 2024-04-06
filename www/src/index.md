---
layout: home
title: YAMLScript.org
---

<p style="text-align: center; font-weight: bold">YAMLScript — Program in
YAML</p>

**YAMLScript is a new YAML Loader** that can add "Super Powers" to your plain
old YAML config files.

By using YAMLScript as your YAML loader, You can dynamically include data from
other data files (YAML, JSON, XML, CSV, etc), pull data in from the web or even
from a database.
You can filter, map, merge, reduce, generate, concatenate, interpolate and
manipulate your data to your heart's content.

If you've ever wanted more from your YAML files, YAMLScript has you covered.
You can easily mix logic into your data files at any point.

On the other hand, if you just want a rock solid YAML 1.2 loader (without any
code evaluation magic) that works the same in any programming language, you
should also give YAMLScript a try.

All valid [YAML 1.2 Core Schema](https://yaml.org/spec/1.2.2/#103-core-schema)
files are also valid YAMLScript files!  That's pretty much any YAML config file
you already have.

Without the special `!yamlscript/v0` tag at the top, your YAMLScript loader
will load any existing YAML (or JSON) just as one would expect a normal YAML
loader to do.

Later you can add the special tag and take your YAML capabilities to a whole
new level!

----

Here's an example of using YAMLScript in a YAML configuration file called
`db-config.yaml`:

```yaml
{% include "../main/sample/www/db-config.yaml" %}
```

From the command line, run:

```bash
$ ys --load db-config.yaml production
{"host":"prod-db.myapp.com","port":12345,"user":"prod","password":"prodsecret"}
```

By default YAMLScript outputs JSON, but it can also output YAML by running:

```bash
$ ys --load --yaml db-config.yaml
host: localhost
port: 12345
user: dev
password: devsecret
```

Notice the first time we ran the command, we passed in the `production` level
key, and it loaded our "production" data.
The second time we ran the command, we didn't pass in a key, so it loaded the
default "development" data.
We specified that default with `main(level='development')`.

----

You can use YAMLScript as a regular YAML loader library in a programming
language like Python:

```python
{% include "../main/sample/www/example.py" %}
```

It loads the YAML data file just like PyYAML would, but with these added
benefits:

* YAMLScript libraries have the same API and work exactly the same in any
  programming language.
* YAMLScript uses the latest YAML 1.2 specification, which eliminates many of
  the complaints people often have about YAML.
* You can add dynamic operations to your YAML file by starting the file with a
  `!yamlscript/v0` tag.

----

**YAMLScript is also a new**, complete, full featured, general purpose,
functional and dynamic **programming language** whose syntax is encoded in
YAML.
YAMLScript can be used for writing new software applications and libraries.

Here's an example of a YAMLScript program called `hello.ys`:

```yaml
{% include "../main/sample/www/hello.ys" %}
```

You can run this program from the command line:

```bash
$ ys hello.ys
Hello, world!
$ ys hello.ys Jack
Hello, Jack!
```

YAMLScript can compile programs to native binary executables.
It's as simple as this:

```bash
$ ys -b hello.ys
$ ./hello Jack
Hello, Jack!
```

The YAMLScript language has all the things you expect from a modern programming
language including:

* Using builtin and third party libraries
* Defining your own namespaces and functions
* All the standard data types and structures
* Standard libraries with hundreds of battle tested functions
* Reasonable performance on par with common dynamic languages


## Installing `ys` - The YAMLScript Command Line Tool

The `ys` command line tool is the easiest way to get started with YAMLScript.
It's currently available on Linux and macOS for both Intel and ARM.

You can try `ys` out temporarily (for the duration of your shell session) by
running this command in your terminal:

```bash
$ . <(curl -sSL yamlscript.org/try-ys)
```

This will install `ys` in a temporary directory and add it to the `PATH`
environment variable of your current shell session.

Or you can install the [latest release](
https://github.com/yaml/yamlscript/releases) with:

```bash
$ curl -sSL yamlscript.org/install | bash
```

Make sure that `~/.local/bin` is in your `PATH` environment variable.

To install elsewhere or install a specific version, set the `PREFIX` and/or
`VERSION` environment variables to the desired values:

```bash
$ curl -sSL yamlscript.org/install | PREFIX=/some/dir VERSION=0.1.xx bash
```

> NOTE: The default `PREFIX` is `~/.local` (or `/usr/local` if you run the
command as `root`).

You can also install `ys` from source:

```bash
$ git clone https://github.com/yaml/yamlscript
$ cd yamlscript
$ make build
$ make install
$ export PATH=~/.local/bin:$PATH
```

> NOTE: The pre-built binaries currently fail on some older kernels.
> If you have trouble with the pre-built binaries, try building from source.

The install process has the very minimal dependencies of `git`, `make`, `curl`,
and `bash`.
(The `libz-dev` package is also required on Linux.)

Test your new `ys` installation by running:

```text
$ ys --help

ys - The YAMLScript (YS) Command Line Tool - v0.1.52

Usage: ys [<option...>] [<file>]

Options:

      --run                Run a YAMLScript program file (default)
  -l, --load               Output (compact) JSON of YAMLScript evaluation
  -e, --eval YSEXPR        Evaluate a YAMLScript expression
                           multiple -e values joined by newline

  -c, --compile            Compile YAMLScript to Clojure
  -b, --binary             Compile to a native binary executable

  -p, --print              Print the result of --run in code mode
  -o, --output FILE        Output file for --load, --compile or --binary

  -T, --to FORMAT          Output format for --load:
                             json, yaml, edn
  -J, --json               Output (pretty) JSON for --load
  -Y, --yaml               Output YAML for --load
  -E, --edn                Output EDN for --load

  -m, --mode MODE          Add a mode tag: code, data, or bare (for -e)
  -C, --clojure            Treat input as Clojure code

  -d                       Debug all compilation stages
  -D, --debug-stage STAGE  Debug a specific compilation stage:
                             parse, compose, resolve, build,
                             transform, construct, print
                           can be used multiple times
  -S, --stack-trace        Print full stack trace for errors
  -x, --xtrace             Print each expression before evaluation

      --install            Install the libyamlscript shared library
      --upgrade            Upgrade both ys and libyamlscript

      --version            Print version and exit
  -h, --help               Print this help and exit
```

or:

```text
$ ys --version
YAMLScript 0.1.47
```


## Installing a YAMLScript Library

YAMLScript can be installed as a YAML loader library (module) in several
programming languages.

So far there are libraries in these languages:

* Clojure
* Java
* NodeJS
* Perl
* Python
* Raku
* Rust
* Ruby

Several more are in the works, and the goal is to get it to every language
where YAML is used.

Currently to install a YAMLScript library you need to install both the language
library and the matching version of `libyamlscript.so`.

For Python you would do:

```bash
$ pip install yamlscript
Successfully installed yamlscript-0.1.52
$ curl -sSL yamlscript.org/install | VERSION=0.1.52 install
Installed ~/.local/lib/libyamlscript.so - version 0.1.52
```

For some other language, use that language's library installer.
Just make sure the versions match for the library and libyamlscript.


## YAMLScript Language Design

YAMLScript code compiles to Clojure code and then is evaluated by a Clojure
runtime native binary engine.
This means that YAMLScript is a very complete language from the get-go.

> NOTE: To see the generated Clojure code for any YAMLScript code just use the
> `-c` (`--compile`) flag for `ys`:
>
> ```bash
> $ ys -c -e 'say: "Hello"'
> (say "Hello")
> ```

Clojure is a Lisp dialect that runs on the JVM, however YAMLScript is not run
on the JVM.
No Java or JVM installation is used to run (or build) YAMLScript programs.

The YAMLScript compiler is written in Clojure and then compiled to a native
machine code binary using GraalVM.
It is standalone and quite fast.

It is also compiled into a native shared library that can be embedded into
almost any programming language.
YAMLScript intends to ship language bindings for (at least) 42 popular
programming languages.

YAMLScript syntax uses a combination of YAML structure and Clojure Lisp code
syntaxes combined together.
The code parts have syntax variants that make it feel more like Python or Ruby
than a Lisp.

How a YAMLScript program is syntactically styled is very much up to the
programmer.
She can go Full Lisp or full YAML, but most likely using a combination of the
two will end up reading the best.


## Status

YAMLScript is already a working programming language but it does not yet have a
stable release version.

A stable release of YAMLScript `v0` is expected in Q2 of 2024.

Once `v0` is announced stable, it's API will remain backwards compatible for
its lifetime.
That is to say, any files containing `!yamlscript/v0` will always continue to
work the same.


## Further Reading

Read the [YAMLScript Advent 2023](https://yamlscript.org/posts/advent-2023/)
posts for lots of explanations and examples of YAMLScript in action.

Documentation is coming soon!

<!--
See the [YAMLScript Docs](https://yamlscript.org/doc/) for more information.
-->

----
----
