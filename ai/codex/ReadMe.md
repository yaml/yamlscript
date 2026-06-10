# YAMLScript Codex Skill

An [OpenAI Codex](https://developers.openai.com/codex) skill for writing
idiomatic YAMLScript, built through interactive teaching sessions with
Ingy (the creator of YAMLScript).

This skill is generated from the canonical Claude skill in
`ai/claude/`, so the two stay in sync. Do not edit the files under
`.agents/skills/yamlscript/` by hand; edit the Claude skill and run
`make ai-skill-generator` (see `ai/ReadMe.md`).


## Install

Copy the skill into a directory Codex scans for skills. For all your
projects:

```
cp -r .agents/skills/yamlscript ~/.agents/skills/
```

Or, for a single repo, copy it into that repo's `.agents/skills/`.

The skill is then available in Codex via `/skills` (or type `$` to
mention it).


## How It Works

YAMLScript has many valid syntax forms that compile to the same Clojure.
This skill encodes the best form for each situation, confirmed through
real corrections from the language creator.

The skill teaches Codex to:

- Write correct Clojure first, then convert to idiomatic YAMLScript
- Use YS-specific functions (`say`, `in?`, `words`, etc.) over Clojure
  equivalents
- Apply dot/colon chaining idiomatically
- Test every attempt before presenting it


## Directory Layout

```
ai/codex/
├── ReadMe.md                       # This file
└── .agents/skills/
    └── yamlscript/
        ├── SKILL.md                # The skill prompt (generated)
        ├── ys-lint.ys              # The linter (generated copy)
        └── agents/
            └── openai.yaml         # Codex UI metadata
```
