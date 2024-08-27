#!/usr/bin/env ys-0

!yamlscript/v0

require ys::taptest: :all

test::
- name: Operator `==` for generic equality
  code: lc('Foo') == 'foo'
- name: Operator `!=` for generic inequality
  code: ('foo' != 'bar')
- code: 3 < 4 < 5
- code: 3 <= 4 <= 4
- code: 5 > 4 > 3
- code: 5 >= 4 >= 4

- name: Operator `=~` for regex match
  code: ('foo' =~ /.o/)
  want: fo

- name: Operator `!~` for regex does not match
  code: ('foo' !~ /.a/)

- code: eq({:a 1 :b 2} {:b 2 :a 1})
- code: ne({:a 1 :b 2} [:b 2 :a 1])
- code: gt(6 5)
- code: ge(6 5)
- code: ge(5 5)
- code: lt(5 6)
- code: le(5 5)
- code: le(5 6)

- code: 2 * 3 * 4
  want: 24
- code: 24 / 3 / 4
  want: 2
- code: 2 + 3 + 4
  want: 9
- code: 9 - 3 - 4
  want: 2
- code: ((2 ** 3) ** 4)
  want: 4096
- code: squared(5)
  want: 25
- code: cubed(5)
  want: 125
- code: sqrt(25)
  want: 5.0

done:
