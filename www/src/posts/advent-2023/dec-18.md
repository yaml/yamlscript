---
title: Santa's in d'buggy
date: '2023-12-18'
tags: [blog, advent-2023]
permalink: '{{ page.filePathStem }}/'
author:
  name: Ingy döt Net
  url: /about/#ingydotnet
---

With one week to go, Santa's gotta get his sleigh in top shape.
Can't have any breakdowns on the big night.
His sleigh might look like a simple wooden buggy, but it's more temperamental
and buggy than a 2023 Tesla!

But this is Santa we're talking about.
He's done this a few times, so he knows how to get the bugs out.


### Welcome to Day 18 of the YAMLScript Advent Calendar

Today we're going to look at a few ways to debug YAMLScript programs.
We'll also cover some of the common mistakes that you might make when writing
YAMLScript code.

```yaml
# hello.ys
say: "Hello, world!"
```

Let's run this very simple program:

```bash
$ ys hello.ys
$
```

Hmmm. Nothing happened. What's wrong?

This first thing I do when my YAMLScript program doesn't work is see what the
Clojure code that it compiled to looks like.
We didn't get a compile error there when we ran `ys`, so let's look at the
code we were running:

```bash
$ ys hello.ys -c
{"say" "Hello, world!"}
```

Oh snap! We forgot to start the program with `!yamlscript/v0`.
The program started of in `bare` mode, which is just a YAML mapping.

We also could have run the program with `--print` to see what it evaluated to:

```bash
$ ys hello.ys -p
{"say" "Hello, world!"}
```

Same thing. Let's fix the program now:

```yaml
!yamlscript/v0
say: "Hello, world!"
```
now:
```bash
$ ys hello.ys
Hello, world!
```

That's better.

----

Let's write a program to dynamically generate a list of numbers:

```yaml
# map.ys
!yamlscript/v0
map inc: [1 2 3]
```

This program doesn't `say` anything.
That's because we are using it to generate data, so we'll `--load` it:

```bash
$ ys map.ys -l
Error: java.lang.Exception: Sequences (block and flow) not allowed in code mode{:eval [], :debug-stage {}}
```

That's scary!
And what's up with Java?!
I don't think it even compiled.

When this happens, I like to debug the 7 layers of YAMLScript compilation, with
the `--debug-stage=all` option, aka `-xall`:

```bash
$ ys map.ys -xall
```

```txt
$ ys map.ys -l -xall
*** parse output ***
({:+ "+MAP", :! "yamlscript/v0"}
 {:+ "=VAL", := "map inc"}
 {:+ "+SEQ", :flow true}
 {:+ "=VAL", := "1 2 3"}
 {:+ "-SEQ"}
 {:+ "-MAP"})

*** compose output ***
{:! "yamlscript/v0", :% [{:= "map inc"} {:-- [{:= "1 2 3"}]}]}

Error: java.lang.Exception: Sequences (block and flow) not allowed in code mode{:eval [], :debug-stage {"parse" true, "compose" true, "resolve" true, "build" true, "transform" true, "construct" true, "print" true}, :load true}
```

The 7 stages of YAMLScript compilation are: `parse`, `compose`, `resolve`,
`build`, `transform`, `construct`, and `print`.
It looks like we are getting an error in the `resolve` stage.

The `-xall` option means the same thing as `-xparse -xcompose -xresolve -xbuild
-xtransform -xconstruct -xprint`.

So we parsed the YAML input into pieces and then composed a tree out of them.
In the resolve stage we look at each node of the tree and figure out what it
means semantically.

YAMLScript doesn't allow sequences in code mode. And it doesn't allow any flow
style collections `[] {}` in code mode either.
But we wrote `[1 2 3]`, not `[1, 2, 3]`.
To YAML, `[1 2 3]` is valid but it means `["1 2 3"]`.
We really meant this list to be a YAMLScript ysexpr vector not a YAML sequence.

