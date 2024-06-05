---
title: Necessary YAML Knowledge
---

YAML was created to make data simple... most of the time.

It was also created to give people a lot of control over how they write their
data.
There's actually quite a bit about YAML that you need to know to use it to
write YAMLScript.

This document will cover all the YAML syntax, concepts and vocabulary that you
need to know to write YAMLScript effectively.

> NOTE: When a word or phrase is presented in double quotes, it is a YAML
vocabulary term that you should commit to memory.
These terms are used consistently throughout the YAMLScript documentation (and
also YAML specification and other YAML-related writings).


## YAML Data Model

YAML represents the same kind of data as JSON: "mappings", "sequences" and
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
  documents separated by `---` or `...` or no documents at all.
  * A YAML "stream" is all of the YAML text in a single YAML file or YAML string
    passed to a YAML "loader".
  * A YAML "document" is a single YAML data "node".
  * A "node" is a single value in the YAML data model: a mapping, sequence or
    scalar.
* Any YAML node in a document can be given a name (called an "anchor") and
  referred to by that name elsewhere in the document using an "alias".
  * An anchor looks like `&foo`.
  * An alias looks like `*foo`.
  * Example:
    ```
    first: &anchor
      a: mapping
      
    second: *anchor
    ```

* A mapping key can be any "kind" of YAML node, (not just a scalar node like in
  JSON).
  * This is not commonly used either in YAML or YAMLScript, but it is completely
    valid.
  * The word "kind" is used to refer to those three different YAML node shapes:
    "scalar", "mapping" or "sequence".
* Any YAML node can have a "explicit tag".
  * A tag looks like `!foo-bar` and is used to associate a word or string with
    a that YAML node.
  * Tags typically instruct a YAML loader how to interpret the node.
  * Even though it is rare to see tags in YAML, part of the loading process is
    to assign a tag to every node that lacks an explicit one.
    This process is known as "implicit tagging" or "tag resolution".
  * In YAMLScript, the tags `!yamlscript/v0` and just `!` are quite common.
    They control the "YAMLScript mode" of a document or node.

## Basic YAML Syntax

YAML supports a comment syntax.
Comments start with a `#` and continue to the end of the line.
Blank lines are also considered comments.

We'll show you same YAML examples to illustrate the basic syntax and use
comments to explain the concepts as much as possible.

Example 1: A YAML stream with several syntax elements:

```
# A line starting with `---` is used to begin a new document.
# It is not required for the first document in a stream.
# It is common to put any tag or anchor for the top level node on the same
# line as the `---`.

---

# The top level node of this document is a mapping:
# Mapping "pairs" consist of a "key" and a "value" separated by a colon.

# For the first pair we have a scalar key and a scalar value.
# Notice how the key is single-quoted and the value is double-quoted.

'first-key': "first-value"

# The second pair has another scalar key and scalar value.

second-key: second value

# Notice that both of them are unquoted.
# Scalars of can be expressed without quotes.
# This is very common in YAML and very important in YAMLScript.

# The kind of quoting (or lack thereof) is called the scalar's "style" in YAML.
# We'll talk more about styles later.

# Mapping pair values can be any kind of YAML node.
# Mappings and sequences can also be used as values and nested to an arbitrary
# depth.
# This is accomplished by indenting the nested node using one or more spaces.

third-key:
  a sub: mapping
  second:
    - a sequence
    - of values
    - a scalar
    - - sequence
      - items
    - another: mapping

  # The value here doesn't seem to be indented but the leading dash acts as
  # indentation.

  another sequence:
  - one
  - two

# Here we start a second document in the stream.
# This one is a top level sequence and it has an anchor and a tag:

--- &my-seq-1 !a-tag

# The first sequence item is a scalar string with content of "first item".
- first item

# The second item is another sequence:
-
  - one
  - two

# It is possible and typical to collapse this like so:
- - one
  - two

# The third item is a mapping:
-
  foo: bar
  baz: 42

# This may also be collapsed:

- foo: bar
  baz: 42

# This sequence collapsing can be many levels deep:

- - - - foo: bar

# But don't try collapsing multiple mappings.
# The next line would be an error:

# - foo: bar: baz   # ERROR

# A third document in the stream.
---

# Mappings can be expressed using a different syntax that looks like JSON.
# It uses curly braces for mappings and square brackets for sequences.
# The is called "flow style" for "collections".
# The normal indented style we've been using is called "block style" by
# comparison.

key 1: {x: red,
        y: blue, z: green}
key 2: [red, blue,
        green]
```

> TODO: This section is incomplete.

## Less Common YAML Syntax

Here's a few YAML syntax variants that you won't see very often and aren't
very common in YAMLScript.

> TODO: This section is incomplete.


## Common YAML/YAMLScript Gotchas

All YAMLScript files are required to be valid YAML.
YAMLScript uses most of YAML's capabilities and flexibilities to create a
clean programming language (one that doesn't look like just a bunch of data).

Unfortunately, this means that you can run into some situations where your
YAMLScript looks perfectly fine but is actually invalid YAML.

YAMLScript can repeat a string with this syntax: `'foo' * 3` yields
`'foofoofoo'`.

However, the following is not valid and would cause a YAML parsing error:

```yaml
say: 'foo' * 3
```

This is because YAML sees a single quoted scalar and text is not allowed to
follow the closing quote.

YAMLScript uses the `.` escaping character to fix this:

```yaml
say: .'foo' * 3
```

Now the right-hand side is an unquoted scalar whose value is `.'foo' * 3`.
YAMLScript will ignore the leading `.` and evaluate the expression as expected.

This `.` escaping character can be used anywhere that you want a scalar to be
"plain" (unquoted) but it happens to start with a character that happens to be
YAML syntax.

Some examples:

```yaml
# Here [3 4 5] is a YAMLScript vector, not a YAML sequence.
# Again we make the entire RHS a plain scalar by starting with a `.`.
say: .[3 4 5].reverse()
```
