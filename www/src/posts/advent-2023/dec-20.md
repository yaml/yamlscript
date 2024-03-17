---
title: Godspeed
date: '2023-12-20'
tags: [blog, advent-2023]
permalink: '{{ page.filePathStem }}/'
author:
  name: Ingy döt Net
  url: /about/#ingydotnet
---

I wonder if Santa has a Hemi?
Supercharged, Turbocharged?
Maybe a Nitro Burning Funny Sleigh?
Dude's got to get around the world in one night.
Godspeed, my festive friend!


### Welcome to Day 20 of the YAMLScript Advent Blog!

Are YAMLScript programs compiled or interpreted?
The answer is yes.

Clojure (thus YAMLScript) is a very dynamic language.
Clojure code gets compiled to Java bytecode just in time.
The JVM compiles the bytecode to machine code just in time.
Libraries that Clojure uses are compiled ahead of time.
The whole YAMLScript runtime was compiled by GraalVM native-image into a native
binary so there is no JVM involved for us in the end.

It's all pretty complicated.

But I was talking about YAMLScript programs being run by the YAMLScript CLI
`ys`.
You can think of `ys` as an interpreter like Python, Perl, Ruby or Java.
Sure, those languages _compile_ to an intermediate AST/opcode-tree to be faster,
but the programs are still interpreted (not binary compiled).

What if you could compile your YAMLScript program to a native binary?
Like an ELF file on Linux or a Mach-O file on macOS.

> Can you guess why I like Linux better this time of year? :- )

**As of today, you can natively compile YAMLScript programs!!**

Merry, Merry!

Let's check it out.
Remember our favorite drinking song from Day 9?
Here it is again:

```yaml
#!/usr/bin/env ys-0
# 99-bottles.ys

defn main(&[number]):
  each [n ((number || 99) .. 1)]:
    say:
      paragraph: n

defn paragraph(num): |
  $(bottles num) of beer on the wall,
  $(bottles num) of beer.
  Take one down, pass it around.
  $(bottles (num - 1)) of beer on the wall.

defn bottles(n):
  cond:
    (n == 0) "No more bottles"
    (n == 1) "1 bottle"
    :else    str(n " bottles")
```

Let's see how long it takes to drink 3 bottles:

```bash
$ time ys 99-bottles.ys 3
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

real    0m0.075s
```

75 milliseconds. Not bad.
Let's see if we can drink a little faster, shall we?

```bash
$ ys --native 99-bottles.ys
* Compiling YAMLScript '99-bottles.ys' to '99-bottles' executable
* Setting up build env in '/tmp/tmp.wpt7O1KsWg'
* This may take a few minutes...
[1/8] Initializing              (5.0s @ 0.08GB)
[2/8] Performing analysis               (20.7s @ 0.35GB)
[3/8] Building universe         (3.3s @ 0.35GB)
[4/8] Parsing methods           (2.3s @ 0.61GB)
[5/8] Inlining methods          (2.0s @ 0.44GB)
[6/8] Compiling methods         (22.2s @ 0.42GB)
[7/8] Layouting methods         (1.7s @ 0.44GB)
[8/8] Creating image            (2.3s @ 0.51GB)
* Compiled YAMLScript '99-bottles.ys' to '99-bottles' executable
$ ls -lh 99-bottles*
-rwxr-xr-x 1 ingy ingy 13M Dec 19 18:14 99-bottles*
-rwxr-xr-x 1 ingy ingy 468 Dec 19 18:10 99-bottles.ys*
```

It appears that we have birthed a new beer singer!
What goes better with beer than race cars?

<details><summary><strong>Answer</strong></summary>

Almost anything.
</details><p></p>

```bash
$ time ./99-bottles 3
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

real    0m0.016s
```

Woah! 16 milliseconds! Now we're drinking with gas!
Errr... never mind.

You may have noticed that the native binary is 13 megabytes.
That's because it contains the entire YAMLScript runtime.
Hopefully we can get that down to a smaller size in the future.

Also did you notice that it took an annoying amount of time to compile?

Let's time native compiling a minimal program:

```bash
$ time ys -Ce 'say: "Hello, world!"'
* Compiling YAMLScript '-e' to './EVAL' executable
* Setting up build env in '/tmp/tmp.mahWVLE9gi'
Could not find main function in '-e'

real    0m0.103s
```

I forgot to mention.
In order to `--native` compile a YAMLScript program, it must have a `main`
function.
That's an easy fix:

```bash
$ time ys -Ce 'defn main(): say("Hello, world!")'
* Compiling YAMLScript '-e' to './EVAL' executable
* Setting up build env in '/tmp/tmp.1zpmh6L1jM'
* This may take a few minutes...
[1/8] Initializing              (4.4s @ 0.17GB)
[2/8] Performing analysis               (17.4s @ 0.30GB)
[3/8] Building universe         (2.4s @ 0.53GB)
[4/8] Parsing methods           (2.4s @ 0.49GB)
[5/8] Inlining methods          (1.4s @ 0.63GB)
[6/8] Compiling methods         (20.4s @ 0.42GB)
[7/8] Layouting methods         (1.6s @ 0.46GB)
[8/8] Creating image            (2.5s @ 0.40GB)
* Compiled YAMLScript '-e' to './EVAL' executable

real    0m59.855s
```

Isn't that pretty cool?
You can native compile a `-e` one liner!

Since the output file needs a name, `ys` uses `./EVAL` when you use `-e`.
You can use the `-o` option to to name the file explicitly.
Otherwise it defaults to the name of the YAMLScript file with the `.ys`
extension removed.

The bad news is that it took almost a minute to compile.
Currently that's the price you pay for a one-liner race car!

Let's see if this little guy has the juice:

```bash
$ time ./EVAL
Hello, world!

real    0m0.010s
```

**Godspeed, you little global greeter!**

----

YAMLScript's `--native` compiler is based on GraalVM's `native-image` tool.
The same process that is used to compile the `ys` CLI binary.

I wish it were faster, but at least now we can be fast while developing our
YAMLScript programs and then compile them to native binaries when we are ready
to ship them.

Let's look at where YAMLScript is at now from a high level view:

* We have a new language that feels clean like Python
* It is actually a functional language adding reliability
* It's really Clojure so very complete and powerful
* It's embeddable in YAML so you can enhance existing YAML files
* It's a better YAML loader for plain old YAML with no magics
* It's quite fast when run with the YAMLScript CLI `ys`
* It's even faster when compiled to a native binary

I think we have a winner here!

There's still a long way to go on many fronts, but all of the above is true
today.
Full disclosure: I only came up the the `--native` idea 2 days ago.

Climb aboard and let's fly this baby to the moon!

Come back tomorrow for Day 21 of the YAMLScript Advent Blog!
