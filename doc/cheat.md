---
title: YS Cheat Sheet
talk: 0
---


### YS Fundamentals

* Valid YS code is always valid YAML
* YS has 3 modes: code, data, and bare
  * Code mode data is treated as code (can toggle to data mode)
  * Data mode data is treated as data (can toggle to code mode)
  * Bare mode data is treated as data (cannot toggle; always normal YAML)
* YS files must start with a YS (YAMLScript) tag:
  * `!YS-v0` - Start in code mode
  * `!YS-v0:` - Start in data mode
  * No tag - Start in bare mode (plain YAML; no code evaluation)
  * Initial tagged pair of `!YS v0:` is an alternative; starts in bare mode
* YS code mode always uses these YAML forms:
  * Block mappings (normal indented mappings; `: ` separated pairs)
  * Plain scalars (no quotes)
  * Quoted scalars (single or double or literal (`|`))
* Theses YAML forms are NOT allowed in code mode:
  * Flow mappings and sequences (`{}` and `[]`)
  * Block sequences (lines starting with `- `)
  * Folded scalars (`>`)
* All YAML forms are allowed in data mode
* `!` tag toggles between code and data mode
* `a:: b` is sugar for `a: ! b` in mapping pairs
* Use `=>: x` to write `x` as a mapping pair in code mode

The following examples are in code mode unless otherwise noted.


### Assignment

The space before the `=` is required.

```
hello =: 'Oh hello'
a b c =: -[1 2 3]  # Destructuring assignment
```


### YS expression escapes

YS expressions need to be written as valid YAML scalars.
When an expression starts with YAML syntax characters like `{`, `[`, `*`, `#`
then its not a valid YAML scalar.
Also expressions that have stuff after a quoted string (`''` `""`) are not valid
YAML.

You can turn text into a valid YAML plain scalar by prefixing it with a dash
(`-`) or a plus (`+`).
The dash or plus is removed when YS reads the scalar.

Note: the dash cannot have whitespace after it, but the plus can.

```
-[1 2 3]: .map(inc)  # => [2 3 4]
=>: +
  'foo' + 'bar'      # => 'foobar'
```

### Printing text

```
say: 'hello'        # String
say: hello          # Variable
say: "$hello!!!"    # Interpolated string
say: |              # Multiline interpolated string
  Hello, $name!
  How are you?
print: 'I have no newline'
warn: 'Prints to stderr (with trailing newline)'
```


### Define a function

YS functions, like Clojure functions, require a specific argument arity, and
can be defined to be multi-arity.

```
defn greet(name):
  say: "Hello, $name!"

defn greet(name='world'):  # Default argument

defn foo(bar *baz): # Variable number of arguments

defn foo(*):        # Any number of arguments

defn foo(_ x _):    # Ignored arguments

defn foo:           # Multi-arity function
  (): 0
  (x): x
  (x y): x + y
```


### Call a function

```
greet()             # Scalar call variations
greet('Bob')
(greet 'Bob')
-'Bob'.greet()

greet:              # Map pair call variations
greet: 'Bob'
greet 'Bob':
```


### Chain calls

```
say: read('/usr/share/dict/words')
     .lines():shuffle.take(3).join(' | ')
# => specialty | mutation's | Kisangani
```

!!! note

    `.lines():shuffle` is short for `.lines().shuffle()`.
    It must be be attached to something on the left.


#### Special chain operators

* `.@` - Short for `.deref()`
* `.$` - Short for `.last()`
* `.#` - Short for `.count()`
* `.?` - Short for `.truey?()`
* `.!` - Short for `.falsey?()`
* `.??` - Short for `.boolean()`
* `.!!` - Short for `.not()`
* `.++` - Short for `.inc()`
* `.--` - Short for `.dec()`
* `.>>>` - Short for `.DBG()`


### Looping

List comprehensions are done with the `for`, `each`, `map`
```
each i (1 .. 3):
  say: i
```

```
map inc: (1 .. 3)
```

```
reduce (fn [acc num] acc + num) 0: (1 .. 3)
```

reduce _ 0 (1 2 3):
  fn(acc num): acc + num
The `_` is a placeholder for the defined function argument.
Use `_` when the function argument is too long to write in place.


### Conditional (if/else)

```
if a > 10:
  say: 'BIG'
  say: 'small'
```

The `if` construct must have a 'then' and an 'else' clause.
Use the `


### Conditional (cond)

```
cond:
  a < 5: 'S'
  a < 10: 'M'
  a < 15: 'L'
  =>: 'XL'
```


### Interpolation

```
say: |
  Dear $name,

  I have 3 words for you: $(words().take(3 ).join(', ')).

  Yours truly, $get(ENV 'USER')
```


### Global variables

```
- _                 # Previous document value
- +++               # Runtime state mapping
- ARGV              # Command line arguments
- ARGS              # Command line arguments parsed
- CWD               # Current working directory
- DIR               # Parent directory path of the current script
- ENV               # Environment variables mapping
- FILE              # File path of the current script
- INC               # File loading include path
- RUN               # Runtime information mapping
- VERSION           # YS version
```
