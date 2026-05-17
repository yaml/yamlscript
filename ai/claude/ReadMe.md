# YAMLScript Claude Code Plugin

A [Claude Code](https://claude.ai/code) plugin for writing idiomatic
YAMLScript, built through interactive teaching sessions with Ingy (the
creator of YAMLScript).


## Install

In Claude Code, run:

```
/plugin marketplace add yaml/yamlscript
/plugin install ys-skill@yamlscript
```

The skill is then available as `/ys-skill:yamlscript`.


## How It Works

YAMLScript has many valid syntax forms that compile to the same Clojure.
This skill encodes the best form for each situation, confirmed through
real corrections from the language creator.

The skill teaches Claude to:

- Write correct Clojure first, then convert to idiomatic YAMLScript
- Use YS-specific functions (`say`, `in?`, `words`, etc.) over Clojure
  equivalents
- Apply dot/colon chaining idiomatically
- Test every attempt before presenting it


## Directory Layout

```
ai/claude/
├── ReadMe.md               # This file
└── ys-skill/               # The plugin (installed by users)
    ├── .claude-plugin/
    │   └── plugin.json
    └── skills/
        └── yamlscript/
            └── SKILL.md    # The skill prompt
```
