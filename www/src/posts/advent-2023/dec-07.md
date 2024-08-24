---
title: Sharpen Your Tools
date: '2023-12-07'
tags: [blog, advent-2023]
permalink: '{{ page.filePathStem }}/'
author:
  name: Ingy döt Net
  url: /about/#ingydotnet
---

Wanna make some fun toys with YAMLScript?
You'll need some sharp tools.
You think those elves make all those toys with dull tools?

The CLI tool `ys` is the main tool you'll use to work with YAMLScript.
Today we'll learn about all the things you can do with it.


### Welcome to Day 7 of the YAMLScript Advent Calendar

On Tuesday you learned how to install YAMLScript.
Reminder, here's the quick way to install the latest version:

```bash
$ curl https://yamlscript.org/install | PREFIX=~/.yamlscript bash
$ export PATH=$HOME/.yamlscript/bin:$PATH
$ ys --version
YAMLScript v0.1.72
```

The best first command to run is `ys --help`:

```bash
$ ys --help

ys - The YAMLScript (YS) Command Line Tool - v0.1.72

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


### Ready, set, actions!

The first thing to notice is that `ys` has 3 "actions":

* `--run` (default) - Compile and evaluate a YAMLScript file
* `--load` - Output the evaluated YAMLScript value as JSON (by default)
* `--compile` - Compile YAMLScript code to Clojure code

For each action you'll need some YAMLScript source code.
This can come from 3 different places:

* The `--eval` (`-e`) option - specifies a line of YAMLScript code.
  You can use this option multiple times to specify multiple lines of code.
* A file path - specify a path to a file containing YAMLScript code.
* Standard input - specify `-` as the file path to read YAMLScript code from
  standard input.
  If no file or `-e` options are specified, `ys` will check to see if there is
  data on stdin.
  That means you can leave off the `-` and pipe data into `ys` like this:
  `echo $'!yamlscript/v0\nsay: "Hello"' | ys`.
  Of course, it doesn't hurt to specify the `-` anyway.


### Running Clojure  with `ys`

Clojure code is often valid YAMLScript code:

```bash
$ ys --compile -e '(println (+ 1 2))'
(println (+ 1 2))
```

This YAMLScript compiles to the exact same Clojure code.

If you want the code you run to be considered to be Clojure code (thus not
compiled by the yamlscript compiler), you can use the `--clj` (`-C`) option.
This is useful when you want to test out the YAMLScript runtime envronment
directly with Clojure code.

Also you can pipe the output of `ys --compile` to `ys --clj` to run the
compiler's Clojure code ouput:

```bash
$ ys -c -e 'say: 123' | ys -C -
```


### Modes and Formats

We learned about modes and the `--mode` option yesterday.
You can set the mode for `--eval` (`-e`) code with the `--mode` (`-m`) option.
The accepted values are `code` (`c`), `data` (`d`) and `bare` (`b`).

When you "load" YAMLScript using `--load` you get the result printed to stdout
as JSON.
These are 3 formatting options to control how the output is displayed:

* `--json` (`-J`) - Output JSON (default)
  Note that this JSON is a bit more nicely formatted than the default output.
* `--yaml` (`-Y`) - Output YAML
* `--edn` (`-E`) - Output EDN. EDN is Clojure's native data format.
  It is also valid Clojure code.

Note that when you specify a formatting option, it implies the `--load` action.


### Debugging

When you "run" a YAMLScript program it doesn't print anything unless you use a
printing command.
This isn't surprising; all languages work this way.

Sometimes you want to know what the final value of the run was.
To get this you could print it with `say.`
You can also use the special `--print` (`-p`) option, which does exactly that
(with less typing)..

Finally there a 3 special debugging options:

* `--stack-trace` (`-S`) - Print a full stack trace for errors (more info)
* `--debug-stage` (`-D`) - Display the result of a stage/stages
* `-d` - Short for `--debug-stage=all` - Display the result of all stages

The `--debug-stage` option is super useful for understanding exactly how
YAMLScript code compiles to Clojure code.

For example, to see the internal AST when compiling some YAMLScript:

```bash
$ ys -c -e 'say: "Hello"' -Dconstruct
*** construct output ***
{:Lst [{:Sym say} {:Str "Hello"}]}

(say "Hello")
```

And to see all 7 compilation stages:

```bash
$ ys -c -e 'say: "Hello"' -d
*** parse     *** 0.181519 ms
({:+ "+MAP", :! "yamlscript/v0/code"}
 {:+ "=VAL", := "say"}
 {:+ "=VAL", :$ "Hello"}
 {:+ "-MAP"}
 {:+ "-DOC"})

*** compose   *** 0.005334 ms
{:! "yamlscript/v0/code", :% [{:= "say"} {:$ "Hello"}]}

*** resolve   *** 0.055135 ms
{:pairs [{:exp "say"} {:vstr "Hello"}]}

*** build     *** 0.102548 ms
{:pairs [{:Sym say} {:Str "Hello"}]}

*** transform *** 0.014468 ms
{:pairs [{:Sym say} {:Str "Hello"}]}

*** construct *** 0.048013 ms
{:Top [{:Lst [{:Sym say} {:Str "Hello"}]}]}

*** print     *** 0.006561 ms
"(say \"Hello\")"

(say "Hello")
```

We'll go over all of these stages in detail in a future post.

In the meantime, try out your new `ys` tool and see what you can do with it.
The more you use it, the sharper it will get.

I'll see you tomorrow for day 8 of YAMLScript Advent 2023!
