---
title: Santa's Big Secret
date: '2023-12-04'
tags: [blog, advent-2023]
permalink: '{{ page.filePathStem }}/'
author:
  name: Ingy döt Net
  url: /about/#ingydotnet
---

I've got a little secret to tell you.
I've been hiding a little something from you.
Even that's a lie.
I've actually been hiding something very very big something from you.

> ### \*\*\* YAMLScript is a Lisp! \*\*\***

### Welcome to day 4 of YAMLScript Advent 2023!

Today is Monday and Monday's are made for big headlines.

That's right.
Not only is YAMLScript a Lisp, it's a very specific and modern Lisp.
It's a Lisp that has a community and conferences and books and jobs that pay
people to write code in Lisp!

If you know the slightest thing about Lisp, you must think I'm crazy.
YAMLScript is YAML, and YAML is no Lisp.
It's almost the Anti-Lisp incarnate.

OK. Here's the deal...

> ### YAMLScript _is_ Clojure

There you go. The secret is out.
Every YAMLScript program is program is a Clojure program.

Every time you run (or load!) a YAMLScript program, it compiles to Clojure code
that is then run by a Clojure runtime engine.
I can prove it!

Consider this YAMLScript program:

```yaml
# hw.ys
!yamlscript/v0
println: 'Hello, world!'
```

Let's run it:

```bash
$ ys --run hw.ys
Hello, world!
```

No surprises there.
Now let's first compile it to Clojure:

```bash
$ ys --compile hw.ys
(println "Hello, world!")
```

Looks pretty Lispy to me.
Now let's run the Clojure code:

```bash
$ ys -c hw.ys | clojure -M -
Hello, world!
```

It works!
YAMLScript really _is_ Clojure.
And Clojure most definitely _is_ a Lisp.
Soooooo...?!
Yeah, you get it. QED, baby!


### What is Clojure?

Oh wait, you don't know what Clojure is?
Or maybe you need a quick refresher?

