# Runtime reference regression tests

These fixtures began as proposals and are now loaded by
`yamlscript.reference-runtime-test` in the normal core test suite.
Run the JVM tests and native CLI checks with:

```sh
make -C core shell CMD='lein test yamlscript.reference-runtime-test'
make -C ys test-references
```

The native cases have a 20-second timeout per process.
The normal `make -C ys test` target includes these checks.

## Fixture formats

`runtime-references.yaml` pairs complete YAMLScript sources with `serialized`
values.
These are the ordinary data structures an output serializer should produce,
not strings containing YAML or JSON.
The runner evaluates the source, converts references to ordinary data with
`ys.v0.reference/serialize`, and compares it with the expected structure.
It must not substitute reference mappings before passing arguments to
functions.
Optional `stdout` and `stdout-contains` fields check effects separately from
the serialized return value, including effect order across suspended calls.
The explicit `get` calls are intentional: they exercise ordinary functions on
references, including computed keys and numeric-looking string keys.
The exact `*.a + 1` regression preserves the user's example rather than
rewriting it to an increment operator.

`reference-paths.yaml` pairs typed `path` vectors with expected `path-string`
values and one-pair `serialized` mappings.
An optional `document` field selects a stream-qualified path.
These fixtures specify formatting independently of runtime evaluation.
For example, `[foo, 42, bar]` becomes `/foo/42/bar`, whereas `[foo, '42', bar]`
becomes `/foo/'42'/bar`.
Backslashes in single-quoted YAML scalars are literal path-string characters.
JSON output will need another escaping layer to carry those backslashes.

## Runtime behavior

Functions can inspect collection shape or select individual values without
forcing every descendant.
Only dependencies actually accessed by an expression need evaluation.
Deferred results are cached, including `null` and `false`.
Suspending an expression must not restart it or repeat its side effects.
Dependency scheduling and serialization should use explicit work stacks rather
than consume one language call frame per reference edge.
Known acyclic expressions keep the existing immutable compilation.
Potential cycles use runtime evaluators and transparent persistent collection
views.
The runtime cache stores evaluation state separately from immutable document
values.
JVM evaluators suspend on virtual threads; the native runtime uses goroutines.
This preserves ordinary synchronous function calls without restarting them.
The JVM implementation requires Java 21 or newer; Makes currently supplies 27.

Collections can retain reference edges instead of recursively copying values.
At output, expand resolvable references and replace circular edges with
`{'*': <target-path>}`.
Detect cycles on the active expansion path, not with a global visited set:
ordinary sharing is not a cycle.
The target path identifies the original node in the current document, not the
location where a copy happens to be printed.
The fixtures include repeated serialization of the same cyclic node to make
that proposed distinction explicit.

Only internal unresolved references receive special handling.
An ordinary user mapping with a `'*'` key remains ordinary data.
A failed function call must not silently become a reference mapping.
Existing safe lookup of an absent key continues to yield `null`.

## Path spelling

Current-document paths start with `/` and have `/`-separated components.
The current-document root is `/`.
Stream paths start with `//` followed by the document number, for example
`//0/foo/42/bar`.
The root of that stream document is `//0`, without a trailing separator.
Document numbering is zero-based, matching `**.0`.
Both kinds use the same `'*'` key in the serialized reference mapping.
Sequence indexes are unquoted decimal integers.
String keys that look numeric are enclosed in single quotes.
The numeric cases include signs, leading zeroes, fractions, and
scientific notation, not only valid sequence indexes.
Other string keys use JSON string escaping without enclosing double quotes.
Embedded `/` is represented as `\u002F` so it cannot split a component.
Literal backslashes are escaped, so a key containing the text `\u002F` stays
distinct from a key containing `/`.
Boundary apostrophes can use `\u0027` to avoid looking like the numeric-key
quoting syntax.

These tests specify output formatting only.
They do not expand the permitted alias-expression syntax, add path parsing, or
define how a reference mapping should be interpreted when reading a document.

## Implemented choices to review

- Empty-string keys need a distinct spelling now that `/` names the root.
  The `decision: empty-key` fixtures quote an empty component as
  `''`, extending the numeric-key quoting convention.
  This is an explicit spelling choice, rather than an ambiguity with `/`.
- Pure reference loops retain each original target path.
  Thus `a` referring to `b` and `b` referring to `a` serialize as references to
  `/b` and `/a`, respectively, rather than arbitrarily choosing one survivor.
- Circular calculations have `decision: circular-calculation`.
  Their output retains each uncomputable result as a reference to its
  own destination path.
  Functions that require the missing value are stopped, not called with the
  serialized marker as a substitute argument.
  This does not supply a numeric solution to an arithmetic cycle.
- Stream fixtures assume completed documents retain reference identity
  internally.
  When a cyclic node from a previous document is printed in a later document,
  its back-reference is qualified with its original stream document number.
  This does not change the rule that `**` excludes the currently evaluating
  document.

Generated tests cover 10,000 alias links, 2,000 computed dependencies, and
2,000 linked collections ending in a cycle.
These exercise the evaluator and serializer without recursive graph traversal.
Quoted-key fixtures also cover the conditional-key resolver bug discovered
while writing these tests.

This does not guarantee termination of arbitrary user code.
A function that independently walks a cyclic collection forever still needs
its own termination condition.
