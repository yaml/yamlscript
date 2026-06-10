# YAMLScript AI Skills

Skills that teach AI coding agents to write idiomatic YAMLScript, built
through interactive teaching sessions with Ingy (the creator of
YAMLScript).

The same skill is published for two agents:

- `claude/` - a [Claude Code](https://claude.ai/code) plugin
- `codex/` - an [OpenAI Codex](https://developers.openai.com/codex)
  skill

See each directory's `ReadMe.md` for install instructions.


## Single Source of Truth

The Claude skill (`claude/ys-skill/skills/yamlscript/`) is the canonical
source. The Codex skill is identical content with a few agent-name
tokens rewritten (`Claude` -> `Codex`, `CLAUDE.md` -> `AGENTS.md`), so
it is generated rather than maintained separately.

After editing the Claude `SKILL.md` or `ys-lint.ys`, regenerate the
Codex skill and commit both. From the repo root:

```
make ai-skill-generator
```

That target runs `ai/bin/gen.ys` (a YAMLScript program) which reads the
Claude skill and writes the Codex copy under
`codex/.agents/skills/yamlscript/`.
