#!/usr/bin/env ys-0

use ys::taptest: :all

hash =::
  FOO: 1
  FOO_BAR: 2

test::
- name: Data mode sequence insertions using '::'
  code: |
    - foo
    - :: 3 .. 5
    - :: 42
    - :: +{'a' 1 'b' 2}
    - bar
  mode: data
  want: ("foo" 3 4 5 42 {"a" 1, "b" 2} "bar")

done:
