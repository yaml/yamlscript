---
title: YS v1 Planning
comments: true
---


See [YS Versioning](ys-versioning.md) for a description of YS language
versioning.

This document describes the changes that we need to make in order to release YS
v0 as stable, then release YS v1 as stable, and then start working on YS v2.


## Basic Rules for v1

* Every YS enabled YAML file (stream) must start with a YS stream tag like
  `!YS/v1`.
* That will declare the semantics for the entire stream.
* A stream can import other streams of different versions.
* A runtime of v3 should support v1, v2 and v3, but not v4 or higher.
* The `v0` version usage will be deprecated soon after v1 is released.


## YS Stream Tags

Currently we allow these tags to start a YAML stream:

- `!yamlscript/v0`
- `!yamlscript/v0:`
- `!yamlscript/v0/bare`
- `!yamlscript/v0/code`
- `!yamlscript/v0/data`
- `!YS-v0`
- `!YS-v0:`

Tags ending with `:` imply data mode.
This is meant to be analogous to the `::` syntax we use for mode switching.

These tags can currently start any document.

After a document is started with one of these tags, subsequent documents can
start with a `!code`, `!data`, or (the default) `!bare` tag.


### v1 Planned Changes

We will allow these `!YS-v#` tags to start a YAML stream (they will be only
allowed on the first document in the stream):

* `!yamlscript.org/v1`  # Long form
* `!YS/v1`              # Short form
* `!YS-v0`              # Legacy v0 form

Note: each of those imply code mode and can have a `:` suffix to imply data
mode.

To start in bare mode you can use:

```yaml
!YS/v1
---
foo: bar
```

or:

```yaml
!YS/v1
--- !bare
foo: bar
```

In the first case, we are starting in code mode, but since we immediately
introduce a new document and that document has no tag, it will be in bare mode.

In the second case we do the same thing, but we explicitly state that we want
bare mode.

Both of these examples technically have two documents, but the initial document
will do nothing since it evaluates to `nil` (even in `-s` streaming mode because
`nil` values are not added to the stream).

The implicit mode (no tag) is always bare mode.

For data mode, we can use:

```yaml
!YS/v1:
foo: bar
```

or:

```yaml
!YS/v1
--- !data
foo: bar
```


### URL Tags

`!YS/v1` is short for `!yamlscript.org/v1` and that will be a URL that can do
interesting things:

* `docs.yamlscript.org/v1` might be the docs.
* `spec.yamlscript.org/v1` might be the spec.
* `schema.yamlscript.org/v1` might be the schema.


## YS Namespaces

Right now yamlscript using `yamlscript.*` and `ys.*` namespaces internally.
It exposes a number of the `ys.*` namespaces to the user.

In v1 and v2 these namespaces will have differing behavior.

Each YS stream (file) will be tied to a particular major version.

We need to rename the internal namespaces to something like `ys.v0.*` and
`yamlscript.v0.*`.
Then introduce `ys.v1.*` and `yamlscript.v1.*` for the next major version.

The new namespaces can "inherit" from the old namespaces and change the things
they need to.


## Documentation

Each version will have its own documentation.

The web site will need to have a version selector.

We need a URL format for the different versions of the documentation.


## v0 Review and Cleanup

We should try to get v0 to a place where it is tight and stable.

This might mean getting rid of some of the features that didn't work out.


### Binary Size

We should try to reduce the size of the binary a bit.


### Bundled Libraries

We should remove the bundled libraries that are not heavily used and could
easily be added as dependencies by programs.


### Remove `std/fs-*` functions

Since functions like `fs-e` are also available as `fs/e` we should remove the
aliases and just keep the `fs` namespace.


### Finish `use` and `dep`

We need to finish `use` and `dep` commands before v1 is stable.


### Binding Capabilities

We should switch the binding capabilities to not allow code mode by default.
