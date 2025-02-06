---
title: Going to the Library
# date: '2023-12-08'
# tags: [blog, advent-2023]
# permalink: '{{ page.filePathStem }}/'
# author:
#   name: Ingy döt Net
#   url: /about/#ingydotnet
---

Programming in YAMLScript is as easy as reading a book.
The only thing you really need is some good books!
For that let's go to the library.

By books of course I mean YAMLScript functions.
And by library I mean the YAMLScript Standard Library!
There you'll find all-time best sellers like `map`, `filter`, `reduce` and
`say`.

And Standard isn't the only Library in town...


### Welcome to Day 8 of the YAMLScript Advent Calendar

In our YS programming so far, we've been using various functions like `say`,
`take`, `keys`, `join` and `map`.
Also we've been using various operators like `+`, `*`, `=>` and `..`.
Operators are just functions with names made out of punctuation characters.

Where do these functions come from?
Are they built into the YAMLScript language?

Nope. They all come from the Library!

<details><summary>
<strong style="color:brown">Libraries and Namespaces</strong></summary>

It's probably a good idea to explain what a Clojure library is.
And we should probably talk about namespaces too.
Don't worry, it's quite simple.

A library is a file that contains a namespace and a bunch of functions (who
belong to that library/namespace).
The namespace name is made up of 2 or more words separated by the `.` character.
The name corresponds to the file path of the library.

For example (in Clojure) the `foo.bar` library would contain a `foo.bar`
namespace and be located at `foo/bar.clj` in your Java classpath.
To access a function called `thinger` in `foo.bar`, you would use the fully
qualified name (aka a Clojure symbol) `foo.bar/thinger`.

YAMLScript is the same except:

* File names end with `.ys`
* The 2 or more words are separated by `::` instead of `.`
* A fully qualified symbol looks like `foo::bar.thinger`.
  * You'll find out later why this is really cool

</details>

The YAMLScript runtime has several libraries that are always available.
The two primary ones are `clojure::core` and `ys::std`.

The `clojure::core` library is what YAMLScript calls Clojure's famous
`clojure.core` library.
In Clojure (and thus YAMLScript!) `clojure.core` is the heart and soul of the
language.
It's where all the Good Parts live.
All of the functions (and macros) that you use constantly in your day-to-day
programming.

How many publicly accessible functions does `clojure::core` give you?
581!!

How do I know that?
YAMLScript just told me so:

```bash
$ ys -pe '->(clojure::core quote find-ns ns-publics count)'
581
```

That's a lot of functions!

<details><summary><strong style="color:red">Why not 671?</strong></summary>

Honestly it's not as many as the real `clojure.core` library called from
Clojure.

```bash
$ clojure -M -e '(-> clojure.core quote ns-publics count)'
671
```

The reason for this is because YAMLScript uses a special version of Clojure
the [SCI](https://github.com/babashka/sci) (Small Clojure Interpreter).

SCI offers a subset of Clojure's functionality, but it's a very useful subset.
It's also a "safe" subset.

I don't think you'll run into anything that's not available in your day to day
YAMLScripting.
If you do, give me a ring and we'll see what we can do to get it added.
</details>

The `clojure.core` library is very well documented so we won't talk about it
more right now.

Let's discuss the other one I mentioned, `ys::std`.
This is the YAMLScript Standard Library.
It's where you'll find functions that make YS nicer to work with.
The `say` function that shows up constantly is defined as `ys::std.say`.

How many functions does it offer?

```bash
$ YS -pe '->(ys::std quote find-ns ns-publics count)'
17
```

Not that many yet, but it is still being actively defined.

Here's a few of them:

* `say` - A shorter way to say `println`
* `warn` - Like `say` but prints to stderr
* `load` - Load an external YAMLScript file
* `=>` - `=>: 123` or `(=> 123)` compiles to `123`
* `pretty` - Pretty formats a data structure (without printing it)
* `toInt`, `toStr`, `toBool` etc casting functions (Integer, String, Boolean)
* `_+` and `_*` - The polymorphic versions of `+` and `*` infix operators

The `ys::std` library will certainly grow over time.
Functions is this library use names that are not used by `clojure.core`
functions.

### Other `ys::` Libraries

There are currently several other `ys::` libraries that are always available.
Here are a few of them:

* `ys::yaml` - YAML `load` and `dump` functions
* `ys::json` - JSON `load` and `dump` functions
* `ys::ys` - `compile`, `eval` and `load` YS from within YS!
* `ys::clj` - Clojure core functions replaced by YAMLScript ones
* `ys::taptest` - A TAP-based testing library

For fun let's write a silly YS program that uses them.

Let's call it `silly.ys`:

```yaml
--8<-- "sample/advent/silly.ys"
```

Now let's run it a few times:

```bash
$ for x in {1..10}; do ys silly.ys; done
Happy Holidays, Kids!!
Hello, World!!
Happy New Year, World!!
Hello, Rudolph!!
Salutations, Santa!!
Salutations, Mrs. Claus!!
Happy Hanukkah, Mrs. Claus!!
Merry Christmas, Santa!!
Ho Ho Ho, Rudolph!!
Hello, Elves!!
```

That's a lot of fun!
We wrote a program in YAML to play with some YAML that was inside the YAML!

I hope you're starting to see the power of YAMLScript, and I hope you have a
wonderful day.

Full disclosure: I'm stuck in the Winnipeg airport, writing this and hacking on
YAMLScript because my flight's toilets stopped working and they decided to land
here for the night. True story.

Tune in tomorrow for Day 9 of the YAMLScript Advent Calendar.
