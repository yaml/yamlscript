---
title: Flip Flops
date: '2023-12-22'
tags: [blog, advent-2023]
permalink: '{{ page.filePathStem }}/'
author:
  name: Ingy döt Net
  url: /about/#ingydotnet
---

Can you imagine Santa walking around in flip flops?
I've never been up to the North Pole, but I'm pretty sure there's no beaches.
I always pictured Santa wearing moon boots around the workshop.

YAMLScript on the other hand, is all about flip flops!


### Welcome to Day 22 of the YAMLScript Advent Blog!

Remember way back in [Day 6](../dec-06/) when we talked about the 3 different
YAMLScript modes?
They were **bare**, **data** and **code**.
The bare mode was what you got then you ran (or loaded) a YAMLScript program
with no `!yamlscript/v0` tag at the top.
It just meant that you were effectively stuck in data mode, with no possibility
of executing any code.

There's actually comfort in **bare** mode.
You are using YAMLScript to process your data because it is one of the best
YAML loaders available, but you don't have to worry about any code being
run accidentally.

Today's post is about the other two modes.
Either you are writing a program (thus starting in **code** mode) or you are
doing cool stuff with your data files (thus starting in **data** mode).

In either case you are going to want to switch modes at various places in your
YAMLScript.
We learned before that you could switch modes by using the `!` tag.
This is the smallest possible tag and we use it for the most common need.

Here's an example of using YAMLScript to generate data to be loaded.
We'll start in code mode by using `!yamlscript/v0`:

```yaml
!yamlscript/v0

vars =: !
  colors:
  - red
  - ! ("gre" + "en")
  - blue
  numbers: !
    vec:
      map \(% * 10): ! [1, 2, 3]
  awesome: { yaml: true, xml: ! (1 > 2) }

=>: !
- ! "$(vars.colors.1) with envy"
- ! "Four score and $(vars.numbers.1) years ago"
- ! "Santa wears flip flops. $(vars.awesome.yaml) or $(vars.awesome.xml)?"
```

That's more bangs than a fireworks show in a barbershop!

This was just a crazy way to get the following data:

```yaml
- green with envy
- Four score and 20 years ago
- Santa wears flip flops. true or false?
```

Every time you see a `!` tag, it means that we are switching modes from `code`
to `data` or vice versa.

Let's walk through it:

* `!yamlscript/v0` - This tag starts the program in `code` mode. If we wanted to
  start in `data` mode, we would use `!yamlscript/v0/data`.
* `vars =: !` - We are setting the value of a variable named `vars` to the some
  data we want to write in plain old YAML. Before we can do that, we need to
  switch to `data` mode.
* `! ("gre" + "en")` - This is a YAMLScript expression that evaluates to the
  string `green`. To let YAMLScript know that we want to evaluate this
  expression, we switch back to `code` mode.
* `numbers: !` - Even though we just switched to `code` mode,
  in the previous point, it was only for that scalar / expression.
  We need to do it again since the mapping we are in is in data mode.

And so on.
Note that when we switch modes we do it only for that node, not for the whole
rest of the document.

### A Better Flip Flop Syntax

The `!` tag is ok for switching modes, but even though it's one character, I
find it a bit noisy.
Here's another way to switch modes:

```yaml
!yamlscript/v0

vars =::
  colors:
  - red
  - ! ("gre" + "en")
  - blue
  numbers::
    vec:
      map \(% * 10):: [1, 2, 3]
  awesome: { yaml: true, xml:: (1 > 2) }

=>::
- ! "$(vars.colors.1) with envy"
- ! "Four score and $(vars.numbers.1) years ago"
- ! "Santa wears flip flops. $(vars.awesome.yaml) or $(vars.awesome.xml)?"
```

When a `!` tag follows a `:` key/value separator, we can use a `::` instead to
mean the same thing.

There are still bangs in there because they didn't follow a `:`.


----

I find the `::` syntax to be a bit more readable.

Note that this isn't a special new YAML syntax.
It's simply an unquoted key that ends with a colon.

This trick works from both code and data modes.
It does not work from bare mode.
You'll just have a mapping key that ends with a colon.

It might seem like using the `!` or `::` as a toggle could get confusing
because you need to keep track of which mode you are in.

But typically it's pretty obvious for a person reading the YAML.
Machines need to be more exact.

I suspect that I'll introduce the `!code` and `!data` tags in the future, for
when you feel the need to be explicit.

----

That was a quick post but also an important one for learning how to write
better YAMLScript.

See you tomorrow for Day 23 of the YAMLScript Advent Blog!


{% include "../../santa-secrets.md" %}
