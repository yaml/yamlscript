#!/usr/bin/env ys-0

require ys::taptest: :all

hash =::
  FOO: 1
  FOO_BAR: 2

test::
- code: hash.FOO:B
- code: hash.FOO_BAR:B

done: 2
