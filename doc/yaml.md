---
title: YAML Basics
talk: 0
---

YAML was created to make data documents simple (most of the time).
It was also created to give people a lot of control over how they write their
data.

Modern programmers generally have a decent understanding of YAML basics as it
ends up in many places in the tech world.
That said, there are many lesser well-known YAML concepts and features and
YS makes use of most of them.

Remember, all valid YS is required to be valid YAML syntax.
To write YS well, you'll need to have a solid understanding of YAML concepts
and also Clojure concepts.

This document will cover all the YAML syntax, concepts and vocabulary that you
need to know to write YS effectively.

!!! note

    When a word or phrase is presented in double quotes here, it is intended
    to be a YAML vocabulary term that you should commit to memory.
    These terms are used consistently throughout the YS documentation (and also
    the YAML specification and other YAML-related writings).


## YAML Data Model

YAML represents the same kinds of data as JSON: "mappings", "sequences" and
"scalars".

* A "mapping" is an unordered collection of key-value pairs where each key is
  unique.
  Mappings are also known as 'hashes', 'dictionaries', 'objects' or 'associative
  arrays' in other languages, but in YAML they are always called mappings.
* A "sequence" is an ordered collection of values.
  Sequences are also known as 'arrays', 'lists' or 'vectors' elsewhere.
* A "scalar" is a single value, like a number, string, keyword, boolean or even
  a null value.
  Scalars are also known as 'atoms' or 'primitives' elsewhere.

YAML adds just a few things to this basic data model:

* YAML "streams" can consist of one YAML "document" (the normal case), multiple
  documents separated by `---` / `...`, or no documents at all.
  * A YAML "stream" is all of the YAML text in a single YAML file or YAML string
    passed to a YAML "loader" or generated by a YAML "dumper".
  * A YAML "document" is a single YAML data "node".
  * A "node" is a single value in the YAML data model: a mapping, sequence or
    scalar.
* Any YAML node in a document can be given a name (called an "anchor") and
  referred to by that name elsewhere in the document using an "alias".
  * An anchor looks like `&foo` and an alias looks like `*foo`.
  * Example:
    ```yaml
    first: &anchor
      a: mapping
    second: *anchor  # Both values are the same mapping: {"a": "mapping"}.
    ```
* A mapping key can be any "kind" of YAML node, (not just a scalar node like in
  JSON).
  * This is not commonly used either in YAML or YS, but it is completely valid.
  * The term "kind" is used to refer to those three different YAML node shapes:
    "mapping", "sequence" or "scalar".
* Any YAML node can have a "explicit tag".
  * A tag looks like `!foo-bar` and is used to associate a word or string with
    a that YAML node.
  * Tags typically instruct a YAML loader how to interpret the node.
  * Even though it is rare to see tags in YAML, part of the loading process is
    to assign a tag to every node that lacks an explicit one.
    This process is known as "implicit tagging" or "tag resolution".
  * In YS, the tags `!YS-v0` and just `!` are quite common.
    They control the YS "[mode](modes.md)" of a particular node.


## Basic YAML Syntax

YAML supports a comment syntax.
Comments start with a `#` and continue to the end of the line.
Blank lines are also considered comments.
The `#` must be at the start of the line or preceded by whitespace.

We'll show you some YAML examples to illustrate the basic syntax and use
YAML comments to explain the concepts as much as possible.

Example 1: A YAML stream with several syntax elements:

```yaml
# A line starting with `---` is used to begin a new document.
# It is not required for the first document in a stream.
# It is common to put any tag or anchor for the top level node on the same
# line as the `---`.

---

# Let's start with the common mapping and sequence nodes.
# Mappings are a group of "pairs", each consisting of a "key" and a "value"
# separated by a colon.
# Sequences are a group of nodes where each one is prefixed by a dash.
# Both the colon and the dash must be followed by a whitespace to be valid.

# The top level node of this document is a mapping.

# For the first pair we have a scalar key and a scalar value.
# Notice how the key is single-quoted and the value is double-quoted.

'first-key': "first-value"

# The second pair has another scalar key and scalar value.

second-key: second value

# Notice that both of them are unquoted.
# Scalars of can be expressed with or without quotes.
# This is very common in YAML and very important in YS.

# The kind of quoting (or lack thereof) is called the scalar's "style" in YAML.
# The different quoting styles have different encoding rules and different
# semantics.
# There are actually 5 distinct scalar styles in YAML: plain, single quoted,
# double quoted, literal and folded.
# We'll cover these styles in more detail in a second.

# Mapping pair values can be any kind of YAML node.
# Mappings and sequences can also be used as values and nested to an arbitrary
# depth.
# This is accomplished by indenting the nested node using one or more spaces.

third-key:
  a: sub-mapping
  second:
  - a sequence
  - of values
  - a scalar
  - - sub-sequence
    - items
  - another: sub-mapping

  # The sequence value under the key `second` here doesn't seem to be indented
  # but the leading dash acts as indentation.
  # This is the preferred style for indenting sequence values of mapping pairs
  # in YAML, but you can also indent them more if you prefer.

  another sequence:
  - one
  - two

Here's what the 5 scalar styles look like:

plain:
- I'm unquoted and plain
- I can also span multiple lines
  where whitespace beteween lines
  folds to a single space
single quoted:
- 'I''m single quoted'
- 'I have one thing that is escapable,
  the single quote itself '' which
  is escaped by doubling it'
- 'Multiline single quoted scalars
  fold the same way as plain'
double quoted:
- "I'm double quoted"
- "I'm the only style capable of encoding
  any possible string value"
- "I have lots of escapes like \n and \t
  and also \" and \\"
- "Multiline double quoted scalars
  fold the same way as plain"
literal: |
  I'm like a heredoc in Shell or Perl.

  But my scope is determined by indentation.
  Newlines are preserved as you would expect
  from a heredoc.
folded: >
  Folded scalars pretty much fold like
  the others, but you can use them without
  worry of ` #` or `: ` being special.

  They are the most rarely used scalar style.

In YS code mode:
- plain: Used for code expressions
- single quoted: Used for character strings
- double quoted: Used for strings with interpolation support
- literal: Used for template strings including interpolation
- folded: Not allowed in code mode

# A line with `...` is used to end a document.
# It's optional, since `---` does the same thing, but you might want to use it
# for clarity.

...


# Here we start a second document in the stream.
# This one is a top level sequence and it has an anchor and a tag:
# The `---` indicator is required and you can also use the line to specify the
# anchor and tag for the top level node.

--- &my-seq-1 !a-tag

# The first sequence item is a scalar string with content of "first item".
- first item

# The second item is another sequence:
-
  - one
  - two

# It is possible, typical and preferred to collapse this like so:
- - one
  - two

# The third item is a mapping:
-
  foo: bar
  baz: 42

# This may also be collapsed (and is also preferred) like so:

- foo: bar
  baz: 42

# This sequence collapsing can be many levels deep:

- - - - foo: bar

# But don't try collapsing multiple mappings.
# The next line would be an error:

# - foo: bar: baz   # ERROR ': ' not allowed in plain scalars

# A third document in the stream. Note that no `...` was used to end the
# previous document.

---

# Mappings and sequences can be expressed using a different syntax that looks
# like JSON.
# YAML uses curly braces for mappings and square brackets for sequences.
# These are called "flow style" for "collections".
# The normal indented style we've been using is called "block style" by
# comparison.
# Note: A YAML "collection" is the generic term for a node that is either a
# mapping or a sequence.

key 1: {x: red,
        y: blue, z: green}
key 2: [red, blue,
        green]

# If you ever need an empty mapping or sequence, you need to use the following:

empty mapping: {}
empty sequence: []

# There is actually no way to write empty collections in block style.
```


## Less Common YAML Syntax

Here's a few YAML syntax variants that you won't see very often.
Some of these are used in YS, so it's good to know about them.

```
# YAML has a top level "directive" syntax.
# There are only 2 directives defined by the YAML 1.2 specification:
# This is the first one, the "YAML directive".
# It simply specifies the version of the YAML specification in play.

%YAML 1.2

# The second directive is the "TAG directive".
# It allows you to specify a shorthand for a tag URI.

%TAG !ys! tag:yamlscript.org,2022:

# To date, directives are not used in YS.

# The `---` indicator is required to start a new document if you have used any
# directives (even on the first document).

---

# YAML allows a mapping key to be any node, not just a scalar.
# In other words it allows collections or even aliases to be used as keys.

- &map1
  a: value
# Here it is obvious that the key is a mapping.
- *map1 : 42
# Here we are using YAML's "explicit key" syntax (`?`) to specify the key.
- ? [a, flow, sequence]
  : 42
# We can use `?` with block collections too.
- ? a: block
    mapping: key
  : 43
# We can even use it for literals scalar keys.
- ? |-
    a literal
    scalar key
  : 44

---

# The collection key does have a good use case in YS.
# Consider this `for` loop:

for a foo(), b bar(), c [1 2 3]:
  say: a + b + c

# YS requires that the `for […]` key be a plain scalar, and YAML
# requires that plain scalar keys need to be a single line.
# This could lead to unreadable code if we our "for binding" is complex.

# Bute can also do it like this:

for:
  ? a: foo()
    b: bar()
    c: [1 2 3]
  : say: a + b

# Which you might find to be clearer.
```

## See Also

* [The yaml.info Site](https://www.yaml.info/learn/)
* [Common YAML / YS Gotchas](gotchas.md)
