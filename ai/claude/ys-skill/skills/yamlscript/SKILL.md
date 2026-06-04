---
name: yamlscript
description: >
  Write idiomatic YAMLScript code. Use when asked to write, convert, or
  review YAMLScript (.ys files). Converts Clojure to idiomatic YAMLScript
  using confirmed style rules and tested examples.
---

# YAMLScript Skill

## Setup

Ensure `ys` is available for testing:

```bash
[[ -x /tmp/ys-skill/bin/ys ]] ||
  curl -s https://yamlscript.org/install | PREFIX=/tmp/ys-skill bash
YS=/tmp/ys-skill/bin/ys
```

Optionally clone the source for looking up stdlib functions, DWIM
support, and docs:

```bash
[[ -d /tmp/ys-skill/yamlscript ]] ||
  git clone --depth 1 https://github.com/yaml/yamlscript \
    /tmp/ys-skill/yamlscript
# Key files:
#   core/src/ys/std.clj   — YS standard library
#   core/src/ys/dwim.clj  — functions with auto arg-placement
#   doc/                  — language documentation
```

## Workflow

1. **Write correct Clojure first** — Clojure is unambiguous; get the logic
   right before worrying about YS syntax
2. **Convert to YAMLScript** — apply the rules below
3. **Test every attempt** before presenting it:
   ```bash
   # Single-line expressions
   $YS -pe 'expr'
   # Multi-line programs
   $YS -c - <<<'!ys-0 ...'
   ```
4. **Iterate** until the output is correct and idiomatic
5. **Lint the source** with the `ys-lint.ys` script that ships next to this
   SKILL.md (same skill directory). Run it against every `.ys` file you
   wrote or edited:
   ```bash
   /path/to/skill/ys-lint.ys FILE...
   ```
   `ys-lint.ys` flags *possible* surface-form mistakes the compiler can't
   see because they vanish at the AST stage: `.nth(N)` vs `.N`,
   `.nth(var)` vs `.$var`, `x + 1` vs `.++`, `x - 1` vs `.--`,
   `then: nil` / `else: nil` vs `when` / `when-not`, lines over 79 cols.

   The linter matches against source text with regex, so every hit is a
   *candidate*, not a verdict. False positives are expected: a long line
   may be a literal task string the program can't shorten; an `x - 1`
   inside a generated string isn't a `.--` candidate; an identifier that
   happens to look like a pattern may not be one. Inspect every reported
   line, fix the real mistakes, and explicitly justify each hit you
   treat as a false positive. This step is required: the working program
   isn't done until you have walked every lint hit and either fixed it
   or accepted it with reason.

## Program Tag

- Always use `!ys-0` — the short idiomatic form
- `!yamlscript/v0` and `!yamlscript/v0/` are legacy — do not use
- `!ys-0` = code mode; `!ys-0:` = data mode

## YS vs Clojure Standard Library

Prefer YS stdlib functions (`ys.std`) over their Clojure equivalents —
they are more powerful and polymorphic (e.g. `reverse` works on strings,
`replace` defaults the replacement to `""`, `rng` works on chars).
If performance is a concern, fall back to the specific Clojure function
for that case.

Most `Math/*` functions are exposed in `ys.std` (`sqrt`, `sqr`,
`floor`, `abs`, `pow`, etc.). Drop the `Math/` prefix when a YS
builtin exists; it's more idiomatic.

## Common Mistakes

Patterns Claude gets wrong most often. Scan these before writing any YS.

### Use `if` for two-branch conditionals — `cond` only for 3+ branches

`cond` is **only** appropriate when there are three or more mutually
exclusive branches. Any time you have a single predicate plus an
`else:` (one real branch and one fallback), use `if` instead. This is
the single most common conditional mistake.

- `cond: x == 0: a / else: b` → `if x == 0: \n  then: a \n  else: b`
- `cond: pred: x / else: recur(...)` → `if pred: \n  then: x \n  else: recur(...)`

`if` can drop the `then:` and `else:` keys when both branches are
*pair-form* children (mapping entries), because YS reads the two
children positionally regardless of their keys. The keys can even
collide:

```
if (n % d) == 0:
  recur: quot(n d) d cnt.++
  recur: n d.++ cnt
```

This also lets the then-branch be a nested `if X:` pair while the
else-branch is an explicit `else:`:

```
if n >= 2:
  if (n % d) == 0:
    recur: ...
    recur: ...
  else: cnt
```

Bare-scalar branches (plain identifiers, calls, expressions) are
fragile in this position — YAML parses two bare siblings only in
specific contexts, and mixing a bare scalar with an `else:` mapping
entry is invalid YAML. When in doubt, keep `then:` and `else:`
explicit; only drop them when both branches are pair-form.

When both branches are bare scalars, a trailing `+` on the `if` line
folds the next two indented lines into one plain scalar that YS reads
as the remaining positional args of `if`:

```
if n == 1: +
  '1'
  factors(n).join(' x ')
```

Compiles to `(if (= n 1) "1" (str/join " x " (factors n)))`. Use this
when both branches are short bare expressions and the symmetry reads
better than `then:`/`else:` keys.

Inner conditionals nested inside a `cond` clause are usually
two-branch and should be `if`. Before writing `cond:`, count the
clauses: if it's two (one predicate + `else:`), rewrite as `if`. Three
or more clauses (excluding `else:`) keeps `cond`.

Scan every `cond:` in the file before finishing — if it has only one
non-`else:` clause, it's wrong.

### `=>:` only when no pair form works

A YAML mapping context — `defn` body, `do:` block, conditional
branch block, `loop` body, etc. — requires every line to be a
`key: value` pair. `=>:` is the fallback key when the expression
genuinely cannot be written as a pair.

**Use `=>:` for atomic values** (no other pair form exists):
- bare identifiers: `=>: x`, `=>: result`
- bare numeric/literal atoms: `=>: 42`, `=>: nil`, `=>: true`,
  `=>: :foo`
- bare interpolated strings: `=>: "$s$check"`
- bare data-collection literals: `=>: +[1 2 3]`, `=>: +{a: 1}`

**Restructure compound expressions into a pair:**

1. **Function call** → fn-call pair `name: args`
   - `=>: f(a b)` → `f: a b`
   - `=>: recur(i.++ b nx)` → `recur: i.++ b nx`
   - `=>: V+(re im)` → `V+: re im`
