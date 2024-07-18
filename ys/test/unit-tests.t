#!/usr/bin/env ys-0

!yamlscript/v0

require ys::taptest: test done

AV =: FILE.dirname() + '/../../sample/advent'

hash =::
  foo: one
  :bar: two

test::
- name: 1.2.3 not a number
  code: 1.2.3
  what: error
  want: 'Invalid number: 1.2.3'

- name: Function call
  code: 41 .inc()
  want: 42

- name: Dot chain
  code: (1 .. 10).drop(2).take(3)
  want:: +[3 4 5]

- name: Get nth
  code: (1 .. 10).5
  want: 6

- name: Get string key maybe
  code: hash.foo
  want: one

- name: Get string key single
  code: hash.'foo'
  want: one

- name: Get string key double
  code: hash."foo"
  want: one

- name: Get keyword key maybe
  code: hash.bar
  want: two

- name: Get keyword key explicit
  code: hash.:bar
  want: two

done:
