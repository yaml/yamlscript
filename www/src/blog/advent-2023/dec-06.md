---
title: Modes of Transportation
date: '2023-12-06'
tags: [blog, advent-2023]
permalink: '{{ page.filePathStem }}/'
author:
  name: Ingy döt Net
  url: /about/#ingydotnet
---

How do you get around?
Some people walk, some ride bikes, some drive cars (or the cars drive them),
some take trains, some in planes, so many ways, even some in sleighs.

In YAMLScript, data gets around via various modes of transportation...
3 modes to be exact.

Rememeber back on December 3rd when we talked about the 2 different states that
a YAMLScript program can be in?
In one state `say` is a function, and in the other it's just a plain string.

We call these states "modes", and there is actually three of them.


### Welcome to Day 6 of the YAMLScript Advent Calendar

YAMLScript has these 3 modes:

* Code Mode

  A starting `!yamlscript/v0` tag puts the YS file into Code Mode.
  Unquoted strings are code expressions which are further parsed into AST
  forms.
  A `!` tag can switch the mode to Data Mode.

* Data Mode

  A starting `!yamlscript/v0/data` tag puts the YS file into Data Mode.
  Everything is the regular YAML data language that you are used to.
  But a `!` tag can switch the mode to Code Mode.

* Bare Mode

  Without a magic starting `!yamlscript` tag, the YS file is in Bare Mode.
  This is like Data Mode but you aren't allowed to ever switch to Code Mode.
  This is the default mode for YAMLScript files, and the reason we can claim
  that almost all existing YAML files are valid YAMLScript files.
  _Specifically all those that adhere to the JSON data model, which is almost
  all YAML config files._

The `ys` CLI tool will implicitly add a `!yamlscript/v0` tag when you use the
`--eval` (aka `-e`) option, unless you actually provide a tag yourself.
This is simply for convenience when you are testing out code snippets.

Note that you can use multiple `-e` options and each one acts like a separate
line of code in a file.

So `ys -e 'say: "Hello"' -e 'say: "World"'` is the same as this YS program:

```yaml
!yamlscript/v0
say: "Hello"
say: "World"
```

If you wanted to write a `ys` one-liner that used Data Mode, you could do this:
`ys --load -e '!yamlscript/v0/data' -e 'foo: 111' -e 'bar: 222'` which is the
same as this YS program:

```yaml
!yamlscript/v0/data
foo: 111
bar: 222
```

If you wanted to write a `ys` one-liner that used Bare Mode, you could do this:
`ys --load -e `!yamlscript/v0/bare` -e 'foo: 111' -e 'bar: 222'` which is the
same as the Data Mode example above.

There's actually a better way to write Data and Bare Mode one-liners.
The `ys` command has a `--mode` (aka `-m`) option that lets you set the mode to
`code` (`c`), `data` (`d`) or `bare` (`b`).

Thus the last one liner could be written as:
`ys -mb -l -e 'foo: 111' -e 'bar: 222'`.

> Note: The `-m` option only works with the `-e` option.
You can't use it to change the mode of a file that you are loading or running.


### Switching Modes

In YAML tags are words that start with `!`.
They are instructions to the YAML loader (specifically to the constructor phase)
about what exactly to construct.

It's quite rare to see a YAML tag in the wild.
But here's something you probably didn't know...
Every untagged node in an internal YAML loader tree is implicitly assigned a
tag.
This process (of a YAML loader) is called "tag resolution".
This is how the unquoted string `123` becomes the integer `123`.
It is implicitly tagged with `!!int`, which happens to be shorthand for
`tag:yaml.org,2002:int`.

All YAML loaders understand the set of `yaml.org,2002` tags: `!!map`, `!!seq`,
`!!str`, etc.
In fact you are free to use these tags in YAMLScript programs, even in Bare
Mode.
But there is really no good reason to do so.

The `!` tag is a valid YAML tag, but it is special in YAMLScript.
It switches between Code Mode and Data Mode.

If you think `!` looks weird, there is another cleaner looking way to switch
from Code Mode to Data Mode.

You can use `::` instead of `:` to separate the key and value of a mapping.

```yaml
!yamlscript/v0
my-map =::
  foo = 111
  bar = 222
```

is the same as:

```yaml
!yamlscript/v0
my-map =: !
  foo: 111
  bar: 222
```

At this point you can't do the same thing to switch from Data Mode to Code Mode.


Well that concludes our coverage of YAMLScript a la Mode!
Mmmm... Now doesn't that just sound delicious? (or at least fashionable!)

I'll see you tomorrow for day 7 of YAMLScript Advent 2023!
