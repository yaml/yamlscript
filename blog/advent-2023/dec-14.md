---
title: Stocking Stuffers
# date: '2023-12-14'
# tags: [blog, advent-2023]
# permalink: '{{ page.filePathStem }}/'
# author:
#   name: Ingy döt Net
#   url: /about/#ingydotnet
---

It's always nice to get a little something extra in your stocking whilst waiting
for the big guy to show up on the big day.

Learning eveything you need to know about YAMLScript in 24 days is a tall order.
I still have a quite a bit to learn about it myself! :-)

It helps to learn the small stuff first.

### Welcome to Day 14 of YAMLScript Advent 2023!

Today we'll look at some of the little things that make YAMLScript a joy to work
with.
We'll go over some of the basic syntax, semantics, features and commands of
YAMLScript.

What if...
What if?
What `if`!

Let's start out by looking at some of the various ways you can write the
ubiquituous `if` statement in YAMLScript.

To set the tone here, let's code an `if` in another simple language, Python,
since it can often be used as pseudocode:

```python
if a > b:
  print(a + " is greater than " + b)
else:
  print(b + " is greater than or equal to " + a)
```

Abstractly we are saying that if some condition is true, then do one thing,
otherwise do another thing.

Since YAMLScript compiles to Clojure, it's important to understand how Clojure
does `if`.
Let's look at the same code in Clojure.

```clojure
(if (> a b)
  (println (str a " is greater than " b))
  (println (str b " is greater than or equal to " a)))
```

The Clojure code has an `if` command followed by:
* a condition form
* a form to run if the condition is true
* a form to run if the condition is false (optional)

This is how all `if` statements look in Clojure.

There are many ways to do the same thing in YAMLScript.
Here's a one good way to do it:

```yaml
!yamlscript/v0
if a > b:
  say: "$a is greater than $b"
  say: "$b is greater than or equal to $a"
```

> All the YAMLScript examples in this post use code-mode which requires the
`!yamlscript/v0` tag at the top of the file.
We'll leave it out of the rest of the examples for brevity.

Let's see what happens when we compile that YAMLScript program:

```bash
$ ys -c if.ys
(if
 (> a b)
 (say (str a " is greater than " b))
 (say (str b " is greater than or equal to " a)))
```

We get the exact same Clojure code as above! (in a slightly different format)
Note that `say` is an alternate way to write `println` in YAMLScript, but you
can use `println` if you prefer to type more.

The `if` command is an interesting YAML mapping.
The key has both the `if` command symbol and the condition form in it.
The value is another YAML mapping with two pairs: one for the "then" form and
one for the "else" form.

> Notice that both pairs have the same key, `say`.
Duplicate keys aren't allowed in YAML, but they are allowed in YAMLScript
code-mode.
That's because the YAMLScript compiler (a fancy YAML loader) isn't loading the
mapping into a data structure, but rather into an AST.
For the AST, the "duplicate keys" are no problem, because they're not actually
being used as mapping keys.
By contrast, in YAMLScript data-mode, duplicate keys are not allowed because
they are actually being used as mapping keys (like normal YAML).

YAMLScript code-mode does everything using only YAML block mappings or scalars.
Using sequences or flow nodes is not allowed in code-mode.

<details><summary><strong style="color:green">Quick YAML Jargon Review</strong></summary>

----

* node - a mapping, sequence or scalar
* collection - a mapping or sequence data structure
* mapping - a key/value data structure; aka hash, dictionary, object,
* sequence - an ordered list of values; aka array, list, vector
* scalar - a single value; aka string, number, boolean, null
* block - the normal YAML indented style (with `-` before each sequence node)
* flow - the JSON looking style with braces and brackets
* plain scalar - a scalar that is not quoted
* double quoted scalar - a scalar that is quoted with double quotes
* single quoted scalar - a scalar that is quoted with single quotes
* literal scalar - a scalar that is quoted with `|`
* folded scalar - a scalar that is quoted with `>`
* pair - a key/value pair in a mapping
* key - the first part of a pair or lefthand side
* value - the second part of a pair or righthand side

