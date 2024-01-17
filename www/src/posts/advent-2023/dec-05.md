---
title: Unwrapping Gifts
date: '2023-12-05'
tags: [blog, advent-2023]
permalink: '{{ page.filePathStem }}/'
author:
  name: Ingy döt Net
  url: /about/#ingydotnet
---

It's certainly a relief now that I've told you the big secret about YAMLScript.
Now that you know that YAMLScript is really Clojure, I don't have to dance
around the subject anymore.
I didn't want to scare you away by going Full-Lisp on you from the start!

Now we can just get into it.
We can write, run and load YAMLScript until the cows come home.

But wait... How do we do that?
You don't even have YS installed yet!


### Welcome to Day 5 of the YAMLScript Advent Calendar

Today we're going to learn how to install YAMLScript a couple different ways.

Luckily for you I just finished creating the [first official YAMLScript
release](https://github.com/yaml/yamlscript/releases/tag/0.1.20).
What a coincidence!

At he moment there are only releases published for Linux x86_64 and macOS
aarch64.
If you happen to be running on one of those platforms, you can run the following
command to install YAMLScript's CLI, `ys`:

```bash
curl https://yamlscript.org/install | bash
```

This installer defaults to installing `ys` into `/usr/local/bin` so you probably
need to run it as root or with `sudo`:

```bash
curl https://yamlscript.org/install | sudo bash
```

If you want to install it somewhere else, like say `~/local/bin`, you can do:

```bash
curl https://yamlscript.org/install | PREFIX=~/local bash
```

Whereever you install it, make sure that the `$PREFIX/bin` directory is in your
`PATH` environment variable.

YAMLScript also provides a release for `libyamlscript.so`, the YAMLScript shared
library.
You can install it like above but with (some variation of):

```bash
curl https://yamlscript.org/install | bash
```

We'll be using the shared library soon when we start playing around with using
YAMLScript from other programming languages.


### Building from Source

The most reliable way to install YAMLScript is to build it from source.
We've put a lot of effort into making this as easy as possible.
You don't need any special prerequisites; just git, bash, curl and make.

The first thing you need to do is clone the YAMLScript repo:

```bash
git clone https://github.com/yaml/yamlscript
```

Then you can build and install the CLI with:

```bash
$ cd yamlscript
$ make build
$ sudo make install
or
$ make install PREFIX=...
```

The `make install` command will install both `ys` and `libyamlscript.so` into
`$PREFIX/bin` and `$PREFIX/lib` respectively.

> Note: Currently you should take care not to run `make build` as root.
Since `make install` triggers `make build`, running `sudo make install`
without first running `make build` may fail.
This should be fixed soon.

YAMLScript has a pretty sophisticated build system, built around GNU Make.
Even though the build uses Java, Clojure and GraalVM, you don't need to install
any of those things.
In fact, even if you have them installed, the build will ignore them.


### Running YAMLScript

Now that you have YAMLScript installed, you can run it.
Try:

```bash
$ ys --help
```

It should display:

```text
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

In the next day or two we'll go over all of these options in detail.

Here's a quick example of how to run YAMLScript to process a file from the
internet that Google just told me about:

```bash
$ curl https://gist.githubusercontent.com/chriscowley/8598119/raw/8f671464f914320281e5e75bb8dcbe11285d21e6/nfs.example.lan.yml |
  ys -J | jq .classes
{
  "nfs::server": {
    "exports": [
      "/srv/share1",
      "/srv/share3"
    ]
  }
}
```

When `ys` notices there is data on STDIN, it will read the YS program from
there.
The `-J` option tells `ys` to `--load` the YS and output the evaluation to JSON.

Well that's a wrap.
Thanks again for following along each day.

I'll see you tomorrow for day 6 of YAMLScript Advent 2023!


{% include "../../santa-secrets.md" %}
