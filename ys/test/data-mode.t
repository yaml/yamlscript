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
  want: '["foo" 3 4 5 42 {"a" 1, "b" 2} "bar"]'

- name: Conditional pairs preserve non-null values
  code: |
    values =::
      text: example
      flag: false
      number: 0
      empty: ''
      mapping: {a: 1}
      sequence: [1, 2]
      missing:
    result =::
      text:?: values.text
      flag:?: values.flag
      number:?: values.number
      empty:?: values.empty
      mapping:?: values.mapping
      sequence:?: values.sequence
      missing:?: values.missing
    =>: result
  want:
    text: example
    flag: false
    number: 0
    empty: ''
    mapping: {a: 1}
    sequence: [1, 2]

- name: Conditional pair omits null even as the only pair
  code: |
    missing:?: nil
  mode: data
  want: '{}'

- name: Conditional pair evaluates its value once
  code: |
    calls =: atom(0)
    result =::
      value:?: swap!(calls inc)
    vector: result deref(calls)
  want:
  - value: 1
  - 1

done:
