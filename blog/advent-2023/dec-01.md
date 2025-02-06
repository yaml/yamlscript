---
title: YAMLScript Advent 2023
# date: '2023-12-01'
# tags: [blog, advent-2023]
# permalink: '{{ page.filePathStem }}/'
# author:
#   name: Ingy döt Net
#   url: /about/#ingydotnet
---

### Seasons Greetings, my YAML friends!

What if I told you that you could write a **program in YAML** that would
generate a Christmas tree?

Well, you can! Here's how:

```yaml
--8<-- "sample/advent/tree.ys"
```

Let's get a tree!

```bash
$ ys tree.ys
     *
    ***
   *****
  *******
 *********
     *
     *
```

That's a pretty nice tree, but it's a little small. Let's make it bigger!

```bash
$ ys tree.ys 10
          *
         ***
        *****
       *******
      *********
     ***********
    *************
   ***************
  *****************
 *******************
          *
          *
```

Welcome to the first day of YAMLScript Advent 2023!
We're going to be writing a lot of YAMLScript this month, so let's get started…

Wait! What is YAMLScript?

YAMLScript is a new programming language that uses YAML as its syntax.
You can do anything in YAMLScript that you can do in a language like
Python or JavaScript such as:

* Defining functions
* Using variables
* String manipulation and interpolation
* Loops and conditionals
* Importing modules
* And more!

YAMLScript looks and feels like an imperative programming language, but it's
actually a functional programming language.
This means that YAMLScript programs are made up of expressions that are
evaluated to produce a result.

But why would you even want to write a program in YAML?

YAMLScript is a full-featured, general purpose programming language, but it's
also designed to be a great language for writing YAML configuration files.
To that point, almost all YAML files are valid YAMLScript programs!
And they evaluate to the same result that a YAML loader would produce.

For example, here's a YAML file that defines a list of fruits:

```yaml
--8<-- "sample/advent/fruits.yaml"
```

Let's run this file as a YAMLScript program:
```bash
$ ys fruits.yaml
$
```

Nothing happens!

But why would anything happen? The program doesn't do anything!

It's the same as running this Python program:
```python
$ python -c '["apple", "banana", "cherry"]'
$
```

To obtain the evaluation result of a YAMLScript program, we need to use the
`--load` option:
```bash
$ ys --load fruits.yaml
["apple", "banana", "cherry"]
$
```

We got some JSON back!
That's because by default, `--load` evaluates the YAMLScript and prints the
result as JSON.

What if we want to include these fruits in our YAML grocery list?
Let's try it:

```yaml
# grocery.yaml
- bread
- fruits: load('fruits.yaml')
- milk
```

Let's add the `--yaml` option to print the result as YAML:

```bash
$ ys --load grocery.yaml --yaml
- bread
- fruits: load('fruits.yaml')
- milk
$
```

That's not what we wanted!
We wanted the contents of the fruits list to be included in the grocery list.

But if you think about it, this is exactly what we asked for.
Since every YAML file is a valid YAMLScript program, it certainly should be
loaded just like any other YAML loader would do it.

Let's fix this to do what we want:

```yaml
--8<-- "sample/advent/grocery.yaml"
```

Now when we run it:

```bash
$ ys -l -Y grocery.yaml
- bread
- fruits:
  - apple
  - banana
  - cherry
- milk
$
```

There we go! We got our fruits!

So what did we do here?
We added 2 things:
* A `!yamlscript/v0/data` tag at the top
* A `!` tag before the `load` function call

We won't get into the details of what these tags mean today, but you'll learn
about them soon enough.


### YAMLScript Advent 2023 Teasers

My name is [Ingy döt Net](https://github.com/ingydotnet).
I'm one of the original creators of the [YAML data language](
https://yaml.org/) and I lead the [YAML Language Development Team](
https://yaml.org/spec/1.2.2/ext/team/).

I've been working on YAMLScript for about a year now, and I'm excited to finally
share it with you.
I believe that YAMLScript is going to take YAML to exciting new places, while
remedying many of its shortcomings, limitations and frustrations.
I hope you'll come to agree as you join me on this holiday season unwrapping of
the gift that is YAMLScript!

I also hope that you enjoyed this first day of the **YAMLScript Advent 2023**!
I'll be posting a new blog article every day this month, so stay tuned!
Well at least until December 25th, but I might keep going after that. :-)

Here's a sneak peek of some of the things to come:

* Installing and using `ys` — the YAMLScript interpreter
* The history of YAMLScript
* How YAMLScript is compiled and evaluated
* How YAMLScript can fix many of YAML's problems
* How to use YAMLScript like a YAML loader in any programming language
* Is YAMLScript actually a Lisp???
* Refactoring complicated YAML configurations with YAMLScript
* Writing polyglot libraries in YAMLScript
* What makes a YAML file a valid (or invalid) YAMLScript program?
* Compiling YAMLScript to native binaries and shared libraries

Hopefully you're as excited as I am to learn more about YAMLScript!

See you tomorrow!