2. **Method chain** → chain-pair `receiver: .method(args)`
   - `=>: a.b(c).d(e)` → `a: .b(c).d(e)`
   - `=>: row.assoc(w best)` → `row: .assoc(w best)`
   - `=>: meta.from.split('/wiki/').$` → `meta.from: .split('/wiki/').$`
3. **Binary operator** → op-pair `lhs OP: rhs`
   - `=>: a + b` → `a +: b`
   - `=>: n == psum` → `n ==: psum`
   - `=>: is-thu || is-wed-leap` → `is-thu ||: is-wed-leap`
4. **`cond` default arm**: use `else:` not `=>:`

**Drop single-use indirection**: a `result =: expr` whose only use
is a trailing `=>: result` folds into a single trailing pair:
- `result =: r:sqr == n; =>: result` → `r:sqr ==: n`
- `result =: row.conj(s); =>: result` → `row: .conj(s)`

**Colon-chains must convert to dot-chains in chain-pair position** —
chain-pair only supports a leading `.`:
- `=>: stack:pop:pop.conj(x)` → `stack: .pop().pop().conj(x)`

**Op-pair / pair-form quirks**:
- `%:` does not parse (`Invalid symbol '%'`). Use the fn-call pair
  form `mod: a b` (or `rem: a b`) instead.
- A pair value cannot begin with a quoted string followed by more
  args. `format: '%+.4f' x y` fails to parse. Workarounds:
  - Promote the string into the key: `format '%+.4f': x y`
  - Force it into the value with `+`: `format: +'%+.4f' x y`

### Never write `x + 1` or `x - 1` — use `.++` / `.--`

Increment and decrement by 1 are common enough to have their own
postfix operators. Use them anywhere — assignment values, argument
positions, return values, loop bodies, string interpolation:

- `v + 1` → `v.++`
- `v - 1` → `v.--`
- `(3 * v) + 1` → `(3 * v).++`
- `recur: i + 1` → `recur: i.++`

`.++` and `.--` compile to `inc+` / `dec+` (polymorphic). This is the
single most-forgotten rule — scan every `+ 1` and `- 1` before
finishing.

### Never write `.nth(N)` or `.nth(bareVar)` — use `.N` / `.$var`

Index access has terse dot-forms that should be preferred over the
explicit `.nth(...)` call:

- `v.nth(0)` → `v.0`           (literal integer index)
- `v.nth(12)` → `v.12`
- `s.nth(0)` → `s.0`           (works on strings too)
- `parts.nth(2)` → `parts.2`
- `v.nth(i)` → `v.$i`          (bare variable index)
- `v.nth(idx)` → `v.$idx`
- `m.nth(ip)` → `m.$ip`

`.nth(expr)` is only correct when the index is a **computed
expression** — e.g. `v.nth(i.--)`, `v.nth((row * 4) + c)`,
`v.nth(i + g)`. The `.$var` form takes a single bare variable; it
does not accept compound expressions.

Scan every `.nth(` in the file before finishing — if the argument is a
literal integer or a bare variable, rewrite to the dot/dollar form.

### Use specific predicates over generic `.!` for numeric tests

When testing whether a number is zero, prefer `:zero?` (chain) or
`zero?(...)` (call) over `.!` or `== 0`. The specific predicate
documents intent: "is the remainder zero" vs "is this falsey".

- `(i % 5):zero?` over `(i % 5).!`
- `count.zero?` over `count == 0`
- `zero?(x - y)` when the expression has a natural prefix form

Reserve `.!` for cases where the expression is already busy and the
terse form aids readability, or where you genuinely want the broader
"falsey" semantics (nil, false, empty collections, empty strings).

### `else:` not `do:` for the else branch of `if`

When the then-branch is a single form and the else-branch is multiple
forms, introduce the else block with `else:`, not `do:`. `do:` compiles
but is not idiomatic.

### `when`/`when-not` for one-armed conditionals returning nil

If a branch of `if` or `cond` returns `nil`, the conditional is really
one-armed — use `when` (or `when-not`) instead. `when` returns nil when
the test is false, so the explicit nil branch is dead weight. A `cond`
with one real arm and a nil fallback is the loudest version of this
mistake.

- `cond: x.!: nil / else: real` → `when x: real`
- `cond: m: i / else: nil` → `when m: i`
- `if cond: form / else: nil` → `when cond: form`
- `when X.!` → `when-not X`

### `declare` is not needed in YAMLScript

YS resolves `defn` references across the whole file, so mutual
recursion works regardless of definition order. Don't reach for
`declare: name` — it's a Clojure habit and adds noise:

```
# correct — F is defined first and references M defined later
defn F(n):
  if n:zero?: 1 (n - M(F(n.--)))

defn M(n):
  if n:zero?: 0 (n - F(M(n.--)))
```

### No reserved symbols in YS or Clojure

Any symbol can be used as a local binding. Names that shadow stdlib
functions (`next`, `count`, `key`, `name`, `val`, `type`, `class`,
`first`, `last`, `rest`, `map`, `line`, `done`, etc.) are fine. Don't
invent abbreviations like `nxt`, `cnt`, `k`, or `done?` just to avoid
the stdlib name.

```
# correct
next =: next-board(b)
when next: recur(next)

# wrong reaching for `nxt` to avoid shadowing `next`
nxt =: next-board(b)
when nxt: recur(nxt)
```

Pick the clearest name from the domain. The only reason to avoid a
particular symbol in a scope is if you need to use the original value
in that same scope.

## Style Defaults

The choices below have no single right answer in YAMLScript. The skill
ships with the defaults listed here, but they are **overridable from a
project's `CLAUDE.md`**. If a project's `CLAUDE.md` contradicts a
default, follow the project.

These are stylistic only — anything in *Common Mistakes*, *Key Rules*,
or *Anti-Patterns* is not negotiable.

### Receiver-first vs bare-function form

When a function has an obvious "subject" argument (the thing being
extended, transformed, or queried), prefer the receiver-first dot
form:

- `m.assoc(:k v)` over `assoc(m :k v)`
- `xs.conj(x)` over `conj(xs x)`
- `s.split('/')` over `split(s '/')`

Use the bare-function form when arguments are co-equal (e.g.
`merge(a b c)`, `concat(xs ys zs)`) or when there is no natural
receiver.

To override: in `CLAUDE.md`, write *"prefer bare-function form
(`assoc(m k v)`) over dot-chain"*.

### Vectors of short strings

For a static vector of short word-like strings, prefer `qw(a b c)`
over `=:: ['a', 'b', 'c']`:

- `colors =: qw(red green blue)` over `colors =:: ['red', 'green', 'blue']`

`qw` produces a vector of strings. Use the data-mode literal when the
elements contain spaces or non-word characters.

### Default argument values

For a `defn` arg with a long default value, prefer setting it in the
body with `||=:` over a long signature line:

```
defn main(text=nil):
  text ||=: 'The quick brown fox jumps over the lazy dog'
```

over

```
defn main(text='The quick brown fox jumps over the lazy dog'):
```

Short defaults (numbers, short strings, keywords) belong in the
signature: `defn main(n=10):`.

### Block form vs chain for multi-arg calls

For a call with three or more substantial args, prefer block form
with one arg per line over a single-line chain:

```
concat:
  quicksort(less)
  vector(p)
  quicksort(more)
```

over

```
concat: quicksort(less) vector(p) quicksort(more)
```

Two args fit fine on one line.

## Key Rules

### Formatting
- **Lines must not exceed 79 columns.** This is a hard limit, not a
  suggestion. Target 20/40/60 columns as the natural "square" sizes for
  most lines. YAML/YS gives you many ways to split:
  - **Block form**: replace a chain with an indented block
  - **Intermediate variables**: assign a sub-expression to a name
  - **Plain scalar folding**: a plain (unquoted) YAML scalar folds at any
    whitespace — break before a binary operator and indent the
    continuation:
    ```
    user =: ENV.RC_USER ||
      die('set RC_USER (botpassword username)')
    ```
  - **Double-quoted line fold**: a `"..."` string can be split at any
    space — YAML folds the newline (and the continuation's leading
    whitespace) into a single space. Indent the continuation to read
    cleanly:
    ```
    say: "map my-add over pairs:
          $(map(my-add [1 2 3] [10 20 30]):joins)"
    ```
  - **Double-quoted backslash continuation**: a `"..."` string can be
    split with `\` at end of line, even when there's no whitespace to
    fold at. Useful for long URLs, identifiers, or any unbroken token:
    ```
    url =: "https://en.wikipedia.org/w/api.php?action=query\
            &titles=Rosetta_Code&format=json"
    ```
  - **Block scalars** (`|`, `>`): for multi-line literal text
- **End the file with exactly one newline.** No trailing blank line.
  The last byte should be one `\n` after the last code line, not two.

### Strings
- Single quotes unless interpolation or escapes needed
- `"Hello, $name!"` not `str('Hello, ' name '!')`
- `"Result: $(x * y)"` for expression interpolation
- `"Now: $now()"` for a bare function or method call. The shortened
  `$ident(args)` form works for plain identifiers (letters, digits,
  underscore, hyphen) and static calls like
  `"$System/currentTimeMillis()"`. Prefer it over `$(ident(args))`
  when the call is a single function or method on a bare name.
- Interpolation stops parsing the identifier at `?` or `!`, so
  predicate names break the shortened form: write
  `"$(all-equal?(xs))"`, not `"$all-equal?(xs)"` (which interpolates
  only `$all-equal` and leaves `?(xs)` as literal text). Reach for
  `$(...)` for operators, chains, anything beyond one call, or any
  identifier containing `?` or `!`.
- `say: |` with a multi-line block — all lines interpolated and printed
- `::` (double colon) is sugar for `! ` (mode-toggle tag).
  `a:: b` = `a: ! b` — toggles between code and data mode:
  - In code mode (default `!ys-0`), `::` switches value to data
  - In data mode, `::` switches value back to code
  - `say:: hello` — data mode: literal string `"hello"`, not
    variable lookup (quoted `'hello'` is already literal either way)
  - `say:: |` — data mode: literal block scalar (no interpolation)
  - `json/dump::` with indented YAML — build data structures
    natively instead of `json/dump: +{...}` with escaped maps
  - `http/post url::` — pass YAML maps as options
  - Inside a `::` data block, `key:: expr` toggles back to code:
    `model:: model` = YAML key `model` with the value of
    *variable* `model`
  - `content:: |` with `$var` — block scalar with interpolation
  - `::` only works on mapping pair values (key-value syntax).
    For sequence entries, use the explicit `!` tag:
    `- ! expr` to evaluate `expr` as code within data mode
- `!<fn>` tag — avoids an extra indent level: `each i xs: !say` instead
  of nesting `say:` as a separate pair inside the body
- CLI args that look like numbers are auto-converted — `num()` not needed.
  Do NOT defensively coerce with `:int` / `:N` either. `defn main(n=10):`
  is enough; `ethiopian(a b)` works with no coercion when `a`/`b` came
  from the command line.
  Two globals expose the raw and converted views:
  - `ARGV` — all CLI args as raw strings (no conversion)
  - `ARGS` — all CLI args with numeric-looking values converted
  Use `ARGV` when the task is *about* string handling of numeric-looking
  input (e.g. "increment a numerical string"); otherwise `ARGS` and
  named/positional params with defaults are fine.
- `+` for simple concatenation at end of dot chain, not `str()`
- `n * 'str'` — integer times string repeats it: `n * '  '` for an
  indent of `n` levels. Replaces `apply: str repeat(n '  ')`. Order
  doesn't matter for this case: `'str' * n` also works.
- Interpolation auto-stringifies — `"$x"` works for any value, no
  `str(x)` needed inside `"..."` or `$(...)`
- `uc1(s)` — capitalize first character; `uc(s)` — all uppercase
- `join(sep coll)` — join with separator; `join(coll)` — no separator
- `joins(coll)` — join with a single space
- `split` and `join` have their own arg-swapping (not in DWIM list)
- `qw(word1 word2 ...)` — quoted word list; creates a vector of
  strings without needing quotes around each word
- `words(s)` — split string on whitespace; colon chain: `text:words`
- `lines(s)` — split string on newlines; colon chain: `text:lines`
- `in?(x coll)` — membership test; works on strings, vectors, sets,
  maps. Dot form: `w.in?(fruits)`. Flipped: `has?(coll x)`
- `replace(s pat repl)` — replace all matches; supports `$1` groups
- `replace(s pat)` — remove all matches (replacement defaults to `""`)
- `replace1(s pat repl)` — replace first match only
- In scalar expressions, escape YAML-special sequences:
  - `:\` → literal `: ` (colon-space would trigger colon-chain)
  - ` \#` → literal ` #` (space-hash would start a YAML comment)

### Comments
- `# ...` — standard YAML comment to end of line. Use it between
  structures, after values, and at file top/bottom.
- A `#` comment terminates the surrounding YAML scalar, so it cannot
  appear inside a multi-line plain-scalar expression such as a
  dot-chain spread across lines.
- `\"..."` — YS expression-level comment. Opens with `\"`, closes at
  the next `"`. Use it to annotate steps inside a multi-line
  expression where `#` would break the scalar:
  ```yaml
  defn scramble(s):
    s          \"input string"
      .lc()    \"lowercase"
      .split() \"split into chars"
      .shuffle()
      .join()
      .uc1()   \"capitalize first char"
  ```
- `\"..."` constraints:
  - The body cannot contain `"` (no escape mechanism).
  - The body is still lexed by YAML, so YAML-special sequences must
    be escaped the same way they are in any plain scalar:
    `:\ ` for `: ` (colon-space), ` \#` for ` #` (space-hash). See
    Strings.
  - Not usable inside YAML string literals (`"..."`, `'...'`) or
    regex literals `/.../`.
  - Not usable as a standalone YAML block element — only inside an
    in-progress multi-line expression.

### Function Definitions
- `defn name(args):` form with parens
- Default args over multi-arity: `defn greet(name='World'):`
- For multi-line default text, use a top-level block scalar variable:
  ```yaml
  dflt =: |-
    line one
    line two
  defn main(text=dflt):
  ```
  Avoids the YAML plain scalar restriction that forbids `: ` inside
  default values written as `\n`-escaped double-quoted strings
- `defn-` for private helpers
- Destructuring in parameter lists: `defn score([a b]):` binds `a`
  and `b` to elements of a pair argument — saves an intermediate
  `a b =: pair` line. Works for both `defn` and `fn`.
- `main` with default args for CLI programs. Defaults should be
  values a user could actually type on the command line — strings
  and numbers, not vectors or maps. If `main` needs a collection,
  default a string and parse it in the body (see `ARGV`/`ARGS`).
- `main` arg-list shapes:
  - `main(name)` — exactly one named arg (auto-converted if numeric)
  - `main(_)` — exactly one arg, unnamed (arity matters, name doesn't)
  - `main(*)` — any number of args, unnamed
  - `main(*args)` — any number of args, named
- Define functions top-down: `main` first, then helpers in call
  order — this is idiomatic YAMLScript

### Function Calls
- Top level: mapping pair — `say: 'hello'`
- `a: b c` ≡ `a b: c` ≡ `a b c:` — the colon splits a call into
  before/after segments; choose the split that reads naturally.
  Promote the "subject" of a call before the colon when it makes
  the call read like English:
  - `write file: content` not `write: file content`
  - `assoc m: k v` not `assoc: m k v`
- **Higher-order function calls** — when the function arg is named,
  put it on the key side; the data flows to the value:
  - `apply str: seq(s)...` — applying `str` to the seq
  - `reduce f: init coll` — reducing with `f` over `init`/`coll`
  - `map double: coll` — mapping `double` over `coll`
- **Inline-defined function for an HOF** — put `_` where the function
  arg goes; define the function as the block value:
  ```
  reduce _ init coll:
    fn(acc x):
      ...body...
  ```
  This works for any HOF: `map`, `filter`, `reduce`, etc. The block
  value substitutes at the `_`.
- Inline: YeS form — `inc(x)` not `(inc x)`
- Prefer `a.b(c)` over `b(a c)` — dot chain from the receiver
  unless the receiver needs escaping (`{}`, `[]`, `""`, `''`)
- Scalar `if`: dot-chain the condition before it —
  `cond.if(then else)` not `if(cond then else)`
- `X OP: Y` at the pair level is sugar for `X OP Y`, for any binary
  operator. `a +: b` ≡ `a + b`; `(cond) &&: body` ≡ `cond && body`
  (body only runs if `cond` is truey); `(cond) ||: body` ≡
  `cond || body` (body only runs if `cond` is falsey). The `&&:`/`||:`
  forms overlap functionally with `when`/`when-not` but the mechanism
  is the operator's short-circuit, not a control structure.

### Control Flow
- `if <cond>: <then-form> <else-form>` — always needs both forms.
  **`if` is the default for two-branch conditionals.** Reach for `cond`
  only when there are 3+ branches — see Common Mistakes.
- Use `when` for one-armed conditional (no else); `when-not` is the
  inverted form (`when-not X` ≡ `when X.!`). See Common Mistakes for
  when to choose `when`/`when-not` over `if`/`cond`.
- `when+ expr:` — like `when`, but binds `_` to the truey value of `expr`
  inside the body. Use it to test-and-capture in one step:
  `when+ schema.'$ref': say: "-type: $(ref-sym(_))"`
- `.when(value)` — receiver acts as the test; returns `value` if truey,
  else nil. Replaces the `.if(value nil)` pattern:
  `only-ref?(s).when(ref-sym(s.'$ref'))` not
  `only-ref?(s).if(ref-sym(s.'$ref') nil)`
- `cond` returns nil when no clause matches — drop trailing `else: nil`
- `case` requires an explicit `else:` default arm. Unlike `cond` (returns
  nil), `case` throws `No matching clause: <value>` if no arm matches.
  A bare trailing form is parsed as another `key: action` pair, not a
  default — `else:` is required.
- `if` accepts three shapes:
  - **form / form** — two consecutive pairs, no keywords:
    `if cond: \n  say: yes \n  say: no`
  - **block / block** — both `then:` and `else:` required; using
    `then:` forces `else:`
  - **form / block** — bare then-form followed by an `else:` block.
    Do NOT use `do:` for the else block — `else:` is the idiomatic
    keyword (see Common Mistakes).
- When both branches are simple, prefer the tersest fit:
  - **Single-line pair**: `if cond: a b` when both forms parse as a
    single plain scalar — e.g. bare symbols, function calls, ranges:
    `if v == v2: v recur(v2)`, `if x:odd?: print('o') print('e')`.
  - **Single-line with `+` escape**: when the first form starts with a
    YAML syntax char (`'`, `"`, `[`, `{`, etc.), add `+` to the front:
    `if x:odd?: +'odd' 'even'`, `if found: +match 'none'`.
  - **Chain form**: `cond.if(a b)` when the condition reads well as a
    receiver and you're not already in a mapping-pair context:
    `x:odd?.if('odd' 'even')`.
  - **Two-pair form** (newlines): when either branch is too long to
    inline, fall back to `if cond: \n  a-form \n  b-form`.
- Consider reversing the condition to avoid `then:` — complex branch
  first (no keyword), simple branch as `else:` — often cleaner
- `else` not `:else` in `cond`
- `each` over `doseq` for side-effecting iteration
- `dotimes [_ n]:` — repeat n times ignoring the index; clearer than
  `each [_ (1 .. n)]:` when you don't need the iteration value
- `loop i 1, acc 0:` — loop with named bindings (no surrounding
  brackets). Same bracket-free form works for `let`, `each`, `for`,
  `dotimes`, `when-let`, `if-let`, `with-open`, etc. (any binding
  form). Use `recur` for tail recursion back to the loop head.
- `recur` — tail-call back to enclosing `loop` or `defn`; multi-arg
  form: `recur: arg1 arg2` or `recur arg1: arg2`
- `for` body can be bare scalar — `=>:` not needed

### Chaining vs Variables vs Block Form

**Prefer block form** — it often adds clarity that chaining hides.
Do not default to chaining just because it is possible.
Chaining is fine for short, obvious pipelines; block form is better
for anything non-trivial, especially iteration and nested logic.

Avoid over-chaining. A long dot chain on one line is hard to read.
Aim to keep chained lines short — 20-60 columns is the natural
"square" range. Never exceed 79 columns (see Formatting in Key Rules).

Options when a chain gets long:
- Use block form — nest the argument as an indented block
- Assign intermediate results to named variables
- Split before `.call(` onto continuation lines (but not before `:call`)
- A YAML plain scalar can be folded onto multiple lines at any
  whitespace — the value is still a single expression. For readability,
  put a binary operator like `||` or `&&` at the end of the first line
  and indent the continuation:
  ```
  user =: ENV.RC_USER ||
    die('set RC_USER (botpassword username)')
  ```
  This is a stylistic choice, not a syntactic rule.

Example — chained vs block form for iterating with a nested function:

```yaml
# Chained — terse but opaque
say: fn([x] sum(digits(x))).iterate(n).drop-while(\(_ >= 10)):first

# Block form — each step is named, reads top-to-bottom
defn main(n=493): !say
  first:
    drop-while ge(10):
      iterate _ n:
        fn(x):
          sum: digits(x)

# Middle ground — intermediate variable + short chain
words =: text:lc.split(/\s+/)
pairs =: words:frequencies.sort-by(val):reverse
```

### Operators & Chaining
- **Binary operators require whitespace on both sides** — `1 .. 5` not
  `1..5`; `a + b` not `a+b`; `a * b` not `a*b`. This applies to all
  binary operators: `..` `+` `-` `*` `/` `||` `&&` `=~` `!~` `%`
  `**` etc. Omitting whitespace may sometimes work but is not idiomatic
  and may break in future versions. Exception: `.` (dot chain) does not
  need whitespace.
- Do NOT mix different operators without parentheses:
  - `a * b * c` — OK (same operator)
  - `a * b + c` — NOT OK
  - `(a * b) + c` — OK
- A chain of the same comparison operator means variadic — not
  nested: `a >= b >= c` is `(>= a b c)`, meaning `a ≥ b AND b ≥ c`,
  not `(a >= b) >= c`. Same for `==`, `<`, `<=`, `>`, `!=`.
- **Do not parenthesize a binary expression that stands alone as
  one side of a key/value pair.** The pair itself delimits the
  expression, so wrapping parens are pure noise:
  - `(r > 180): r - 360` → `r > 180: r - 360`
  - `x =: (a * b * c)` → `x =: a * b * c`
  - `cs =~ /[0-9]/: I(cs)` (already correct — no parens needed)

  This applies to both sides of the pair. Parens are still needed
  when the expression is *not* standalone — e.g. when it feeds a
  chain like `(d < 10).if(...)` or groups mixed operators like
  `(dir == 'w') && (row > 0):`.
- `..` for inclusive ranges, not `range`
- `rng(x y)` — use only for char ranges: `rng(\\a \\z)`. For integer
  ranges, always use `..`: `1 .. 5` (forward) or `5 .. 1` (reverse).
  `\\a` is a Clojure char literal (backslash doubled in YAML block
  scalars); `C('a')` also works but is verbose.
- `%` = `rem` (remainder); `%%` = `mod` — prefer `%`; they differ only
  for negative numbers
- `.!` for falsey check (`falsey?`) — YS truth: 0 and empty
  collections are also falsey. `x.!` combines nil-check and
  empty-check in one — use it instead of separate `nil?` + `empty?`
  guards. For "is this number zero" prefer `:zero?` / `zero?(...)` —
  see Common Mistakes.
- Dot chaining for calls with args: `s.replace(/x/ '')`, `s1.anagram?(s2)`
- Colon chaining for zero-arg calls: `s:lc:frequencies:reverse`
- `obj.name` (dot without parens) is a **property/key lookup** — NOT
  a function call. Use `:name` to call a zero-arg function by name.
  Example: `.first` → key lookup (nil); `:first` → `(first obj)` (correct)
- `obj.'key'` — quoted property lookup for keys that aren't valid bare
  identifiers (start with `$`, contain `-`, etc.):
  `schema.'$ref'`, `schema.'$defs'` not `schema.get('$ref')`
- `obj.$var` — dynamic property/index lookup; the runtime value of
  `$var` is used as the key. Works on maps (`m.$key`) and vectors
  (`v.$i`). Useful when the key/index is computed:
  ```
  key =: "${typ}token"
  data: .query.tokens.$key   # map lookup
  word =: words.$idx          # vector index
  ```
  Only takes a bare variable — for computed indices use `.nth(expr)`:
  `v.$(i - 1)` does not work; write `v.nth(i - 1)` instead. Inside a
  dot chain that starts the value, split with
  `data: .query.tokens.$key` rather than `=>: data.query.tokens.$key`.
- `obj.N` — literal-index lookup (the property name is the literal
  number). Works on vectors and strings: `v.0`, `v.12`, `s.3`. Use
  this for any constant index — `v.nth(N)` is verbose for literals.
  Inside `\(...)` lambdas, `_.0`, `_.1`, `_.2`, ... index the
  implicit arg.
- **Choose the right index form**:
  - literal index → `v.N` (e.g. `v.0`, `emp.2`)
  - bare variable → `v.$var` (e.g. `v.$i`, `units.$idx`)
  - computed expression → `v.nth(expr)` (e.g. `v.nth(i.--)`,
    `v.nth((row * 4) + c)`, `v.nth(w - wt)`)
  Never write `.nth(N)` or `.nth(bareVar)` — the dot/dollar forms
  are tighter.
- Property lookup is nil-safe — `nil.foo` returns `nil` (no NPE).
  A chain like `data.error.code` yields `nil` if `error` is missing,
  so you don't need to guard each step. Combine with `when` for
  presence checks: `when err.code == 'maxlag': ...` works even when
  `err` itself is `nil`.
- Special postfix operators are NOT property lookups — they compile
  to explicit function calls and work in string interpolation:
  `.++` = `inc+`, `.--` = `dec+`, `.#` = `count`, `.!` = `falsey?`,
  `.?` = `truey?`, `.$` = `last`, `.@` = `deref`, `.>` = `DBG`,
  `.??` = `boolean`, `.!!` = `not`.
  Example: `$(i.++)` = `inc+(i)`, `$(xs.#)` = `count(xs)`
- Postfix forms compile to the polymorphic `ys.std/<op>+` variant
  (`.++` → `inc+`, `.--` → `dec+`, etc.). Prefer them for readability,
  and especially in argument positions: `m.--.ack(1)` chains cleanly,
  whereas `ack((m - 1) 1)` needs paren-grouping so `m - 1` doesn't
  read as two args. In a tight numeric loop where raw speed dominates,
  fall back to the colon-chain forms `:inc` / `:dec` which call
  `clojure.core/inc` / `clojure.core/dec` directly.
- Use `:sqr` for `** 2` and `:cube` for `** 3` — `x:sqr` not `x ** 2`,
  `x:cube` not `x ** 3`. Works in any position: `1.0 / _:sqr`,
  `n:cube + 1`, etc.
- `\(_ * 2)` for inline lambdas — prefer over `fn([x] x * 2)` for
  single-expression bodies.
  Use `fn` only when you need destructuring or multiple args that `_`
  can't express.
  Never `fn(x): body` — invalid inline (`:` splits the expression)
- `_` placeholder when collection arg should come last in a chain,
  or to mark where a block value will be substituted
- DWIM auto-placement: the following functions detect arg types at
  runtime and swap order when needed — chain from any arg naturally:
  `apply` `chop` `cons` `contains?` `drop` `drop-last` `drop-while`
  `every?` `escape` `filter` `filterv` `format` `interpose` `keep`
  `map` `mapcat` `mapv` `not-any?` `nth` `partition` `random-sample`
  `re-find` `re-matches` `re-seq` `reduce` `remove` `repeat`
  `replace` `some` `sort` `sort-by` `split-at` `split-with` `take`
  `take-last` `take-while`
  For functions NOT in this list, put the collection as receiver or
  use `_`. For performance-critical code, use `_` to skip the check.
- `map-indexed(f coll)` — not DWIM; use `_` placeholder:
  `coll.map-indexed(f _)` or `coll.map-indexed(vector _)`
- `group-by(f coll)` — group items by function result
- `partition-by(f coll)` — split when function result changes
- `grep(P C)` — not in DWIM list but has own arg-swapping; P can be
  a regex (`re-find`), function (`filter`), or value (`=`); chain
  naturally: `coll.grep(regex)`, `coll.grep(fn?)`, `coll.grep(val)`
- `qr("pat")` — build a regex from a string (with interpolation):
  `qr("[$chars]")`. Use when `/.../` literals can't help (they don't
  interpolate). Prefer `qr` over Clojure's `re-pattern`.
- `starts?(s prefix)` / `ends?(s suffix)` — string prefix/suffix
  tests; dot form: `s.starts?(prefix)`, `s.ends?(suffix)`
- `a` is YS's alias for `identity` — returns its argument unchanged.
  Useful as a no-op transform (`map a coll`) or to satisfy a callback
  signature.
- For atoms, use the bangless aliases `swap` and `reset` instead of
  Clojure's `swap!` and `reset!`. `swap! buf: constantly(b2)` becomes
  `swap buf: constantly(b2)`; `reset! a: 0` becomes `reset a: 0`.
- Named operator functions — usable as first-class values or via dot
  syntax (e.g. `6.mul(7)`):
  - Arithmetic: `add` `sub` `mul` `div`
  - Comparison: `lt` `gt` `le` `ge` `eq` `ne`
  - Logical: `and` `or`; YS truth variants: `and?` (`&&&`),
    `or?` (`|||`) — use YS falsey semantics (0/empty = false).
    `(a ||| b)` = use `a` if truey, else `b` (like `a || b` in JS)
  - Regex: `s =~ /pat/` for match, `s !~ /pat/` for no-match
  - For simple inline comparisons, use symbolic operators directly:
    `limit > 2` not `limit.ge(2)`.
    Named forms (`ge`, `lt`, etc.) are primarily for creating predicate
    values to pass to higher-order functions like `filter`, `drop-while`,
    `take-while`.
  - Called with 1 arg, comparison operators return predicates:
    `ge(n)` → `(fn [x] (>= x n))`, `lt(n)` → `x < n`, etc.
    Useful with `filter`, `drop-while`, `take-while`, `remove`
- `f * g` — left-to-right function composition; applies `f` first
  then passes result to `g`. Example: `first * say` = get first,
  then print it. In block form, combine consecutive single-word keys
  with `*` to avoid nesting: `lc * say: value` instead of
  `say:\n  lc: value`
- `f + arg` — when `f` is a function, `+` partially applies it:
  `map + uc1` = `(partial map uc1)`, a function that maps `uc1` over
  a collection. Combine with `*`: `(map + uc1) * joins * say`
- `f(coll*)` — splat spreads collection as variadic args: `min(nums*)`
- `.#` — count/length operator, shorthand for `:count`
- `:S` — convert to string (alias for `str`)
- `:V` — convert to vector (alias for `vec`)
- `:V+` — wrap as vector (alias for `vector`); `x:V+` ≡ `[x]`
- `:I` — parse string to integer (alias for `parse-long`)
- `:N` — parse string to number, int or float (handles `'42'` and `'2.1415'`)
- `_.0`, `_.1` — indexed access on implicit lambda arg `_`
- `x.(f*)` — shorthand for `x.apply(f)`
- `x OP=: expr` — augmented assignment, sugar for `x =: x OP expr`.
  Works for any binary operator: `.=:` (chain into receiver), `+=:`,
  `*=:`, `||=:`, etc. Example: `nums .=: words().map(N)` replaces
  `nums` with `nums.words().map(N)`.

### Values & Data
- For purely literal collections (no code inside), prefer the
  data-mode toggle `=::` over `+`-escaped code-mode literals.
  YAML is good at data; let it do that work:
  - `a =:: [1, 2, 3]` — flow seq, data mode (preferred for literals)
  - `a =: +[1 2 3]` — code-mode vector literal (use when the
    collection mixes in computed values, e.g. `+[0] + row`)
- **`+` escape** — needed when the first character of a value would
  otherwise be a YAML syntax character (`[`, `{`, `"`, `'`, `|`, `>`,
  `!`, `&`, `*`). It forces the entire value to parse as a single plain
  scalar; YS then strips the `+` and reads the rest as code.

  Two distinct reasons `+` may be needed:
  1. **YAML-invalid without it.** `key: 'a' 'b'` — YAML sees `'a'` end
     and `'b'` dangle. `key: +'a' 'b'` makes the whole `+'a' 'b'` a
     plain scalar.
  2. **YAML-valid but YS-rejected.** `key: [b c]` — valid YAML (flow
     sequence value), but YAMLScript **forbids flow collections and
     block sequences at code-mode value positions by design**. Code
     mode only needs scalars and block mappings; flow forms are
     reserved for use as vector/map literals *via* `+`-escape. So
     `key: +[b c]` is the canonical form.

  **`+` is only needed at the START of a value.** Once the value is a
  plain scalar expression, flow forms inside it are fine as arguments:
  `foo([b c])`, `map(double [1 2 3])`, `assoc(m :k [1 2])` all parse
  without `+`. The brackets are mid-expression, not at the value start.

  `+` works ONLY at the very start of a value plain scalar — anywhere
  else in an expression, `+` is addition/concatenation:
  - `+[1 2 3]` — escape: vector literal
  - `+"hello" + "world"` — escape on leading `"`, then `+` is concat
  - `+[0] + row` — escape on leading `[`, then `+` is concat
  - `sieve(xs) +[]` — NOT an escape: means `sieve(xs) + []`
    (vector addition, a no-op)
  - Whitespace after `+` is fine — useful for multi-line expressions:
    ```
    foo =: +
      [a] + [b]
    ```
- Keyword keys need `:` prefix: `+{:name "Alice", :age 30}`
- Flow maps need commas: `{a: 1, b: 2}`
- **Set literals**: write `\{a b c}`, not Clojure's `#{a b c}` (the
  `#` starts a YAML comment). Use `\{}` for an empty set. `hash-set(...)`
  also works but is verbose:
  - `seen =: \{}` — empty set
  - `s =: \{:a :b :c}` — three-element set
