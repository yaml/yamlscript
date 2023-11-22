#!/usr/bin/env yamlscript

use: YS-TestYAMLScript

main():
- is:
  - add: [2, 2]
  - 4
  - Testing 'add' function

- is:
  - (+): [2, 2]
  - 4
  - Testing '+' operator

- foo =: 42
- is:
  - $foo
  - 42
  - Testing 'def' variable assignment

- for:
  - conj:
      - range: [1, 3]
      - $foo
  - pass:
    - Testing 'for (1, 2, 3, $foo)' -- $_ passes

- my-is:
  - Testing locally defined 'my-is' function (label first!)
  - $foo
  - 42

my-is(label, got, want):
- is: [$got, $want, $label]

# vim: ft=yaml:
