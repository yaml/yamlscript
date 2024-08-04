#!/usr/bin/env ys-0

!yamlscript/v0

require ys::taptest: :all

test::
- name: Operator `==` for generic equality
  code: ('foo' == 'oof'.reverse())
  SKIP: true

- name: Operator `!=` for generic inequality
  code: ('foo' != 'bar')

done:
