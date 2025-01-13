#!/usr/bin/env ys-0

use ys::taptest: :all

test::
- name: Private function
  code: |
    defn foo(x): bar(x) + 1
    defn- bar(x): x * 2
    foo: 7
  want: 15

done:
