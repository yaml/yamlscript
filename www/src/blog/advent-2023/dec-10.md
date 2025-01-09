---
title: States and Ladders
date: '2023-12-10'
tags: [blog, advent-2023]
permalink: '{{ page.filePathStem }}/'
author:
  name: Ingy döt Net
  url: /about/#ingydotnet
---

Santa is a busy guy.
He has a lot of work to do.
He has to make a list and check it twice.
He has to find out who's naughty and nice.
He has the monumental task of transforming wishes into happiness.

YS only needs to transform YAMLScript into Clojure.
But it's a bit more involved than you might think.

To make things easier the YS compiler breaks the transformation into eight
distinct States and seven distinct ~~Ladders~~ transformations.

### Welcome to Day 10 of the YAMLScript Advent Calendar

Today we're going to learn more exactly how `ys --compile` turns YAMLScript into
Clojure.  
_Fair Warning: This is going to be a long post_.

The `ys` CLI has an awesome tool to visualize the transformation process any
time you are interested.
This might be for debugging where something went wrong, or just to learn more
about how the compiler works.

To begin this journey, let's use this visualizer in action.
You could write it like this:

```bash
ys --compile --debug-stage=all -e 'name =: "Clojure"' -e 'say: "Hello, $name!"'
```

The one line program is equivalent to the file containing:

```yaml
!yamlscript/v0
name =: "Clojure"
say: "Hello, $name!"
```

Let's run the command now (but we'll use the shorter options):

```bash
$ ys -c -e 'name =: "Clojure"' -e 'say: "Hello, $name!"' -d
*** parse     *** 0.127737 ms
({:+ "+MAP", :! "yamlscript/v0/code"}
 {:+ "=VAL", := "name ="}
 {:+ "=VAL", :$ "Clojure"}
 {:+ "=VAL", := "say"}
 {:+ "=VAL", :$ "Hello, $name!"}
 {:+ "-MAP"}
 {:+ "-DOC"})

*** compose   *** 0.009495 ms
{:! "yamlscript/v0/code",
 :% [{:= "name ="} {:$ "Clojure"} {:= "say"} {:$ "Hello, $name!"}]}

*** resolve   *** 0.073969 ms
{:pairs
 [{:def "name ="}
  {:vstr "Clojure"}
  {:exp "say"}
  {:vstr "Hello, $name!"}]}

*** build     *** 0.375378 ms
{:pairs
 [[{:Sym def} {:Sym name}]
  {:Str "Clojure"}
  {:Sym say}
  {:Lst [{:Sym str} {:Str "Hello, "} {:Sym name} {:Str "!"}]}]}

*** transform *** 0.027342 ms
{:pairs
 [[{:Sym def} {:Sym name}]
  {:Str "Clojure"}
  {:Sym say}
  {:Lst [{:Sym str} {:Str "Hello, "} {:Sym name} {:Str "!"}]}]}

*** construct *** 0.087933 ms
{:Top
 [{:Lst [{:Sym def} {:Sym name} {:Str "Clojure"}]}
  {:Lst
   [{:Sym say}
    {:Lst [{:Sym str} {:Str "Hello, "} {:Sym name} {:Str "!"}]}]}]}

*** print     *** 0.014494 ms
"(def name \"Clojure\")(say (str \"Hello, \" name \"!\"))"

(def name "Clojure")
(say (str "Hello, " name "!"))
```

Woah! That's a lot of output.
But it's exactly what I want to tell you about today.

At the bottom is the clojure code we expect.
Above that is the output of each of the seven transformations (aka the 7 states
that come after the first state: our YS input).

Let's look at each of these transformations in turn...


### Transformation 1: Parse

The first transformation is parsing YAML into a sequence of "parse events".
This is by far the hardest transformation, not just for YS but for any YAML
processor.
Sadly, it's so hard that only a few of the dozens of YAML processors out there
actually do it correctly.
See: <https://matrix.yaml.info/>

On the other hand, this is actually the simplest part for the YS compiler to
implement.
Why?
Because it uses somebody else's YAML parser!!

> Note: YS currently uses the
[SnakeYAML Engine](https://bitbucket.org/snakeyaml/snakeyaml-engine) framework
(only for it's YAML 1.2 parser component).
Later we plan to use [libfyaml](https://github.com/pantoniou/libfyaml) which is
currently considered the best YAML parser in the world.
SnakeYAML was the obvious first choice because it's written in Java, YS is
written in Clojure and Clojure is a JVM language.
It's doing a great job for now!

This is a good time to mention that the YAML data language spec describes
"loading" YAML text into native data structures as a several step process of
states and transformations.
In reality, the YS compiler is really just a very fancy YAML loader!

YAML parsers typically produce 10 different kinds of parse events:

* Start Stream
* End Stream
* Start Document
* End Document
* Start Sequence
* End Sequence
* Start Mapping
* End Mapping
* Scalar Value
* Alias Reference

YS (currently) doesn't care about the first 4 events, so it just ignores them.

That leaves use with a mapping containing 2 key/value pairs:

```clojure
[{:+ "+MAP", :! "yamlscript/v0"}
 {:+ "=VAL", := "name ="}
 {:+ "=VAL", :$ "Clojure"}
 {:+ "=VAL", := "say"}
 {:+ "=VAL", :$ "Hello, $name!"}
 {:+ "-MAP"}]
```

We are showing this state as a Clojure data structure and that's the kind of
serialization that we use in the YS compiler tests.

Here's a quick breakdown of the mapping keywords used above:

* `:+` is the parse event type
* `:!` is the (optional) YAML tag
* `:=` is a plain (unquoted) scalar value
* `:$` is a double quoted scalar value

That was a lot of info, but hopefully it sets the stage for the rest of this
post.

If you are interested in seeing how all this is implemented, check out:

* [All the YS transformation libraries source code](
  https://github.com/yaml/yamlscript/tree/main/core/src/yamlscript)
* The YS Test Suite Files:
  * <https://github.com/yaml/yamlscript/blob/main/core/test/compiler-stack.yaml>
  * <https://github.com/yaml/yamlscript/blob/main/core/test/compiler.yaml>


### Transformation 2: Compose

The second transformer is called the composer (in the `yamlscript.composer`
library).

```clojure
{:! "yamlscript/v0",
 :% [{:= "name ="} {:$ "Clojure"} {:= "say"} {:$ "Hello, $name!"}]}
```

Its simple job is to take the parse events and compose them into a tree of
mapping, sequence and scalar nodes.

It preserves the node's tag if any.

Here we see a new keyword `:%` which is used for a mapping node.
Sequence nodes use the keyword `:-` but they only show up in data mode, which we
aren't using here.

Now we have all the important information from the YAML input in an AST form
that we can refine (transform) a few more times until it becomes a Clojure AST!


### Transformation 3: Resolve

YAML tags are rarely seen in the wild.
Many YAML users don't even know they exist.
But tags play an important role in the YAML load process.
Internally every single untagged node is assigned a tag based on heuristics that
consider its kind, position and content.

This process is called "tag resolution" and it's what we are doing here:

```clojure
{:ysm
 [{:ysx "def name"}
  {:ysi "Clojure"}
  {:ysx "say"}
  {:ysi "Hello, $name!"}]}
```

Notice that all the keywords changed to `:ys?` here.
This is how YS stores the tag.
Each tag knows what kind of node it is attached to (map, seq or scalar) so, to
keep things simple, it doesn't store that info in the AST.

> Note: Keeping the structure of each state AST as simple as possible is a key
concern of the YS compiler.
It makes it easier to see what's going on when debugging and even more
importantly, it makes it easier to write tests for each transformation.

The keywords seen above are:

* `:ysm` is a code mode mapping node
* `:ysi` is a scalar that supports interpolation
* `:ysx` is a ys expression (a string of code to be further parsed)

Tags are are essentially the names of the transformations that will be applied
to the node in the following transformation.


### Transformation 4: Build

The build transformation is where a lot of the magic happens.

Most importantly, it's where scalars containing YAMLScript expressions are lexed
and parsed into Clojure expression ASTs.

```clojure
{:ysm
 ([{:Sym def} {:Sym name}]
  {:Str "Clojure"}
  {:Sym say}
  {:Lst [{:Sym str} {:Str "Hello, "} {:Sym name} {:Str "!"}]})}
```

For instance we can see that the simple string expression "def name" was parsed
into 2 Clojure symbol nodes: `{:Sym def}` and `{:Sym name}`.

We can also see that the string expression "Hello, $name!" was parsed into a an
interpolated string expression that joins string literals and variable values
into a single string.

One important aspect of the build output AST is that it retains the original
structure of the YAML input.
Mappings still have key/value pairs, and the key info stays separate from the
value info.


### Transformation 5: Transform

The transformer transformation (that's a bit awkward I'll admit) is where
"special cases" are handled.

Not much happens in this stage yet.
In fact nothing at all happened in this example.

```clojure
{:ysm
 [[{:Sym def} {:Sym name}]
  {:Str "Clojure"}
  {:Sym say}
  {:Lst [{:Sym str} {:Str "Hello, "} {:Sym name} {:Str "!"}]}]}
```

It's exactly the same as the build output.

In the future, the transform stage is where we will add support for letting
users defined their own syntax and semantic transformations for specific
functions.
If you know about Lisp macros, this will be something spiritually similar.

If you happen to think a particular function should be coded a specific way,
you'll be able to make it so.
As long as you don't break the rules of physics... or YAML!

Note that even though this transformation can change the AST quite a bit, it
still retains its overall YAML structure.


### Transformation 6: Construct

The constructor phase's job is simple.
Turn the final YAML structured AST into an AST that directly represents the
intended Clojure code compilation result.

It does this essentially by applying the function asscoiated with a node's tag
to the node's value.

```clojure
{:Lst
 [{:Sym do}
  {:Lst [{:Sym def} {:Sym name} {:Str "Clojure"}]}
  {:Lst
   [{:Sym say}
    {:Lst [{:Sym str} {:Str "Hello, "} {:Sym name} {:Str "!"}]}]}]}
```

Every node in this AST directy represents a Clojure code construct.
Voila!


### Transformation 7: Print

Just like in any Lisp, a Clojure AST "prints" directly to Clojure code.
This is because every node in the AST is essentially a token that knows how to
print itself!

Again here's our final AST:

```clojure
{:Lst
 [{:Sym do}
  {:Lst [{:Sym def} {:Sym name} {:Str "Clojure"}]}
  {:Lst
   [{:Sym say}
    {:Lst [{:Sym str} {:Str "Hello, "} {:Sym name} {:Str "!"}]}]}]}
```

The `yamlscript.printer/print` function converts that to this string (of Clojure
code):

```clojure
"(def name \"Clojure\")\n(say (str \"Hello, \" name \"!\"))\n"
```

When that string is printed out, it looks like this:

```clojure
(def name "Clojure")
(say (str "Hello, " name "!"))
```

Aaaand, we're done!


### Transformation 1-7: Compile

The YAMLScript compiler is just all the above crammed together is sequence;
passing YS input through the 7 transformations we just described.

I'd like to show you the actual YAMLScript `compile` function (written in
Clojure):

```clojure
(defn compile
  "Convert YAMLScript code string to an equivalent Clojure code string."
  [^String yamlscript-string]
  (->> yamlscript-string
    yamlscript.parser/parse
    yamlscript.composer/compose
    yamlscript.resolver/resolve
    yamlscript.builder/build
    yamlscript.transformer/transform
    yamlscript.constructor/construct
    yamlscript.printer/print))
```

Pretty straightforward, right?
Or if you are more familiar with YAMLScript than Clojure:

```yaml
defn compile(yamlscript-string):
  "Convert YAMLScript code string to an equivalent Clojure code string.":
  ->>:
    yamlscript-string
    yamlscript::parser/parse
    yamlscript::composer/compose
    yamlscript::resolver/resolve
    yamlscript::builder/build
    yamlscript::transformer/transform
    yamlscript::constructor/construct
    yamlscript::printer/print
```

I hope I didn't waste too much of your Sunday on this post.
I know it was a lot to take in, and I also know you don't need to know all this
to be a good YAMLScript programmer.

But then again, you probably want to know all about this to become a great one!

See you tomorrow for Day 11 of the YAMLScript Advent Calendar.
