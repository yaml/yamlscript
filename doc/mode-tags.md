---
title: YS Mode Tags
talk: 0
---

YS has 3 [modes](modes.md) that every node in a YAML document can be in: data,
code, and bare.

By default YS sees YAML files as being in bare mode, and loads them according to
basic YAML rules.

You need to use a special tag for YS to be able to use logic in a YAML file.

These tags are applied to each document (top level node) in a YAML file.
Since most YAML files only have one document, you can think of these tags as
being declared at the top of the files and being applied to the whole file.


## v0 YS Mode Tags

* `!YS-v0` - Start in code mode.
* `!YS-v0:` - Start in data mode.
* `!code` - Start in code mode. Must come after a `!YS-v0` tag.
* `!data` - Start in data mode. Must come after a `!YS-v0` tag.
* `!bare` - Start in bare mode. Must come after a `!YS-v0` tag.

The `!code`, `!data`, and `!bare` tags are used for clarity but can only be used
on a document that follows a document with a `!YS-v0` tag.

One way to use `!data` without have a previous document is:

```yaml
!YS-v0
--- !data
num:: 6 * 7  # 42
```

In reality this YAML has two documents, but since the first one has no content
it is ignored by the YS compiler.
Even though it is ignored, it serves to add the `!YS-v0` tag to the file, so
that you can now use `!data` or `!code` or `!bare` tags in the rest of the file.

!!! note

    Every document in a YAML file is considered "bare" unless it has a mode tag
    at the top of that document.


## Alternate v0 YS Mode Tags

Originally, these mode tags were supported for YS v0:

* `!yamlscript/v0` - Start in code mode.
* `!yamlscript/v0:` - Start in data mode.
* `!yamlscript/v0/code` - Start in code mode.
* `!yamlscript/v0/data` - Start in data mode.
* `!yamlscript/v0/bare` - Start in bare mode.

These tags are still supported for backwards compatibility, but the `!YS-v0`
tags are preferred.
They may be removed in a future version of YS.
