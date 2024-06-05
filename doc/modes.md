---
title: YAMLScript Syntax Modes
---

One of the most important things to understand when learning YAMLScript is the
concept of "modes".

It basically comes down to whether an unquoted scalar like `count` should be
considered as a data string or a code symbol (variable, function name etc).
Since YAMLScript's main focus is about embedding code into YAML data files,
it's very important to know what mode you are in at any given point.

YAMLScript has 3 modes:

* Bare mode

  Exactly the same as YAML 1.2 (Core Schema).
  YAMLScript can load most existing files without executing any code.

* Data mode

  Very similar to bare mode, but allows you to switch to code mode.
  All YAML syntax forms are allowed here.

* Code mode

  Plain (unquoted) scalars are treated as code expressions.
  YAML's flow mappings (`{}`), flow sequences (`[]`) and block sequences (`-`)
  are not allowed in code mode.
  YAMLScript "code" is  written using block mappings (`k: v`), plain scalars,
  quoted scalars (single and double) and literal (`|`) scalars.
  Folded scalars (`>`) are also disallowed in code mode.

The most important ones to learn about are data and code modes.
To use YAMLScript effectively you'll need to be comfortable with switching back
and forth between the two.

Bare mode is the default when you haven't added a `!yamlscript/…` tag to the
start of a YAMLScript document.
It means that everything in the file is data; code can never be used.
This is how we can make sure that existing YAML files are valid YAMLScript.

To enable a YAML file to use YAMLScript code, you need to add one of these tags
to the top:

* `!yamlscript/v0/code` - Start in code mode.
* `!yamlscript/v0/data` - Start in data mode.
* `!yamlscript/v0` - Short for `!yamlscript/v0/code`
* `!yamlscript/v0/` - Short for `!yamlscript/v0/data`

Consider the following examples.

Bare mode:

```txt
$ ys --load <(echo '
foo:
  count: [red, green, blue]')
{"foo":{"count":["red","green","blue"]}}
```

Data mode:

```txt
$ ys --load <(echo '
!yamlscript/v0/
foo:
  count: [red, green, blue]')
{"foo":{"count":["red","green","blue"]}}
```

Code mode:

```txt
$ ys --load <(echo '
!yamlscript/v0
foo:
  count: [red, green, blue]')
Error: Sequences (block and flow) not allowed in code mode
```

Oops.
Looks like we need to switch to data mode in there.


## Switching Modes

If we want to add a function to a data file we should start in data mode.
Then we should switch to code mode for things that are code.

Here we want to call the `count` function with a sequence and get back 3, the
number of elements in the sequence.

The special tag `!` can be used to switch from data to code and vice versa.

```txt
$ ys --load <(echo '
!yamlscript/v0/
foo: !
  count: [red, green, blue]')
Error: Sequences (block and flow) not allowed in code mode
```

Here we started in data mode but then switched the mode to code with `!`.
We got the same error.
YAMLScript only allows block mappings for code.
We need to put `[red, green, blue]` into data mode:

```txt
$ ys --load <(echo '
!yamlscript/v0/
foo: !
  count: ! [red, green, blue]')
{"foo":3}
```

It worked!

Using `!` is so common that YAMLScript has a cleaner way to do it when used on
a mapping pair value.
If you use `::` instead of `:` it does the same thing.

Let's try it out:

```txt
$ ys --load <(echo '
!yamlscript/v0/
foo::
  count:: [red, green, blue]')
{"foo":3}
```

Sweet!

However, when *switching in a sequence* you'll need to use `!`:

```txt
$ ys --load <(echo '
!yamlscript/v0/
- !
  count:: [red, green, blue]')
[3]
```

NOTE: `::` isn't special YAML syntax.
YAMLScript cannot change YAML 1.2 syntax in any way.
In the examples above `count:` is simply a plain scalar ending with `:`.

We can see that in bare mode:

```txt
$ ys --load <(echo '
count:: [red, green, blue]')
{"count:":["red","green","blue"]}
```
