---
title: Common YAML / YAMLScript Gotchas
---

All YAMLScript files are required to be valid YAML.
YAMLScript uses most of YAML's capabilities and flexibilities to create a
clean programming language (one that doesn't look like just a bunch of data).

Unfortunately, this means that you can run into some situations where your
YAMLScript code looks perfectly fine but is actually invalid YAML.

Here's a common gotcha.
YAMLScript can repeat a string with this syntax: `'foo' * 3` yields
`'foofoofoo'`.

However, the following is not valid and would cause a YAML parsing error:

```yaml
say: 'foo' * 3
```

This is because YAML sees a single quoted scalar and text is not allowed to
follow the closing quote.

YAMLScript provides the `.` escaping character to fix this:

```yaml
say: .'foo' * 3
# (these also work in this case):
say: ('foo' * 3)
say: 3 * 'foo'
```

Now the right-hand side is a plain scalar whose value is `.'foo' * 3`.
YAMLScript will ignore the leading `.` and evaluate the expression as expected.

This `.` escaping character can be used anywhere that you need to use a plain
scalar to write a YAMLScript expression but the leading character would
otherwise be interpreted as YAML syntax.

Another example:

```yaml
# Here [3 4 5] is a YAMLScript vector, not a YAML sequence.
# Again we make the entire RHS a plain scalar by starting with a `.`.
say: .[3 4 5].reverse()
```

It is super common to use `[]` vectors and `{}` mappings in Clojure code
expressions and thus in YAMLScript code expressions.
In YAML, the same syntax is used for flow sequence and mapping nodes.
This can cause confusion.

We "fix" this, again, by using the `.` escaping character.
Also, we simply disallow (in code mode) YAML flow sequences and flow mappings.
Using the `.` to get the scalar expression version of the same thing works fine
(and as a bonus, there is no need for the commas and colons).

For example:
```yaml
!yamlscript/v0

# This is an error (using a flow sequence in code mode):
say: [1, 2, 3]
# This is a scalar that re-parses as a vector:
say: .[1, 2, 3]
# And thus does not need the commas (commas are whitespace in previous line):
say: .[1 2 3]
# We can use the YAML flow collection syntax if we switch to data mode:
say:: [1, 2, 3]
# Same switching as above, but with `!` instead of `::`:
say: ! [1, 2, 3]
# The commas are require in the flow collections.
```

YAML block sequences (lines starting with `- `) are also disallowed in code
mode.

The rationale here is that if you ever see `{`, `[`, `- ` or `>' (folded scalar)
you can assume that you are in data mode and not code mode.

Example errors:

```text
$ ys -e 'say: [1, 2, 3]'
Error: Sequences (block and flow) not allowed in code mode

$ ys -e '
say:
- 1
- 2
- 3'
Error: Sequences (block and flow) not allowed in code mode
```

There are various ways to do it correctly:

```text
$ ys -e 'say:: [1, 2, 3]'
[1 2 3]

$ ys -e '
say::
- 1
- 2
- 3'
[1 2 3]

$ ys -e 'say: .[1, 2, 3]'
[1 2 3]
```

Don't worry, you'll get the hang of it quickly!
