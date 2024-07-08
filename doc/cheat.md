---
title: YAMLScript Cheat Sheet
---

```
### Assign a value to a variable
hello =: 'Oh hello'

### Print something
say: 'hello'        # String
say: hello          # Variable
say: "$hello!!!"    # Interpolated string

### Define a function
defn greet(name='world'):
  say: "Hello, $name!"

### Call a function
greet()             # Scalar call variations
greet('Bob')
(greet 'Bob')

greet:              # Map pair call variations
greet: 'Bob'
greet 'Bob':

### Chain calls
say: slurp("/usr/share/dict/words")
     .split(/\n/).shuffle().take(3)
     .join(".")
# => specialty.mutation's.Kisangani

### Looping
each [i (1 .. 3)]:
  say: i

### Conditional (if/else)
if (a > 10):
  say: 'BIG'
  say: 'small'

### Conditional (cond)
cond:
  a < 5: 'S'
  a < 10: 'M'
  a < 15: 'L'
  =>: 'XL'

### Interpolation
say: |
  Dear $name,

  I have 3 words for you: $(words().take(3 ).join(", ")).

  Yours truly, $get(ENV "USER")

### Global variables
- $                 # Runtime state mapping
- $$                # Previous document value
- $#                # Document evaluation count
- ARGV              # Command line arguments
- ARGS              # Command line arguments parsed
- CWD               # Current working directory
- ENV               # Environment variables mapping
- FILE              # File path of the current script
- INC               # File loading include path
- VERSION           # YAMLScript version
- VERSIONS          # Runtime component versions
```
