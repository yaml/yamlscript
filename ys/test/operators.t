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
- code: sqr(5)
  want: 25
- code: cube(5)
  want: 125
- code: sqrt(25)
  want: 5.0

- note: add+ tests

- code: 2 + \\A
  want:: 67
- code: \\A + 2
  want:: \\C
- code: \\A + \\B
  want: AB
- code: (\\A + 2) + 3
  want:: \\F
- code: -{:a 1} + {:b 2} + {:c 3}
  want:: -{:a 1 :b 2 :c 3}
- code: -'J' + 5
  want: J5
- code: -'J' + \\5
  want: J5
- code: -'12' + \\3 + 4
  what: error
  want: Cannot add+ multiple types when more than 2 arguments
- code: \{:a :b} + \{:b :c :d}
  want:: \{:a :b :c :d}
- name: add+ with hash-maps and array-maps
  code: -{:a 1} + {:b 2} + {:c 1 :d 1 :e 1 :f 1 :g 1 :h 1 :i 1 :j 1 :k 1 :l 1}
  want:: -{:a 1 :b 2 :c 1 :d 1 :e 1 :f 1 :g 1 :h 1 :i 1 :j 1 :k 1 :l 1}

- note: sub+ tests

- code: \\C - 2
  want:: \\A
- code: \\C - \\A
  want: 2
- code: \\C - 2
  want:: \\A
- code: \\A - []
  what: error
  want: Cannot sub(\A [])
- code: -'foobarbazbar' - 'bar'
  want: foobaz
- code: 2 - nil
  what: error
  want: Cannot subtract with a nil value

- note: .-- and .++

- code: 2 .++
  want: 3
- code: 2 .--
  want: 1
- code: \\B.++
  want:: \\C
- code: \\B.--
  want:: \\A
- code: nil.++
  what: error
  want: Can't convert nil to number
- code: -"B".++
  what: error
  want: Cannot inc+("B")

- code: (1 ... 10).#.eq(9)
- code: (1 ... 0).#.eq(0)
- code: (1 ... 1).#.eq(0)
- code: (1 ... 2).--.eq(0)

done:
