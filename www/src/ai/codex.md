---
title: YAMLScript for Codex
talk: 0
hide:
- navigation
---

# YAMLScript for Codex

The YAMLScript skill teaches Codex to write and validate idiomatic YS code.
It includes focused language instructions, a YAMLScript linter, and Codex UI
metadata.


## Install with Codex

Invoke the built-in skill installer from a Codex prompt:

```text
$skill-installer Install the YAMLScript skill from
https://github.com/yaml/yamlscript/tree/v0/ai/codex/.agents/skills/yamlscript
```

Codex detects installed skills automatically.
If the skill does not appear, restart Codex.

Run `/skills` or type `$yamlscript` to invoke it explicitly.
Codex can also select it automatically when your task involves YAMLScript.


## Install manually

From a YAMLScript source checkout, install the skill for all your projects:

```bash
mkdir -p ~/.agents/skills
cp -R ai/codex/.agents/skills/yamlscript ~/.agents/skills/
```

For one repository, copy it into that repository instead:

```bash
mkdir -p /path/to/repository/.agents/skills
cp -R ai/codex/.agents/skills/yamlscript \
  /path/to/repository/.agents/skills/
```

Codex scans `.agents/skills` from the working directory through the repository
root, as well as `$HOME/.agents/skills` for user-wide skills.
See the
[OpenAI Codex skills documentation](
https://developers.openai.com/codex/build-skills) for details.


## What the skill teaches

- Write correct Clojure first, then convert it to idiomatic YAMLScript.
- Prefer YS functions such as `say`, `in?`, and `words` over their Clojure
  equivalents.
- Apply dot and colon chaining idiomatically.
- Validate every attempt before presenting it.


## Try it

Ask Codex:

> Use `$yamlscript` to write a program that groups a list of people by city,
> then validate the result.

[View the Codex skill source](
https://github.com/yaml/yamlscript/tree/v0/ai/codex)
