---
layout: home
title: YAMLScript.org
---

<p style="text-align: center; font-weight: bold">Program in YAML — Code is
Data</p>

**YAMLScript is a new YAML Loader** that can add "Super Powers" to your plain
old YAML config files.

YAMLScript intends to provide a loader library for every programming language
that uses YAML.
Currently we have working libraries for
[Clojure](https://clojars.org/org.yamlscript/clj-yamlscript),
[Go](https://github.com/yaml/yamlscript-go),
[Java](https://clojars.org/org.yamlscript/yamlscript),
[Julia](https://juliahub.com/ui/Packages/General/YAMLScript),
[NodeJS](https://www.npmjs.com/package/@yaml/yamlscript),
[Perl](https://metacpan.org/dist/YAMLScript/view/lib/YAMLScript.pod),
[Python](https://pypi.org/project/yamlscript/),
[Raku](https://raku.land/zef:ingy/YAMLScript),
[Ruby](https://rubygems.org/search?query=yamlscript) and
[Rust](https://crates.io/crates/yamlscript).

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

Here's an example of using YAMLScript in a YAML file called `file.yaml`:

```yaml
--- !yamlscript/v0/ &pets

cats:: load("cats.yaml")
dogs:: curl("https://yamlscript.org/dogs.yaml")
      .yaml/load().big

--- !yamlscript/v0/

about: A YAMLScript Example about Pets
title:: "$(ENV.USER.str/capitalize())'s Pets"
birds: !sort:
- Parrot
- Canary
- Owl
cats:: .*pets.cats
dogs:: .*pets.dogs.shuffle().take(2 _)
```

And these other files:
```bash
$ cat cats.yaml
- Siamese
- Persian
- Maine Coon

$ curl -s https://yamlscript.org/dogs.yaml
small:
- Chihuahua
- Pomeranian
- Maltese

big:
- Mastiff
- Great Dane
- Saint Bernard
- Otterhound
```

From the command line, run:

```bash
$ ys --load file.yaml
{"about":"A YAMLScript Example about Pets",
"title":"Ingy's Pets",
"birds":["Canary","Owl","Parrot"],
"cats":["Siamese","Persian","Maine Coon"],
"dogs":["Otterhound","Saint Bernard"]}
```

By default YAMLScript outputs JSON, but it can also output YAML by running:

```bash
$ ys -Y file.yaml
about: A YAMLScript Example about Pets
title: Ingy's Pets
birds:
- Canary
- Owl
- Parrot
cats:
- Siamese
- Persian
- Maine Coon
dogs:
- Great Dane
- Mastiff
```

----

You can get the same result from a programming language like Python by using its
YAMLScript loader library.
Here's a CLI one liner to do the same thing in Python:

```bash
$ python -c '
import yamlscript,yaml
ys = yamlscript.YAMLScript()
input = open("file.yaml").read()
data = ys.load(input)
print(yaml.dump(data))'
about: A YAMLScript Example about Pets
birds:
- Canary
- Owl
- Parrot
cats:
- Siamese
- Persian
- Maine Coon
dogs:
- Otterhound
- Mastiff
title: Ingy's Pets
```

----

**YAMLScript is also a new**, complete, full featured, general purpose,
functional and dynamic **programming language** whose syntax is encoded in
YAML.
YAMLScript can be used for writing new software applications and libraries.

Here's an example of a YAMLScript program called `99-bottles.ys`:

```yaml
{% include "../main/sample/rosetta-code/99-bottles-of-beer.ys" %}
```

You can run this program from the command line:

```bash
$ ys 99-bottles.ys 3
3 bottles of beer on the wall,
3 bottles of beer.
Take one down, pass it around.
2 bottles of beer on the wall.

2 bottles of beer on the wall,
2 bottles of beer.
Take one down, pass it around.
1 bottle of beer on the wall.

1 bottle of beer on the wall,
1 bottle of beer.
Take one down, pass it around.
No more bottles of beer on the wall.
```

YAMLScript can compile programs to native binary executables.
It's as simple as this:

```bash
$ ys -b 99-bottles.ys
* Compiling YAMLScript '99-bottles.ys' to '99-bottles' executable

$ time ./99-bottles 1
1 bottle of beer on the wall,
1 bottle of beer.
Take one down, pass it around.
No more bottles of beer on the wall.

real    0m0.010s
user    0m0.006s
sys     0m0.005s
```

That's pretty fast!

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
$ . <(curl https://yamlscript.org/try-ys)
```

This will install `ys` in a temporary directory and add it to the `PATH`
environment variable of your current shell session.

Or you can install the [latest release](
https://github.com/yaml/yamlscript/releases) with:

```bash
$ curl https://yamlscript.org/install | bash
```

Make sure that `~/.local/bin` is in your `PATH` environment variable.

To install elsewhere or install a specific version, set the `PREFIX` and/or
`VERSION` environment variables to the desired values:

```bash
$ curl https://yamlscript.org/install | PREFIX=/some/dir VERSION=0.1.xx bash
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
ys - The YAMLScript (YS) Command Line Tool - v0.1.74

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
  -U, --unordered          Mappings don't preserve key order (faster)

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
YAMLScript 0.1.74
```


## Installing a YAMLScript Library

YAMLScript can be installed as a YAML loader library (module) in several
programming languages.

So far there are libraries in these languages:
[Clojure](https://clojars.org/org.yamlscript/clj-yamlscript),
[Go](https://github.com/yaml/yamlscript-go),
[Java](https://clojars.org/org.yamlscript/yamlscript),
[Julia](https://juliahub.com/ui/Packages/General/YAMLScript),
[NodeJS](https://www.npmjs.com/package/@yaml/yamlscript),
[Perl](https://metacpan.org/dist/YAMLScript/view/lib/YAMLScript.pod),
[Python](https://pypi.org/project/yamlscript/),
[Raku](https://raku.land/zef:ingy/YAMLScript),
[Ruby](https://rubygems.org/search?query=yamlscript) and
[Rust](https://crates.io/crates/yamlscript).

Several more are in the works, and the goal is to get it to every language
where YAML is used.

Currently to install a YAMLScript library you need to install both the language
library and the matching version of `libyamlscript.so`.

For Python you would do:

```bash
$ pip install yamlscript
Successfully installed yamlscript-0.1.74
$ curl https://yamlscript.org/install | VERSION=0.1.74 install
Installed ~/.local/lib/libyamlscript.so - version 0.1.74
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

The YAMLScript compiler and runtime interpreter is written in Clojure and then
compiled to a native machine code binary using
[GraalVM](https://www.graalvm.org/)'s [native-image](
https://www.graalvm.org/22.0/reference-manual/native-image/) compiler.
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
She can go **Full Lisp** or **Full YAML**, but most likely using a combination
of the two will end up reading the best.


## Status

YAMLScript is already a working programming language but it does not yet have a
stable `v0` API release version.
In other words, you can use it now but some things _might_ change.

A stable release of YAMLScript `v0` is expected in 2024.

Once `v0` is announced stable, its API will remain backwards compatible for its
lifetime.
That is to say, any files containing `!yamlscript/v0` will always continue to
work the same.


## YAMLScript Resources

* [Web Site](https://yamlscript.org)
* [Documentation](https://yamlscript.org/doc)
* [Matrix Chat](https://matrix.to/#/#chat-yamlscript:yaml.io)
* [Slack Chat](https://clojurians.slack.com/archives/yamlscript)
* [Blog](https://yamlscript.org/blog)
* [March 2023 Talk](https://www.youtube.com/watch?v=GajOBwBcFyA)
* [GitHub Repository](https://github.com/yaml/yamlscript)
* [Discussions](https://github.com/yaml/yamlscript/discussions)
* [Issues](https://github.com/yaml/yamlscript/issues)


----
----