We wanted YAML to see the RHS as a scalar value, not a sequence.
YAML plain (unquoted) scalars can't begin with certain characters, like `[`,
`{`, `*`, `&`, `!`, `|`, `>`, `%`, `@`, `#` etc because they are YAML syntax.
In YAMLScript when we want a ysexpr string that starts with one of these
characters, we can escape it with a dot `.`.

```yaml
!yamlscript/v0
map inc: .[1 2 3]
```

And let's just check the resolve stage this time:

```bash
$ ys map.ys -l -xresolve
```

```txt
$ ys map.ys -l -xresolve
*** resolve output ***
{:ysm [{:ysx "map inc"} {:ysx "[1 2 3]"}]}

[2,3,4]
```

It resolved! And it worked! We got our list of numbers.

> Note: The error message indicated a `java.lang.Exception`.
Remember that YAMLScript is Clojure and Clojure is Java.
The JVM is compiled out of the picture in YAMLScript, but the error message
still comes from Java stuff.

----

Here's a little program to calculate the factorial of a number:

```yaml
# factorial.ys
!#/usr/bin/env ys-0

defn main(n):
  say: factorial(n)

defn factorial(x):
  apply *: 2..x
```

Let's see how it works:

```txt
$ ys factorial.ys
Error: Wrong number of args (0) passed to: sci.impl.fns/fun/arity-1--3508

$ ys factorial.ys 10
3628800
$ ys factorial.ys 20
2432902008176640000
$ ys factorial.ys 30
Error: long overflow
```

Two of the four runs we got an error.
Hopefully the errors are pretty obvious.
The first time we forgot the number it wanted.
The second time we tried to calculate a number that was too big for a 64 bit
integer.

This was a very small program, but when things blow up, it's nice to have a
stack trace to see exactly where the error happened and what code path it took
to get there.
Especially when many library files are involved.

You can see the stack trace on any error by using the `--stack-trace` option aka
`-X`:

```txt
$ ys factorial.ys 30 -X
Error: {:stack-trace true,
 :cause "long overflow",
 :file nil,
 :line nil,
 :column nil,
 :trace
 [[clojure.lang.Numbers multiply "Numbers.java" 1971]
  [clojure.lang.Numbers$LongOps multiply "Numbers.java" 503]
  [clojure.lang.Numbers multiply "Numbers.java" 175]
  [clojure.core$_STAR_ invokeStatic "core.clj" 1018]
  [clojure.core$_STAR_ invoke "core.clj" 1010]
  [clojure.lang.LongRange$LongChunk reduce "LongRange.java" 316]
  [clojure.core$reduce1 invokeStatic "core.clj" 944]
  [clojure.core$_STAR_ invokeStatic "core.clj" 1020]
  [clojure.core$_STAR_ doInvoke "core.clj" 1010]
  [clojure.lang.RestFn applyTo "RestFn.java" 142]
  [clojure.core$apply invokeStatic "core.clj" 667]
  [clojure.core$apply invoke "core.clj" 662]
  [sci.lang.Var invoke "lang.cljc" 202]
  [sci.impl.analyzer$return_call$reify__4621 eval "analyzer.cljc" 1422]
  [sci.impl.fns$fun$arity_1__3508 invoke "fns.cljc" 107]
  [sci.lang.Var invoke "lang.cljc" 200]
  [sci.impl.analyzer$return_call$reify__4617 eval "analyzer.cljc" 1422]
  [sci.impl.analyzer$return_call$reify__4617 eval "analyzer.cljc" 1422]
  [sci.impl.fns$fun$arity_1__3508 invoke "fns.cljc" 107]
  [clojure.lang.AFn applyToHelper "AFn.java" 154]
  [clojure.lang.AFn applyTo "AFn.java" 144]
  [clojure.core$apply invokeStatic "core.clj" 667]
  [clojure.core$apply invoke "core.clj" 662]
  [sci.lang.Var invoke "lang.cljc" 202]
  [sci.impl.analyzer$return_call$reify__4621 eval "analyzer.cljc" 1422]
  [sci.impl.interpreter$eval_form invokeStatic "interpreter.cljc" 40]
  [sci.impl.interpreter$eval_string_STAR_
   invokeStatic
   "interpreter.cljc"
   66]
  [sci.impl.interpreter$eval_string_STAR_ invoke "interpreter.cljc" 57]
  [sci.impl.interpreter$eval_string_STAR_
   invokeStatic
   "interpreter.cljc"
   59]
  [sci.impl.interpreter$eval_string invokeStatic "interpreter.cljc" 77]
  [sci.core$eval_string invokeStatic "core.cljc" 225]
  [yamlscript.runtime$eval_string invokeStatic "runtime.clj" 114]
  [yamlscript.cli$do_run invokeStatic "cli.clj" 221]
  [yamlscript.cli$do_default invokeStatic "cli.clj" 284]
  [yamlscript.cli$_main invokeStatic "cli.clj" 381]
  [yamlscript.cli$_main doInvoke "cli.clj" 370]
  [clojure.lang.RestFn applyTo "RestFn.java" 137]
  [yamlscript.cli main nil -1]
  [java.lang.invoke.LambdaForm$DMH/sa346b79c
   invokeStaticInit
   "LambdaForm$DMH"
   -1]]}
```

