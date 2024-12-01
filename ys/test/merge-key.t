#!/usr/bin/env ys-0

require ys::taptest: :all

test::
- name: Merge key simple
  data: |
    foo: bar
    <<:
      bar: baz
  want: |
    foo: bar
    bar: baz

- name: Mapping key wins
  data: |
    foo: 1
    <<:
      foo: 2
      bar: 3

  want: |
    foo: 1
    bar: 3

- name: Multiple << keys - last wins
  data: |
    foo: 1
    <<:
      bar: 2
    <<:
      bar: 3
  want: |
    foo: 1
    bar: 3

- name: Sequence of maps in << merge key
  data: |
    foo: 1
    <<: [{bar: 2}, {baz: 3}]
  want: |
    foo: 1
    bar: 2
    baz: 3

- name: Key order comes from main map
  data: |
    foo: 1
    baz: 3
    <<: [{bar: 2}, {quux: 4}]
  want: |
    foo: 1
    baz: 3
    bar: 2
    quux: 4

- name: Chained << merges
  data: |
    one: &a
      a: 1
    two: &b
      <<: *a
      b: 2
    three:
      <<: *b
      c: 3
  want: |
    one:
      a: 1
    two:
      b: 2
      a: 1
    three:
      c: 3
      b: 2
      a: 1

done: 6