> YAML block mapping values can span multiple lines, and can start after the
line with the key and the `:` separator.

----

</details>

Let's write the `if` statement using YAML scalars instead of a mapping:

```yaml
if a > b:
  say("$a is greater than $b")
  say("$b is greater than or equal to $a")
```

If you `ys --compile` this you get the exact same thing as the previous compile.
In this case the value of the top YAML mapping is just a scalar.
Even though it is 2 lines of code, it represents a a single long line containing
two function calls, one for "then" and one for "else".

So that's 2 ways to write the `if` statement in YAMLScript.
The two best ways.
There are several other ways to do it that are less readable but still valid
syntax.
And there's nothing special about this being an `if` statement; the same rules
apply to all YAMLScript commands in general.


### General Syntax Rules

By default in code-mode, a mapping is a set of pairs where each pair compiles to
a form:

```yaml
a: b c d
a b: c d
a b c: d
a b c d:
=>: (a b c d)
=>: a(b c d)
a:
  =>: b
  =>: c
  =>: d
a b:
  =>: c
  =>: d
```

> ***This is important to understand!***

All the pairs above compile to the same Clojure form: `(a b c d)`, which is
calling a function `a` with 3 arguments: `b`, `c` and `d`.

The `=>` symbol is a special YAMLScript key symbol that can be used when you
only care about using a scalar in that spot.
Since YAMLScript always needs to be valid YAML, this can often be useful.

So now you can see what I was talking about with all the ways to write an `if`
statement.
Only the `a b: c d` forms look normal to most programmers.


### YAMLScript Loops

Let's look at another common control structure: looping.

There are a lot of ways to do loops in Clojure (thus YAMLScript).
They have lots of subtle differences and properties.
We'll just cover one or two here for now.
(We're not even halfway through December yet!)

Here's a simple `for` loop in Python that prints the numbers 0 through 4:

```python
for i in range(5):
  print(i)
```

Here's the same loop in Clojure:

```clojure
(for [i (range 5)]
  (println i))
```

So in YAMLScript we can do this:

```yaml
for i range(5):
  say: i
```

When we compile this we get:

```bash
$ ys -c for.ys
(for [i (range 5)] (say i))
```

Let's run it:

```bash
$ ys for.ys
$
```

Nothing happened!

This has to do with some of the more advanced understanding of Clojure.
Specifically laziness and evaluation.

To be honest, the Clojure code I wrote above would not print anything depending
on how you ran it.
If you ran it in the Clojure REPL or as a one liner it would print the numbers.
If you ran it as a program it would not print anything.

To make things work as expected in the REPL Clojure forces the lazy structures
to be evaluated.

Clojure also has a `doall` function that forces evaluation.
Let's use that in our YAMLScript program:

```yaml
doall:
  for i range(5):
    say: i
```

Now when we compile and run it we get:

```bash
$ ys for.ys
0
1
2
3
4
```

Success!
But at what cost?
That's a lot of code to print 5 numbers.

YAMLScript has a nicer way to do this.
It has an `each` command that calls `doall` and `for` for you:

```yaml
each i range(5):
  say: i
```

or:

```yaml
each i range(5): say(i)
```

or:

```yaml
each i (0 .. 4): say(i)
```

Doesn't it seem strange to you that `(range 5)` really means 0 to 4?
There are technical reasons for this, but it's not very intuitive.

YAMLScript's `..` operator does what you told it to.
It really just compiles to `(ys.std/rng 0 4)` which is a standard YAMLScript
function that returns a range from 0 to 4.

It also works in reverse:

```bash
$ ys -e 'each i (4 .. 0): say(i)'
4
3
2
1
0
```

To do that in Clojure you'd have to write:

```clojure
(doall
  (for [i (range 4 -1 -1)]
    (println i)))
```

Sheesh!

----

I know that was only 2 or 3 things in your stocking, but they were quite a lot
to chew on.
Maybe we'll have to stuff your stocking a couple more times this month!

See you tomorrow for Day 15 of YAMLScript Advent 2023!
