---
title: YS Syntax Modes
talk: 0
---

One of the most important things to understand when learning YS is the concept
of "modes".

It basically comes down to whether an unquoted scalar like `count` should be
considered as a data string or a code symbol (variable, function name etc).
Since the main focus of YS is about embedding code into YAML data files,
it's very important to know what mode you are in at any given point.

YS has 4 modes:

* Bare mode

  Exactly the same as YAML 1.2 (Core Schema).
  YS can load most existing files without executing any code.

* Data mode

  Very similar to bare mode, but allows you to switch to code mode.
  All YAML syntax forms are allowed here.

* Code mode

  Plain (unquoted) scalars are treated as code expressions.
  YAML's flow mappings (`{}`), flow sequences (`[]`) and block sequences (`-`)
  are not allowed in code mode.
  YS "code" is  written using block mappings (`k: v`), plain scalars,
  quoted scalars (single and double) and literal (`|`) scalars.
  Folded scalars (`>`) are also disallowed in code mode.

* Code-value mode

  Mapping keys and collection structure are data, while scalar values and
  sequence elements are code.
  This mode is only entered on a mapping pair value with `:::`.

The most important ones to learn about are data and code modes.
To use YS effectively you'll need to be comfortable with switching back and
forth between the two.

Bare mode is the default when you haven't added a `!ys-0` tag to the start of
a YS document.
It means that everything in the file is data; code can never be used.
This is how we can make sure that existing YAML files are valid YS.

To enable a YAML file to use YS code, you need to add one of these tags to the
top:

* `!ys-0` - Start in code mode.
* `!ys-0:` - Start in data mode.

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
!ys-0:
foo:
  count: [red, green, blue]')
{"foo":{"count":["red","green","blue"]}}
```

Code mode:

```txt
$ ys --load <(echo '
!ys-0
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
!ys-0:
foo: !
  count: [red, green, blue]')
Error: Sequences (block and flow) not allowed in code mode
```

Here we started in data mode but then switched the mode to code with `!`.
We got the same error.
YS only allows block mappings for code.
We need to put `[red, green, blue]` into data mode:

```txt
$ ys --load <(echo '
!ys-0:
foo: !
  count: ! [red, green, blue]')
{"foo":3}
```

It worked!

Using `!` is so common that YS has a cleaner way to do it when used on a
mapping pair value.
If you use `::` instead of `:` it does the same thing.

Let's try it out:

```txt
$ ys --load <(echo '
!ys-0:
foo::
  count:: [red, green, blue]')
{"foo":3}
```

Sweet!

However, when *switching in a sequence* you'll need to use `!`:

```txt
$ ys --load <(echo '
!ys-0:
- !
  count:: [red, green, blue]')
[3]
```

NOTE: `::` isn't special YAML syntax.
YS cannot change YAML 1.2 syntax in any way.
In the examples above `count:` is simply a plain scalar ending with `:`.

We can see that in bare mode:

```txt
$ ys --load <(echo '
count:: [red, green, blue]')
{"count:":["red","green","blue"]}
```


## Code Values in Data Collections

Use `:::` when a mapping or sequence contains mostly computed values.
It keeps the collection structure and mapping keys in data mode while treating
each scalar value and sequence element as code.

```yaml
!ys-0
x =: 6
result =:::
  answer: x * 7
  nested:
    next: inc(x)
  items:
  - x
  - x + 1
```

This produces:

```yaml
answer: 42
nested:
  next: 7
items:
- 6
- 7
```

Code-value mode recurses through block and flow mappings and sequences.
It can be entered from code mode, data mode, or another code-value collection.
The value after `:::` must be a mapping or sequence.

Use `::` on a mapping pair to make its entire value subtree data again.
Use `!` for the same override on a sequence element.
An inner `::` can switch from that data subtree back to code as usual.

```yaml
result =:::
  computed: inc(x)
  config::
    literal: inc(x)
    computed:: inc(x)
  items:
  - inc(x)
  - ! inc(x)
```

An unmarked mapping in code-value mode is always a structural mapping.
Write a block-form code value as a scalar call to a helper function instead.

Use `:::` when most values in a collection are computed.
For a mostly literal collection with only one or two computed values, ordinary
data mode with `::` on those values is often clearer.
