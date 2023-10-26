---
layout: home
title: YAMLScript.org
---

**YAMLScript — Program in YAML**

YAMLScript is a functional programming language whose syntax is encoded in
YAML.
YAMLScript can be used for writing new software applications and libraries.

Here's an example of a YAMLScript program called `hello.ys`:

```yaml
!yamlscript/v0

defn main(&[name]):
  greet: name || "world"

defn greet(name):
  say: "Hello, $name!"
```

You can run this program from the command line:

```bash
$ ys hello.ys
Hello, world!
$ ys hello.ys Bob
Hello, Bob!
```

YAMLScript can also be used in plain YAML files to add dynamic operations at any
level.
Here's an example of using YAMLScript in a YAML configuration file called
`db-config.yaml`:

```yaml
!yamlscript/v0

base =:
  ys::yaml.load-file: "db-defaults.yaml"
  # host: localhost
  # port: 12345
  # user: app
  # password: secret

=>::
  development::
    merge base::
      user: dev
      password: devsecret

  staging::
    merge base::
      host: staging-db.myapp.com

  production::
    merge base::
      host: prod-db.myapp.com
      user: prod
      password: prodsecret
```

From the command line, run:

```bash
$ ys --load db-config.yaml
[{"development":
  {"host":"localhost", "port":12345, "user":"dev", "password":"devsecret"}},
 {"staging":
  {"host":"staging-db.myapp.com", "port":12345, "user":"app", "password":"secret"}},
 {"production":
  {"host":"prod-db.myapp.com", "port":12345, "user":"prod", "password": "prodsecret"}}]
```

By default YAMLScript outputs JSON, but it can also output YAML by running:

```bash
$ ys --load --yaml db-config.yaml
```

You can use YAMLScript as a regular YAML loader module in a programming language
like Python:

```python
import yamlscript as ys
config = ys.load_file("db-config.yaml")
```

It loads the YAML data file just like PyYAML would, but with these added benefits:

* YAMLScript modules have the same API and work exactly the same in any
  programming language.
* YAMLScript uses the latest YAML 1.2 specification, which eliminates many of
  the complaints people often have about YAML.
* You can add dynamic operations to your YAML file by starting the file with a
  `!yamlscript/v0` tag.


## Installing `ys` - The YAMLScript Command Line Tool

The `ys` command line tool is the easiest way to get started with YAMLScript.

You can install the [latest release](
https://github.com/yaml/yamlscript/releases) (currently on Linux x86, macOS
aarch64) with:

```bash
$ curl -sSL https://yamlscript.org/install-ys | bash
```

Or you can install from source:

```bash
$ git clone https://github.com/yaml/yamlscript
$ cd yamlscript
$ make build
$ make install PREFIX=~/.local
$ export PATH=~/.local/bin:$PATH
```

The install process has the very minimal dependencies of `git`, `make`, `curl`,
and `bash`.
(The `libz-dev` package is also required on Linux.)

Test your new `ys` installation by running:

```text
$ ys --help
ys - The YAMLScript (YS) Command Line Tool

Usage: ys [options] [file]

Options:
  -r, --run                Compile and evaluate a YAMLScript file (default)
  -l, --load               Output the evaluated YAMLScript value
  -c, --compile            Compile YAMLScript to Clojure
  -e, --eval YSEXPR        Evaluate a YAMLScript expression
  -C, --clj                Treat input as Clojure code

  -m, --mode MODE          Add a mode tag: code, data, or bare (only for --eval/-e)
  -p, --print              Print the result of --run in code mode

  -o, --output             Output file for --load or --compile
  -t, --to FORMAT          Output format for --load

  -J, --json               Output JSON for --load
  -Y, --yaml               Output YAML for --load
  -E, --edn                Output EDN for --load

  -X, --debug              Debug mode: print full stack trace for errors
  -x, --debug-stage STAGE  Display the result of stage(s)
      --version            Print version and exit
  -h, --help               Print this help and exit
```

or:

```text
$ ys --version
YAMLScript 0.1.24
```

## Language Design

YAMLScript code compiles to Clojure code and then is evaluated by a Clojure
runtime engine.
This means that YAMLScript is a very complete language from the get-go.

Clojure is a Lisp dialect that runs on the JVM, however YAMLScript is not run on
the JVM.
No Java or JVM installation is used to run YAMLScript programs.

The YAMLScript compiler is written in Clojure and then compiled to a native
machine code binary using GraalVM.
It is standalone and quite fast.

It is also compiled into a native shared library that can be embedded into
almost any programming language.
YAMLScript intends to ship language bindings for the 42 most popular programming
languages.

YAMLScript syntax uses a combination of YAML structure and Clojure Lisp syntaxes
combined together.
The Lisp parts can use variants that make it feel more like Python or Ruby, instead of a Lisp.

How a YAMLScript program is syntactically styled is very much up to the
programmer.
She can go Full Lisp or full YAML, but most likely will use a combination of
the two will end up reading the best.


## Status

YAMLScript is currently in the early stages of development, but it is already
a working language.

Read the [YAMLScript Advent 2023](https://yamlscript.org/posts/advent-2023/)
posts for lots of explanations and examples of YAMLScript in action.

<!--
See the [YAMLScript Docs](https://yamlscript.org/doc/) for more information.
-->
