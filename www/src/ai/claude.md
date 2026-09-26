---
title: YAMLScript for Claude Code
talk: 0
hide:
- navigation
---

# YAMLScript for Claude Code

The YAMLScript plugin teaches Claude Code to write and validate idiomatic YS
code.
It includes focused language instructions and a YAMLScript linter.


## Install

In Claude Code, run:

```text
/plugin marketplace add yaml/yamlscript
/plugin install ys-skill@yamlscript
```

The skill is then available as `/ys-skill:yamlscript`.


## What the skill teaches

- Write correct Clojure first, then convert it to idiomatic YAMLScript.
- Prefer YS functions such as `say`, `in?`, and `words` over their Clojure
  equivalents.
- Apply dot and colon chaining idiomatically.
- Validate every attempt before presenting it.


## Try it

Ask Claude Code:

> Use the YAMLScript skill to write a program that groups a list of people by
> city, then validate the result.

[View the Claude Code plugin source](
https://github.com/yaml/yamlscript/tree/v0/ai/claude)
