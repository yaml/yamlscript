yamlscript/core
===============

The YS compiler and runtime written in Clojure


## Synopsis

```clojure
(do
  (require '[yamlscript.compiler :as ys])
  (-> "foo: bar baz"
    ys/compile))
=> "(foo bar baz)\n"
```


## Description

This directory builds the YS language written in Clojure.

The `yamlscript.compiler/compile` function takes a YS input string and compiles
it to a Clojure code string.


## Document references and snapshots

Code in a data collection can reference its containing collection with `*.`,
an ancestor with `*^`, the document root with `*/`, or an anchor with `*name`.
Paths, sibling selectors, colon calls, and postfix operators can follow these
references.
Calls, postfix operators, and lookups can be interleaved in written order:
`*~:keys.#` counts keys, and `*~.b:vector.#.++` wraps `b` in a vector, counts
it, then increments the count.
A lookup after a call or postfix operator reads its result, not the original
document: `*.b:vector.0` selects the first element of the resulting vector.
`**` accesses completed stream documents, excluding the current document.

Reference expressions are evaluated after the initial document is built.
The compiler orders known dependencies before their dependents, choosing ready
expressions in source order.
Ordinary collection reads depend on deferred descendants too.
Cycles are compile-time errors, even when a function might only inspect keys.
No function name receives special dependency treatment.

Put `~` immediately after the initial `*` to read a snapshot without adding
dependency edges: `*~foo.bar`, `*~^.key`, or `*~/key`.
`*~` and `*~.` both mean the containing collection's current snapshot.
`count(*~)`, `count(*~.)`, `*~:count`, and `*~.#` are equivalent.
The snapshot contains already evaluated values and `nil` placeholders for
pending deferred entries.
It stays immutable when later expressions update the document.

Snapshot expressions are still deferred.
Ordinary references in the same expression still establish dependencies.
Only a leading snapshot modifier is supported; `*foo.~bar`, `*~~`, and `*~*`
are invalid.
`*.!` remains the falsey operator, and bare `*` remains multiplication.
In bare YAML, aliases retain their literal names, including `~` prefixes.

Runtime graph resolution and circular-reference serialization are not part of
this implementation.
The earlier experiment is preserved on `parent-references-runtime-cycles`.


## Makefile usage

* `make test`

  This runs the test suite.
  Run with `make test v=1` for verbose output.
  Run with `w=1` to show reflection warnings.

* `make -C ../ys test-snapshots`

  This runs the shared snapshot fixtures on the JVM and rebuilt native CLI.
  JVM validation currently requires the local SnakeYAML fork installed from
  `../repos/snakeyaml-engine`.
  Publishing that dependency remains a separate release prerequisite.
