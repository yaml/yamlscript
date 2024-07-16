#!/usr/bin/env ys-0

!yamlscript/v0

require ys::taptest: test done

test::
- name: Function call
  code: 41 .inc()
  want: 42

- name: Dot chain
  code: (1 .. 10).drop(2).take(3)
  want:: +[3 4 5]

- name: Run program, test output
  code: sh("ys $(ENV.ROOT)/sample/advent/tree.ys 3").out
  want: |1+
      *
     ***
    *****
      *
      *

done:
