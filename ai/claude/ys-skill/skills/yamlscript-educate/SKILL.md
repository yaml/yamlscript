---
name: yamlscript-educate
description: >
  Educate the yamlscript skill from a diff between Claude's attempt and
  Ingy's correction. Use when the user runs
  `/yamlscript-educate a.ys b.ys` — `a.ys` is Claude's version and
  `b.ys` is the corrected version. Clones the yamlscript repo locally,
  diffs the two files, extracts the lesson, and edits the yamlscript
  SKILL.md for review.
---

# yamlscript-educate

Maintenance skill that folds a single Claude-vs-Ingy correction into the
`ys-skill` plugin's `SKILL.md`. Run from any directory — the repo is
cloned locally for review.


## Invocation

```
/yamlscript-educate a.ys b.ys
```

- `a.ys` — the YAMLScript program Claude wrote
- `b.ys` — Ingy's corrected / idiomatic version


## Workflow

### 1. Parse arguments

Expect exactly two file paths. If either is missing or unreadable, print
the usage line above and stop. Do not guess file names.

### 2. Ensure the working checkout

Work in `./yamlscript` relative to the current directory. Never write
inside any other yamlscript checkout (e.g. one the user may be sitting
in).

```bash
REPO=./yamlscript
if [[ -d $REPO/.git ]]; then
  git -C "$REPO" pull --ff-only || true
else
  git clone --depth 1 https://github.com/yaml/yamlscript "$REPO"
fi
```

If `./yamlscript` exists but is not a yamlscript checkout (no
`ai/claude/ys-skill/skills/yamlscript/SKILL.md`), fail loudly and stop —
do not delete or overwrite it.

### 3. Locate the target file

```
SKILL=$REPO/ai/claude/ys-skill/skills/yamlscript/SKILL.md
```

This is the only file the skill is allowed to edit inside the clone.

### 4. Diff the two inputs

Run `diff -u a.ys b.ys` and also read both files end-to-end. Look for
every material change:

- Program tag changes (`!yamlscript/v0` → `!ys-0`)
- Function name swaps (`println` → `say`, `str` → interpolation,
  `slurp`/`spit` → `read`/`write`, `range` → `..`, etc.)
- Chaining style changes (block ↔ dot ↔ colon)
- Data-mode `::` introductions for maps, block scalars, nested data
- Top-down function order (`main` first)
- Default-arg usage instead of multi-arity
- Operator whitespace (`1..5` → `1 .. 5`, `a+b` → `a + b`)
- Lambda form (`fn(x): ...` vs `\(_ * 2)` vs `fn([x] ...)`)
- Anything already listed in SKILL.md's **Anti-Patterns** section

### 5. Optional semantic-equivalence check

If `ys` is on PATH (check `command -v ys`), run both files and compare
stdout. If they differ, say so — the correction is not purely stylistic
and any extracted rule should reflect that. Continue regardless; the
diff may still teach a lesson. Do not fail the skill on this check.

### 6. Extract the lesson(s)

For each material difference, answer two questions:

1. **Is this rule already in `SKILL.md`?** Grep the file. Consider
   near-matches — the rule may be present but weakly worded, missing an
   example, or located in the wrong section.
2. **If not, which section does it belong in?** The existing top-level
   sections are:
   - Setup
   - Workflow
   - Program Tag
   - YS vs Clojure Standard Library
   - Key Rules → Strings
   - Key Rules → Function Definitions
   - Key Rules → Function Calls
   - Key Rules → Control Flow
   - Key Rules → Chaining vs Variables vs Block Form
   - Key Rules → Operators & Chaining
   - Key Rules → Values & Data
   - Key Rules → Do Semantics
   - Key Rules → I/O, System & Namespaces
   - Anti-Patterns
   - Reference

   Pick the closest fit. Do not invent new top-level sections.

If the diff teaches nothing the skill does not already know — say so
and make no edit.

### 7. Edit `SKILL.md`

Edit only `$SKILL`. Follow the repo's style rules (from `CLAUDE.md`):

- Wrap lines at exactly 80 characters — count carefully.
- 2-space indentation for nested bullets.
- Prefer single quotes in examples.
- Match the surrounding bullet style (each bullet starts with `-` and is
  a short imperative or rule-plus-example).
- No trailing whitespace.
- Do not reflow unrelated paragraphs. Minimal diff.
- If adding an example, keep it short and testable with `$YS -pe` or
  `$YS -c -`.
- If the rule is a **don't**, add it to the `## Anti-Patterns` list in
  addition to (or instead of) the positive rule — follow existing
  `Do NOT …` phrasing.

Use the Edit tool on the file inside `$REPO`. Never edit a yamlscript
checkout at any other path.

### 8. Report back

Show the user, in this order:

1. The output of `diff -u a.ys b.ys`.
2. A short bullet list of the lessons you identified, each tagged as
   **new**, **reinforced**, or **already covered**.
3. The output of `git -C "$REPO" diff -- "$SKILL"` so the user sees
   exactly what the skill changed.
4. A closing note:
   > The updated SKILL.md is in `./yamlscript`. Review the diff there,
   > then commit and push manually when ready.

### 9. Out of scope for this skill

Do **not**, in this iteration:

- Add entries to `ai/claude/devel/ys-skill/sessions/*.yaml`.
- Run `skill-audit`.
- Bump `plugin.json` / `marketplace.json` versions.
- Create branches, commits, or pull requests.
- Push to any remote.

The user keeps review control. They will commit the SKILL.md change
themselves after inspecting it.


## Reference

Target file (inside the clone):

```
./yamlscript/ai/claude/ys-skill/skills/yamlscript/SKILL.md
```

Its tone, section layout, and bullet style are the template for every
edit this skill makes.
