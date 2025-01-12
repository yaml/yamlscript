---
title: Reindeer All The Way Down
date: '2023-12-19'
tags: [blog, advent-2023]
permalink: '{{ page.filePathStem }}/'
author:
  name: Ingy döt Net
  url: /about/#ingydotnet
---

Santa is in charge of Christmas.
He's the one who makes sure that all the children get presents.
But who is in charge of getting Santa his presents?
That's where the reindeer come in.
They are the ones who make sure that Santa gets his presents.
But who is in charge of getting the reindeer their presents?
More reindeer!
But who is in charge of getting the reindeer's reindeer their presents?
More reindeer!
It's reindeer all the way down.


### Welcome to Day 19 of the YAMLScript Advent Calendar

YAMLScript is a new programming language that is based on Clojure.
It's written in Clojure and compiles to Clojure.
In effect YAMLScript is Clojure.
So if YAMLScript is written in Clojure, why not just use YAMLScript to write
YAMLScript?

Isn't there a chicken and egg problem here?
Sure, but that's easy to solve.

Once you have a stable version of YAMLScript, you can rewrite YAMLScript in
YAMLScript.
You just need to use a previously compiled version of YAMLScript to compile
the new YAMLScript-in-YAMLScript with.

This is called self-hosting and it's a common practice in the programming
language world.
Clojure itself is written in Clojure.
Not all of it.
It's also written in Java, since it runs on the JVM.
But all the core libraries are written in Clojure.

YAMLScript isn't quite ready to be self-hosted yet.
But I was able to convert the `ys::std` library to YAMLScript and then build
YAMLScript with it!

Have a look at this [gist](
https://gist.github.com/ingydotnet/480d7243a797c9323b973cf5c5dea933).

It has 3 files:

* [orig-std.clj](
https://gist.github.com/ingydotnet/480d7243a797c9323b973cf5c5dea933#file-orig-std-clj)
  — The original Clojure version of `ys::std` as of today.
* [std.clj](
https://gist.github.com/ingydotnet/480d7243a797c9323b973cf5c5dea933#file-std-clj)
  — The compiled version of YAMLScript port of `ys::std`.
* [std.ys](
https://gist.github.com/ingydotnet/480d7243a797c9323b973cf5c5dea933#file-std-ys)
  — The YAMLScript port of `ys::std`.

I don't know about you but I find the YAMLScript version much easier to read
than the Clojure version.

However since there's still a lot of work left to do on YAMLScript, before the
first stable release, some of the YAMLScript code is a bit ugly.

Let's look at a few ugly forms and think about how to make them better.

Let's start with the `ns` declaration at the top of the file:

```yaml
ns ys::std:
  (:require
   [yamlscript::debug]
   [clojure::pprint]
   [clojure::string])
  (:refer-clojure :exclude [print])
```

Soon we'll have a YAMLScript macro system for making certain calls look better
than they do with the base syntax.
The future `ns` declaration might look like this:

```yaml
ns ys::std:
  require:
    yamlscript::debug:
    clojure::pprint: pp
    clojure::string:
  refer-clojure:
    exclude: print
```

That looks pretty nice!

How about this ugly multi-arity defn:

```yaml
defn toMap:
  +[]: +{}
  +[x]:
    apply: hash-map x
  +[k v & xs]:
    apply: hash-map k v xs
```

Here we needed to plus-escape the `[]` keys.
I'd rather see like:

```yaml
defn toMap:
  (): hash-map()
  (x):
    apply: hash-map x
  (k v *xs):
    apply: hash-map k v xs
```

Parentheses are just normal characters in YAML so we don't need to escape them.
And we already use parens in `defn foo(bar): baz` so it's consistent.

One of the more problematic forms in this file is the macro definition:

```yaml
defmacro each [bindings & body]:
  +`(do
      (doall
        (for [~@bindings] (do ~@body)))
      nil)
```

I pretty much had to leave the original Clojure syntax alone here.
The backtick is a reserved character in YAML so we had to plus-escape it.

There's actually quite a few problems that macros cause for YAMLScript.
I won't bore you with the details.
We'll figure out a good way to code macros in YAMLScript but I can't say that I
have it figured out yet.

At this point I'd say that about 80% of Clojure code ports nicely to YAMLScript.
The other 20% is a bit of a struggle.
But that's stuff I want to improve before the first stable release.

----

I hope you enjoyed today's post.
It was a little shorter than usual.
But YAMLScript is keeping me very busy and I was a little short on time today.

I have something really special planned for tomorrow.
But I need to make it work first!
Fingers crossed.

Come back tomorrow for Day 20 of the YAMLScript Advent Calendar.
