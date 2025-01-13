#!/usr/bin/env ys-0

use ys::taptest: :all

hash =::
  FOO: 1
  FOO_BAR: 2

test::
- code: hash.FOO:B
- code: hash.FOO_BAR:B

- code: (1 .. 5).map(\(_:inc))
  want: [2, 3, 4, 5, 6]

done: 3
