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
YAMLScript v0.1.21
```

The best first command to run is `ys --help`:

```bash
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
$ ys -c -e 'say: 123' | ys -C
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

Finally there a 2 special debugging options:

* `--debug` (`-X`) - Print a full stack trace for errors (more info)
* `--debug-stage` (`-x`) - Display the result of a stage/stages

The `--debug-stage` option is super useful for understanding exactly how
YAMLScript code compiles to Clojure code.

For example, to see the internal AST when compiling some YAMLScript:

```bash
$ ys -c -e 'say: "Hello"' -xconstruct
*** construct output ***
{:Lst [{:Sym say} {:Str "Hello"}]}

(say "Hello")
```

And to see all 7 compilation stages:

```bash
$ ys -c -e 'say: "Hello"' -xall
*** parse output ***
({:+ "+MAP", :! "yamlscript/v0"}
 {:+ "=VAL", := "say"}
 {:+ "=VAL", :$ "Hello"}
 {:+ "-MAP"})

*** compose output ***
{:! "yamlscript/v0", :% [{:= "say"} {:$ "Hello"}]}

*** resolve output ***
{:ysm ({:ysx "say"} {:ysi "Hello"})}

*** build output ***
{:ysm ({:Sym say} {:Str "Hello"})}

*** transform output ***
{:ysm ({:Sym say} {:Str "Hello"})}

*** construct output ***
{:Lst [{:Sym say} {:Str "Hello"}]}

*** print output ***
"(say \"Hello\")\n"

(say "Hello")
```

We'll go over all of these stages in detail in a future post.

In the meantime, try out your new `ys` tool and see what you can do with it.
The more you use it, the sharper it will get.

I'll see you tomorrow for day 8 of YAMLScript Advent 2023!
