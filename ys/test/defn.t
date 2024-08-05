#!/usr/bin/env ys-0

!yamlscript/v0

require ys::taptest: test done

test::
- name: Private function
  code: |
    defn foo(x): bar(x) + 1
    defn- bar(x): x * 2
    foo: 7
  want: 15

done:
