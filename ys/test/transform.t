#!/usr/bin/env ys-0

require ys::taptest: :all

say: CWD

base =:
  if CWD =~ /\/yamlscript$/:
    then: "$CWD/ys"
    else: CWD

test::
- name: Check that `case` works like `cond`
  code: |
    x =: 42
    case type(x):
      String  : "S"
      Boolean : "B"
      else    : "X"
  want: X

done:
