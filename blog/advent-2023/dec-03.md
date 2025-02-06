---
title: Load em Up!
# date: '2023-12-03'
# tags: [blog, advent-2023]
# permalink: '{{ page.filePathStem }}/'
# author:
#   name: Ingy döt Net
#   url: /about/#ingydotnet
---


> On the 3rd day of Advent, my YS gave to me...  
A sequence in a map tree!

Did you know that all JSON _is_ YAML?
You should, because I told you that [yesterday](dec-02.md)!

It's true.
YAML is a superset of JSON.
Both in terms of syntax and data model.

This means that any possible valid JSON input is also valid as a YAML input.
A proper YAML loader and a JSON loader should produce the same data structure
from the same JSON input.

> Assuming a YAML 1.2 loader using the YAML 1.2 JSON Schema


### Welcome to day 3 of YAMLScript Advent 2023!

This YAML/JSON relationship has some interesting implications for people
interacting with systems that read YAML or JSON as input.

People often prefer to read or write data in YAML because it's more
human-friendly with its structured indentation, lack of excessive punctuation
and its support for comments.
JSON is more machine-friendly due to its simplicity and robust tooling, thus
often prererable for machine-to-machine communication.

It's quite common to see people configure their systems that have JSON inputs by
using YAML instead, and setting things up to convert the hand maintained YAML to
JSON before the system sees it.
It can also be helpful to format large JSON API responses as YAML so that they
are easier to read.

Going the other way, people can refactor large YAML configurations by first
converting them to JSON, using JSON tools like `jq` to manipulate the data and
then converting the data back to YAML.

YAMLScript is an ideal technology for performing these kinds of conversions and
manipulations.


### Loading vs Running YAMLScript

On one hand, YAMLScript is a complete programming language that you can use for
writing new applications (and libraries).
In YAMLScript jargon, we "run" these applications.
We'll discuss YAMLScript apps and "running" them extensively in the coming days.

Today we are covering YAMLScript's purpose we just described: reading YAML
files into data; possibly transforming the data dynamically along the way.

This use of YAMLScript is called "loading" and can be done on most existing YAML
files and all existing JSON files.

<details><summary>What does "most" mean?</summary>

When I say "most YAML input files" I mean YAML input that fits into the JSON
data model.
Almost all YAML files used for configuration purposes fall into this category.

Specifically:

* Mapping keys must be strings.
* Aliases may not create circular references.
* Custom tags (those beyond the YAML 1.2 JSON Schema) must not be used.

I've never seen any of those things used in a configuration file.

---
</details>

Let's look at an example.

YAML uses the term "load" to refer to the process of a computer program
converting a YAML text into a data structure in memory.
It is common for a YAML framework to have a `load` (and `dump`) function or
method as its primary API.

Here's a Python single line program that loads a YAML text into a Python
dictionary and and then prints it.

```bash
$ python -c 'import yaml; print(yaml.safe_load("Advent day: 3"))'
{'Advent day': 3}
```

YAMLScript has a Python binding that does the same thing:

```bash
$ python -c 'from yamlscript import YAMLScript; print(YAMLScript().load("Advent day: 3"))'
{'Advent day': 3}
```

It also has these bindings in many other programming languages and plans to have
them in all modern programming languages.

A major goal of the YAMLScript project is to have these YAMLScript binding
libraries be the best way to load YAML config files in all modern programming
languages.
This will be a big improvement over the current situation where every YAML
framework has its own API and its own set of bugs and quirks.


### Loading YAMLScript from the Command Line

We can also load YAMLScript outside of any programming language.

We can do it from the command line using the `ys` command with the `--load`
option.

```bash
$ ys -le 'Advent day: 3'
{"Advent day": 3}
```

> Note: The `-le` option is a short for `-l` (short for `--load`) and `-e`
(short for `--eval).

Since the command line doesn't have any way to store the loaded YAML (like a
programming language would as a data structure) we have to get it back as text.
By default, `ys` prints the loaded YAML as JSON.

That may seem strange; a YAML loader defaulting to JSON.
But it's not strange at all, for two reasons:

* JSON _is_ YAML. Remember?!
* JSON is the de facto Lingua Franca of inter-program communication.

Put another way, `ys` outputs YAML in its most compatible format.

In a programming language, we load YAML data into an object and pass it to some
function to do something with it.
In the CLI-as-programming-language analogy, the functions are other programs!


### Loading YAML Dynamically

The main point of today's post is to show how YAMLScript can be used as a normal
YAML loader.
But of course, YS has SuperPowers™ that other YAML loaders don't have.

> NOTE: From now on I will sometimes use "YS" as shorthand for "YAMLScript".
This is distinct from `ys` which is the command-line tool that runs/loads YS.

With great SuperPowers comes SuperResponsibility.
YS won't use its SuperPowers unless you ask it to.
You may ask it (politely) in one of these ways:

* Start the YS with the `!yamlscript/v0` tag
  * Words are commands by default
* Start the YS with the `!yamlscript/v0/data` tag
  * Words are data by default
* Use a `ys-0` shebang line like: `#!/usr/bin/env ys-0`
  * Implicitly defaults to `!yamlscript/v0`
* Use the `-e` option for YS one-liners
  * Imlicitly defaults to `!yamlscript/v0` (for one-liner convenience)

After that you are good to go!

Imagine we have YAML files containing top level mappings such as:

```yaml
# map1.yaml
reindeer:
- name: Dancer
- name: Blitzen
- name: Rudolph
  nosy: true
```

We can pull data from these files into our YAML dynamically:

```yaml
# file1.ys
!yamlscript/v0/data

key1: val1
key2: ! load('map1.yaml')
key3: val3
```

Now if we "ran" the YS nothing would happen, but when we "load" it, we get the
data we expect.

```bash
$ ys --load file1.ys
{"key1": "val1", "key2": {"reindeer": [{"name": "Dancer"}, {"name": "Blitzen"}, {"name": "Rudolph", "nosy": true}]}, "key3": "val3"}
```

Now I can explain those pesky YAML tags!
What does this YS mapping pair mean?

```yaml
say: 123
```

Well, it depends.
It could be a command to print the number 123 as text to the console.
Or it could be a mapping pair with the key `say` and the value `123`.
The starting tag tells us which it is.
The `!yamlscript/v0` tag means that we start off things in a state where plain
(unquoted) YAML scalars are code.
OTOH, with `!yamlscript/v0/data` these scalars are data like in normal YAML.

The `!` tag is how we switch back and forth (toggle) between these two states.
If you have existing YAML files and you want to use a couple of YS functions in
them, start them with `!yamlscript/v0/data` and then use `!` tags before the
functional parts.

Another way to accomplish the same result is:

```yaml
# file2.ys
!yamlscript/v0

map1 =: load('map1.yaml')

=>:
  +{
    :key1 'val1'
    :key2 map1
    :key3 'val3'
  }
```

Here everything is code, and the final expression is the data we want to load.
When you run `ys --load file2.ys` you get the same result as before.

> Note: The things starting with `:` are called keywords, and they turn into one
word strings on output.
More on keywords another day.


### Merging Mappings and Joining Sequences

I'll leave you with the two most frequent requests that Santa gets from YAML
kids: `merge` and `concat`.

```yaml
# file3.ys
!yamlscript/v0/data

my-map: !
  merge:
    load('map1.yaml')
    load('map2.yaml')
my-seq: !
  concat:
    load('seq1.yaml')
    load('seq2.yaml')
```

When we were inventing YAML 20 years ago, one of the most confusing things we
did was to suggest that `<<` used as a key could trigger a merge operation.

It wasn't a good idea because:

* It's the only dynamic thing we put in YAML
* It actually wasn't in the spec proper
* It's not well defined at all
* Many YAML frameworks don't support it at all
* The ones that do it, all do it differently

But people love it, and they want more!

Well... YS gives you more.
100s more (standard functions) in fact!
(All in good time, my patient Advent-urers.)

Let's end this day by making that last YS file even cooler than Rudolph's toes!

```yaml
!yamlscript/v0/data

my-map: ! load('map1.yaml') + load('map2.yaml')
my-seq: ! load('seq1.yaml') + load('seq2.yaml')
```

YAMLScript's `+` operator is a general purpose joiner.
It's polymorphic for numbers, strings, sequences and mappings.

As ususal, I hope you enjoyed today's post.
I'll see you tomorrow for day 4 of YAMLScript Advent 2023!
