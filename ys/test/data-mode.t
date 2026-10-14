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

- name: Non-nil conditional pairs preserve non-null values
  code: |
    values =::
      text: example
      flag: false
      number: 0
      empty: ''
      empty_map: {}
      empty_sequence: []
      mapping: {a: 1}
      sequence: [1, 2]
      missing:
    result =::
      text:??: values.text
      flag:??: values.flag
      number:??: values.number
      empty:??: values.empty
      empty_map:??: values.empty_map
      empty_sequence:??: values.empty_sequence
      mapping:??: values.mapping
      sequence:??: values.sequence
      missing:??: values.missing
      absent:??: values.absent
    =>: result
  want:
    text: example
    flag: false
    number: 0
    empty: ''
    empty_map: {}
    empty_sequence: []
    mapping: {a: 1}
    sequence: [1, 2]

- name: Truey conditional pairs preserve truey values
  code: |
    result =::
      text:?: :'example'
      yes:?: true
      no:?: false
      zero:?: 0
      zero_float:?: 0.0
      empty:?: :''
      empty_map:?: :{}
      empty_sequence:?: :[]
      negative:?: -1
      mapping:?: :{'a' 1}
      sequence:?: :[1 2]
      missing:?: nil
    =>: result
  want:
    text: example
    yes: true
    negative: -1
    mapping: {a: 1}
    sequence: [1, 2]

- name: Conditional pair omits null even as the only pair
  code: |
    missing:??: nil
  mode: data
  want: '{}'

- name: Conditional pair evaluates its value once
  code: |
    calls =: atom(0)
    result =::
      truey:?: swap!(calls inc)
      non_nil:??: swap!(calls inc)
    vector: result deref(calls)
  want:
  - truey: 1
    non_nil: 2
  - 2

- name: Omitted conditional pairs evaluate their values once
  code: |
    calls =: atom(0)
    defn missing():
      swap!: calls inc
      =>: nil
    result =::
      truey:?: missing()
      non_nil:??: missing()
    vector: result deref(calls)
  want:
  - {}
  - 2

- name: Conditional pair semantics in code-value collections
  code: |
    result =:::
      truey_zero:?: 0
      non_nil_zero:??: 0
      truey_false:?: false
      non_nil_false:??: false
      missing:??: nil
    =>: result
  want:
    non_nil_zero: 0
    non_nil_false: false

- name: Conditional pairs around embedded code
  code: |
    a: 1
    b:??: false
    x =: 0
    c:?: x
    d:??: x
  mode: data
  want: '{"a" 1, "b" false, "d" 0}'

- name: Quoted conditional suffixes stay literal
  code: |
    result =::
      'literal:?': false
      'literal:??': 0
    =>: result
  want:
    'literal:?': false
    'literal:??': 0

done:
