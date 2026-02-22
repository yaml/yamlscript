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

## Key Rules

### Strings
- Single quotes unless interpolation or escapes needed
- `"Hello, $name!"` not `str('Hello, ' name '!')`
- `"Result: $(x * y)"` for expression interpolation
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
- CLI args that look like numbers are auto-converted — `num()` not needed
- `+` for simple concatenation at end of dot chain, not `str()`
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
- `main` with default args for CLI programs
- Define functions top-down: `main` first, then helpers in call
  order — this is idiomatic YAMLScript

### Function Calls
- Top level: mapping pair — `say: 'hello'`
- Inline: YeS form — `inc(x)` not `(inc x)`
- Prefer `a.b(c)` over `b(a c)` — dot chain from the receiver
  unless the receiver needs escaping (`{}`, `[]`, `""`, `''`)
- Scalar `if`: dot-chain the condition before it —
  `cond.if(then else)` not `if(cond then else)`

### Control Flow
- `if <cond>: <then-form> <else-form>` — always needs both forms
- Use `when` for one-armed conditional (no else)
- Two consecutive pairs in an `if` block are then and else — no
  keywords needed when both branches are single pairs:
  `if cond: \n  say: yes \n  say: no`
- `then:`/`else:` keys for clarity or multi-form bodies
- Consider reversing the condition to avoid `then:` — complex branch
  first (no keyword), simple branch as `else:` — often cleaner
- `else` not `:else` in `cond`
- `each` over `doseq` for side-effecting iteration
- `dotimes [_ n]:` — repeat n times ignoring the index; clearer than
  `each [_ (1 .. n)]:` when you don't need the iteration value
- `loop i 1, acc 0:` — loop with named bindings; use `recur` for
  tail recursion back to the loop head
- `recur` — tail-call back to enclosing `loop` or `defn`; multi-arg
  form: `recur: arg1 arg2` or `recur arg1: arg2`
- `for` body can be bare scalar — `=>:` not needed

### Chaining vs Variables vs Block Form

**Prefer block form** — it often adds clarity that chaining hides.
Do not default to chaining just because it is possible.
Chaining is fine for short, obvious pipelines; block form is better
for anything non-trivial, especially iteration and nested logic.

Avoid over-chaining. A long dot chain on one line is hard to read.
Aim to keep lines under 50 characters as a rough guide.

Options when a chain gets long:
- Use block form — nest the argument as an indented block
- Assign intermediate results to named variables
- Split before `.call(` onto continuation lines (but not before `:call`)

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
- Parentheses are not needed if the full operator expression is
  in a key or value position: `x =: a * b * c` is fine because
  `a * b * c` is the entire value
- `..` for inclusive ranges, not `range`
- `rng(x y)` — use only for char ranges: `rng(\\a \\z)`. For integer
  ranges, always use `..`: `1 .. 5` (forward) or `5 .. 1` (reverse).
  `\\a` is a Clojure char literal (backslash doubled in YAML block
  scalars); `C('a')` also works but is verbose.
- `%` = `rem` (remainder); `%%` = `mod` — prefer `%`; they differ only
  for negative numbers
- `.!` for falsey check (`falsey?`) — replaces `zero?` on mod
  results; YS truth: 0 and empty collections are also falsey.
  `x.!` combines nil-check and empty-check in one — use it instead
  of separate `nil?` + `empty?` guards
- Dot chaining for calls with args: `s.replace(/x/ '')`, `s1.anagram?(s2)`
- Colon chaining for zero-arg calls: `s:lc:frequencies:reverse`
- `obj.name` (dot without parens) is a **property/key lookup** — NOT
  a function call. Use `:name` to call a zero-arg function by name.
  Example: `.first` → key lookup (nil); `:first` → `(first obj)` (correct)
- Special postfix operators are NOT property lookups — they compile
  to explicit function calls and work in string interpolation:
  `.++` = `inc+`, `.--` = `dec+`, `.#` = `count`, `.!` = `falsey?`,
  `.?` = `truey?`, `.$` = `last`, `.@` = `deref`, `.>` = `DBG`,
  `.??` = `boolean`, `.!!` = `not`.
  Example: `$(i.++)` = `inc+(i)`, `$(xs.#)` = `count(xs)`
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
- `starts?(s prefix)` / `ends?(s suffix)` — string prefix/suffix
  tests; dot form: `s.starts?(prefix)`, `s.ends?(suffix)`
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
- `:S` — convert to string
- `_.0`, `_.1` — indexed access on implicit lambda arg `_`
- `x.(f*)` — shorthand for `x.apply(f)`
- `a .=: f(b)` — augmented assignment; works for `.`, `*`, `+`, `||`, etc.

### Values & Data
- `+` escape works ONLY at the very start of a YAML value plain
  scalar — it tells YS the rest of the scalar is a single expression.
  Anywhere else in an expression, `+` is the addition/concatenation
  operator:
  - `+[1 2 3]` — escape: vector literal (value starts with `[`)
  - `+"hello" + "world"` — escape: string concatenation expression
  - `+[0] + row` — escape: prepend zero to sequence
  - `sieve(xs) +[]` — NOT an escape: means `sieve(xs) + []`
    (vector addition, a no-op)
  - Whitespace after `+` is fine — useful for multi-line expressions:
    ```
    foo =: +
      [a] + [b]
    ```
- Keyword keys need `:` prefix: `+{:name "Alice", :age 30}`
- Flow maps need commas: `{a: 1, b: 2}`
- `=:` for assignment (replaces `def`/`let`)
- `x y =: 6 7` for multiple assignment
- `=>:` when a mapping pair is required but the value is a plain scalar
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

### I/O, System & Namespaces
- `read(path)` / `path:read` — read file contents;
  `write(path content)` — write content to file
- `say` / `warn` / `err` / `out` — print to stdout/stderr;
  `warn` and `err` go to stderr; `say` adds newline, `err` does not
- `die(msg)` — print error message to stderr and exit
- `read-line()` — read a line from stdin
- `trim(s)` — strip leading/trailing whitespace
- `fs-e` / `fs-f` / `fs-d` — file exists / is-file / is-dir;
  zero-arg so colon-chain: `path:fs-e`
- Namespace-qualified calls: `json/load(s)`, `json/dump(data)`,
  `http/get(url)`, `http/post(url opts)` — call with `/` separator


## Anti-Patterns

- Do NOT use `str()` for string building — use interpolation or `+`
- Do NOT use `(func arg)` Lisp style — use `func(arg)` or pair form
- Do NOT use `range` or `rng` when `..` works — write `1 .. 5` not `1..5`
- Do NOT use `println` — use `say`
- Do NOT use `:else` — use `else`
- Do NOT use `fn(x): body` inline — invalid YAML (`:` splits the expression)
- Do NOT use `fn([x] ...)` when `\(...)` with `_` suffices —
  prefer the shorthand for single-expression lambdas
- Do NOT start an expression with `[`, `{`, `"`, `'` etc. without
  a `+` prefix — YAML will interpret them as flow sequences/maps/strings
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
- Do NOT use `.get("key")` for map access — use `.key` property
  lookup when the key is a simple string

## Reference

Key docs in the YAMLScript repo:
- `doc/clj-to-ys.md` — Clojure to YS conversion tutorial
- `doc/cheat.md` — Quick syntax reference
- `doc/yes.md` — YeS expressions
- `doc/chain.md` — Dot chaining
- `doc/operators.md` — Operators

Session logs with confirmed examples: `skill/sessions/`