Well... You asked for it. :- )


----

Print debugging is a great way to debug programs.
YAMLScript provides some help here with it's `www`, `xxx`, `yyy`, and `zzz`
standard library functions.
Conceptually these come from an old Perl module I wrote years ago called
[XXX](https://metacpan.org/pod/XXX).

* `www` warns (prints to stderr) it's argument and returns it.
* `xxx` dies (prints and then terminates) it's argument.
* `yyy` prints it's argument as YAML and returns it.
* `zzz` is like `xxx` but it prints the stack trace too.

Here's a contrived example that passes data through a pipeline of functions:

```yaml
# pipeline.ys
!yamlscript/v0
->> (1..10):
  map: inc
  filter: \(= 0 (mod % 2))  # odd?
  reduce: +
  =>: say
```

Check it:

```txt
$ ys pipeline.ys
30
```

The `->>` function is Clojure's threading macro.
It lets you pass a value through a pipeline of transformation functions without
having to reverse nest them in a ton of parentheses.
It's quite nice and handy.

Often times when I'm writing a pipeline like this, I want to see what the data
looks like after a particular transformation or maybe after several of them.
I almost always us `www` for this.

```yaml
!yamlscript/v0
->> (1..10):
  www: "before map"
  map: inc
  www: "after map"
  filter: \(= 0 (mod % 2))  # odd?
  www: "after filter"
  reduce: +
  www: "after reduce"
  =>: say
  =>: www
```

The `www` function can actually take multiple arguments.
It prints them all and returns the last one.
The `->>` threading macro adds its value as the last argument to each function.
So the way we did it here we are adding a label to each debugging section.

I used `=>: www` to show how to call it with no extra label argument.
Remember that `=>:` is the YAMLScript way to write a mapping pair when you only
need one thing (the `www` function in this case).

```txt
$ ys pipeline.ys
---
("before map" (1 2 3 4 5 6 7 8 9 10))
...
---
("after map" (2 3 4 5 6 7 8 9 10 11))
...
---
("after filter" (2 4 6 8 10))
...
---
("after reduce" 30)
...
30
---
nil
...
```

Each www call wraps the output with a `---` and a `...` so you can see where
the output starts and ends.

----

I hope you enjoyed this little tour of YAMLScript debugging.
There are many more ways to debug YAMLScript programs.
Likely many than I've even thought of yet.

See you tomorrow for Day 19 of the YAMLScript Advent Calendar.



{% include "../../santa-secrets.md" %}