Clojure is a modern Lisp that runs on the JVM.
It was created by this really interesting guy named [Rich Hickey](
https://en.wikipedia.org/wiki/Rich_Hickey) whom I've actually met many times...

...on YouTube.

I don't typically watch a lot of programming videos, but I've seen at least a
dozen of his.
I encourage you to watch some too.
Or at least peruse some of his [various opinions on varying programming topics](
https://gist.github.com/reborg/dc8b0c96c397a56668905e2767fd697f).

Rich programmed professionally in Java for many years.
One day he decided that he couldn't take it anymore.
He took a couple years off and he changed the world.
The Java world anyway.

Java is a widely used imperative programming language, with mutable data
structures and a noisy syntax.
Clojure by contrast is a functional programming language, with immutable data
structures and a very clean syntax.
You can use any Java library directly from Clojure and vice versa, making
Clojure an extremely practical language.

In a phrase: **"Clojure makes Java not suck"**. (Ingy's words, not Rich's)


### What does this all have to say about YAMLScript?

**Yesterday we learned that all JSON is YAML and that almost all YAML is
YAMLScript.
Does this means that all JSON is Clojure?**

Actually it kind of does.

Let's compile some JSON with `ys`:

```bash
$ ys -md -ce '{ "foo": "bar", "baz": [1, 2, null, true] }'
{"foo" "bar", "baz" [1 2 nil true]}
```

Yep. That's Clojure.
It also happens to be EDN, which is Clojure's native data format.

> Note: The `-md` option tells `ys` not to add the `!yamlscript/v0` tag that it
usually does with `-e` to make your YS life easier.
We'll learn more about `-m` another time.

**Lisp puts parentheses around everything.
Does that mean that YAMLScript does too?**

Good question. The answer may surprise you.
YAMLScript has a lot of different ways to express code.
It embraces diversity. (As long as that diversity can be written as YAML!)
One of the ways to write code in YAMLScript is in Clojure syntax!

This YAMLScript prints 3 symbol names available in the current namespace:

```bash
$ ys -e '(say (take (+ 1 2) (keys (ns-map NS))))'
(+' decimal? sort-by)
```

In Lisp every expression (function call) is a paranthesized list where the first
word is the function name and the rest are the arguments.
That means arithmetic expressions like `1 + 2` are written as `(+ 1 2)`.

This feels very natural to Lisp programmers, but it can be a bit of a shock to
the rest of us.
YAMLScript offers alternate ways (called ysexprs or YeS Expressions) to write
these Lisp basic forms.

We'll learn the gritty details in another post, but here's the basics:

```yaml
=>: 1 + 2               # (+ 1 2)   ;; + - * /
=>: 3 * 4 * 5           # (* 3 4 5) ;; if operators are the same
=>: foo(bar(42 true))   # (foo (bar 42 true))
```

We could write the above YAMLScript expression like this:

```yaml
say:
  take:
    +: 1 2
    keys:
      ns-map: NS
```

Both YAMLScript forms compile to the same Clojure code.

Basically at any level of YAML in YAMLScript, you can switch to using Clojure.
Since an entire YAML document can be a single string you can sometimes use an
entire Clojure file as a YAMLScript program.
As long as it's valid YAML, of course.


**Clojure is a JVM Language.
Does that mean that YAMLScript is a JVM language?**

This one is crazy.
YAMLScript does not need the JVM or anything Java whatsoever.
The `ys` binary is a native machine code executable.
The `libyamlscript` shared library is also native machine code and thus can be
FFI bound to almost any programming language.

You don't even need to have Java installed on your system to *build* YAMLScript.
That's a little fib.
The build system always downloads a Java build system and then discards it when
it's done.
The point is that to build `ys` you don't need to set up any prerequisites.
It just works.

How is this possible?
It's all thanks to [GraalVM](https://www.graalvm.org/) which is, as one of my YS
friends puts it, "a cheat code"!
GraalVM's `native-image` compiler can magically turn anything Java into native
machine code on Linux, macOS and Windows.
**Wow!**

A very noticeable difference between YAMLScript and Clojure is startup time:

```bash
$ time ys -e 1
real    0m0.044s
$ time perl -e 1    # for comparison
real    0m0.048s
$ time clojure -M -e 1
1
real    0m0.637s
```

Clojure's not 10 times slower than YS (or Perl).
It just takes 10 times longer to start up a JVM.


**Do you need to know Clojure to use YAMLScript?**

No, not at first.
Proof?
I just got through 3 days of YAMLScript Advent without mentioning Clojure once.

If you just want to make your out of control YAML files more manageable,
composable and maintainable, you can easily learn how to wield YAMLScript
without knowing a lick of Clojure.

When you need more power it's there for you because Clojure is there for you.
But you have to learn some new things first.

I honestly think YS can be a great introduction to Clojure.
I think that Clojure is a great introduction to Lisp, working with immutable
data structures, and functional programming.
And I think that learning these things will make you a better programmer in
whatever language you use.

YAML has always been about making things easier in all programming languages.
My love for Clojure is that it has the right parts to make YAML more powerful
in all those same languages.
I have no desire to see the whole world switch to Clojure (or anything else).
Clojure is a great gift and I hope YS can help more people benefit from it in
the languages and technologies they already use.


**How does YAMLScript benefit from building over Clojure?**

In a nutshell, it makes YAMLScript a complete, robust, battle tested, and
well-documented language from the get go.
Rich knew that to make a new language in 2006 he needed to build on something
that was already a big deal; the Java ecosystem.

I feel the exact same way about YAMLScript.

YAMLScript is poised to take YAML to a whole new level.
This is all thanks to the shoulders of these specific giants:

* [YAML](https://yaml.org/)
* [Clojure](https://clojure.org/)
* [SnakeYAML](https://bitbucket.org/asomov/snakeyaml/src/master/)
* [Small Clojure Interpreter (SCI)](https://github.com/babashka/sci)
* [GraalVM](https://www.graalvm.org/)

I'll have more to say about each of these in future posts.
For now, I'll just say that I'm extremely grateful for all of them.

I'll see you tomorrow for day 5 of YAMLScript Advent 2023!