- **Special float literals**: write `\\Inf`, `\\-Inf`, and `\\NaN` for
  positive infinity, negative infinity, and NaN. The `\\` escape
  stands in for `#` (Clojure's `##Inf` syntax), which YAML would
  otherwise treat as a comment.
- **Single-character casting functions** in `ys.std`:

  | Fn | Casts to     | Fn | Casts to         |
  |----|--------------|----|------------------|
  | B  | Boolean      | M  | Map              |
  | C  | Character    | N  | Number           |
  | D  | Atom deref   | O  | Ordered map      |
  | F  | Float        | S  | Set (not String) |
  | I  | Integer      | T  | Type-name string |
  | K  | Keyword      | V  | Vector           |
  | L  | List         |    |                  |

  `L+`, `M+`, `O+`, `V+` are variadic variants that build the
  collection from multiple args. Prefer these single-letter forms
  over `long(...)`, `int(...)`, etc.: `I(sqrt(n))` is idiomatic.
- `=:` for assignment (replaces `def`/`let`)
- `x y =: 6 7` for multiple assignment
- `=>:` only when no pair form works: bare identifiers, atoms,
  interpolated strings, data-collection literals (`+[1 2 3]`,
  `+{a: 1}`). For compound expressions, restructure into a pair —
  fn-call `f: args`, chain `x: .m(a)`, or op `a +: b`. See Common
  Mistakes.
- `? expr : value` — YAML complex key syntax; lets a multi-line
  expression serve as the key of a mapping pair. Useful when a
  pipeline is too long to fit before the `:` of a block pair:
  ```yaml
  ? each row
    next-row
      .iterate([1])
      .take(n)
  : say: row:joins
  ```
- Inside YeS expressions (inside parens), `[...]` needs no `+` escape

### Do Semantics
- Top-level, `defn`, `fn` bodies have implicit `do` — rarely need `do:` explicitly
- YS code blocks are ASTs not mappings — duplicate keys are valid

### Eval
- `eval(s)` / `s:eval` — parse a string as YS source and run it,
  returning the value of the final expression. Useful when user input
  must execute as code: in a 24-game task, the player's expression
  `'(8 - 2) * (7 - 3)':eval` returns `24`. The string is unrestricted
  YS, not a sandboxed arithmetic subset, so use it only on trusted
  input.

### I/O, System & Namespaces
- `read(path)` / `path:read` — read file contents;
  `write(path content)` — write content to file
- `say` / `print` / `out` / `warn` / `err` — write to stdout/stderr.
  `say` adds a newline; the others do not. `warn` and `err` go to
  stderr; the rest go to stdout. `print` and `out` are synonyms (both
  are `clojure.core/print` with auto-flush); prefer `print` when it
  stands alone, `out` when chaining or pairing with `err`.
- `die(msg)` — print error message to stderr and exit
- `read-line()` — read a line from stdin
- `IN` — stdin handle for `read`: `read: IN` instead of
  `slurp: System/in`
- `trim(s)` — strip leading/trailing whitespace
- `sleep(n)` / `sleep: n` — pause for `n` seconds. Use the builtin
  rather than shelling out via `bash-out: "sleep $n"`.
- `bash-out(cmd)` / `cmd:bash-out` — run a shell command and return
  stdout as a string. Pair with a `|` block scalar for multi-line
  scripts; bash continues naturally across newlines after `&&`, `||`,
  or `|`, so no trailing `\` is needed:
  ```
  cmd =: |
    cd work &&
      git rm -fq -- '$rel' &&
      git commit -q -m 'Push $rel' -- '$rel'
  bash-out: cmd
  ```
  When the command is used only once, pass the heredoc directly:
  `bash-out: |` followed by the indented script.
- Filesystem operations live in the `fs/` namespace. When a program
  needs to do anything with the filesystem (test, read metadata, copy,
  move, remove, etc.), consult https://yamlscript.org/doc/ys-fs/ for
  the full inventory. Common entries:
  - predicates: `fs/e` (exists?), `fs/f` (file?), `fs/d` (dir?),
    `fs/l` (link?), `fs/r`/`fs/w`/`fs/x` (perms), `fs/z` (empty?)
  - getters: `fs/abs`, `fs/basename`, `fs/dirname`, `fs/cwd`,
    `fs/ls`, `fs/glob`, `fs/which`, `fs/mtime`
  - mutators: `fs/cp`, `fs/mv`, `fs/rm`, `fs/rm-r`, `fs/mkdir-p`,
    `fs/touch`
  Predicates and getters also have `fs-` aliases interned into
  `ys::std` (e.g. `fs-e`, `fs-d`) because those came first, before
  the `fs/` library existed. Mutators are `fs/`-only. Prefer the
  `fs/` form for new code; both work for predicates.
- Namespace-qualified calls: `json/load(s)`, `json/dump(data)`,
  `http/get(url)`, `http/post(url opts)` — call with `/` separator


## Anti-Patterns

- Do NOT use `=>:` for compound expressions — restructure into a
  pair: `=>: a.b.c` → `a: .b.c`; `=>: f(a b)` → `f: a b`;
  `=>: a == b` → `a ==: b`. For a `cond` default arm use `else:`,
  not `=>:`.
- Do NOT write `x + 1` or `x - 1` — use `.++` and `.--`: `i.++` not
  `i + 1`, `(3 * v).++` not `((3 * v) + 1)`, `n.--` not `n - 1`.
  Works in chains, args, interpolation, anywhere.
- Do NOT write `x ** 2` or `x ** 3` — use `:sqr` and `:cube`:
  `_:sqr` not `_ ** 2`, `n:cube` not `n ** 3`
- Do NOT use `do:` for the else branch of `if` — use `else:`
- Do NOT use `cond` for two-branch conditionals — `cond` is for 3+
  branches. One predicate + `else:` is always `if`: `cond: p: a / else: b`
  → `if p: \n  then: a \n  else: b`. Scan every `cond:` and count
  non-`else:` clauses; if it's one, rewrite as `if`.
- Do NOT write lines longer than 79 columns. Use block form,
  intermediate variables, plain scalar folding, or double-quoted `\`
  continuation to split (see Formatting in Key Rules).
- Do NOT add `:int` / `:N` coercion to numeric CLI args in `main`.
  YS auto-converts numeric-looking CLI args; coercion is dead code.
- Do NOT use `str()` for string building — use interpolation or `+`
- Do NOT use `(func arg)` Lisp style — use `func(arg)` or pair form
- Do NOT use `range` or `rng` when `..` works — write `1 .. 5` not `1..5`
- Do NOT use `println` — use `say`
- Do NOT use `:else` — use `else`
- Do NOT use `:name(args)` — colon chain is zero-arg only. If you
  need to pass args, use the dot form `.name(args)`. Writing
  `xs:join(", ")` fails to compile with a confusing error like
  `Compile error: nth not supported on this type: PersistentArrayMap`.
  Write `xs.join(", ")` instead. Zero-arg `xs:join` (no parens) is fine.
- Do NOT use `fn(x): body` inline — invalid YAML (`:` splits the expression)
- Do NOT use `fn([x] ...)` when `\(...)` with `_` suffices —
  prefer the shorthand for single-expression lambdas
- Do NOT start a value with `[`, `{`, `"`, `'`, `|`, `>`, `!`, `&`, `*`
  without a `+` prefix. Either YAML rejects it, or YAML accepts it but
  YS rejects flow collections / block sequences at code-mode value
  positions (by design — see Values & Data). Use `+[...]` / `+{...}`
  for code-mode literals. Note: this only applies at the START of a
  value — `foo([b c])` is fine because the `[` is mid-expression.
- Do NOT use `+` mid-expression to "escape" — `+` is only an escape
  at the start of a value plain scalar; elsewhere it means addition.
  `sieve(xs) +[]` is vector addition (a no-op), not an escaped `[]`
- Do NOT use `!yamlscript/v0` — use `!ys-0`
- Do NOT use named comparison operators (`ge`, `lt`, etc.) for simple
  inline comparisons — write `limit > 2` not `limit.ge(2)`.
  Reserve named forms for use as predicates passed to higher-order
  functions (`filter ge(10)`, `drop-while lt(0)`, etc.)
- Do NOT guess without testing — run `$YS -pe` or `$YS -c -` first
- Do NOT define helpers before `main` — `main` must always be first;
  define helpers below in call order (top-down style)
- Do NOT use `+{...}` to build maps passed to functions — use
  `fn::` data mode when the map is static or mostly static
- Do NOT use `str()` for multi-line text — use `:: |` block scalar
  with `$var` interpolation
- Do NOT use `slurp`/`spit` — use `read`/`write`
- Do NOT use `.get(...)` for index/key access — use property lookup:
  - `.key` for a simple string/symbol key (`schema.tokens`)
  - `.'key'` for a non-bare-identifier key (`schema.'$ref'`)
  - `.N` for a literal numeric index (`v.0`, `emp.2`)
  - `.$var` when the index/key is a variable holding the value at
    runtime (`v.$i` not `v.get(i)`)
- Do NOT write `.nth(N)` or `.nth(bareVar)` — use `v.N` for literal
  indices and `v.$var` for bare-variable indices. Reserve `.nth(...)`
  for computed expressions (`v.nth(i.--)`, `v.nth((r * 4) + c)`).
- Do NOT write `.if(value nil)` — use `.when(value)` instead, which
  returns `value` if the receiver is truey and nil otherwise
- Do NOT end a `cond` with `else: nil` — `cond` returns nil by
  default when no clause matches
- Do NOT use `apply: str repeat(n '  ')` for string repetition —
  use `n * '  '`
- Do NOT wrap interpolated values in `str()` — `"$v"` and `"$(expr)"`
  auto-stringify any value
- Do NOT use `#` to annotate steps inside a multi-line dot-chain — it
  terminates the YAML scalar. Use `\"..."` instead.

## Reference

Key docs in the YAMLScript repo:
- `doc/clj-to-ys.md` — Clojure to YS conversion tutorial
- `doc/cheat.md` — Quick syntax reference
- `doc/yes.md` — YeS expressions
- `doc/chain.md` — Dot chaining
- `doc/operators.md` — Operators

Session logs with confirmed examples: `skill/sessions/`
