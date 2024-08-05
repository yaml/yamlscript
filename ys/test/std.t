#!/usr/bin/env ys-0

!yamlscript/v0

require ys::taptest: :all

say: CWD

base =:
  if (CWD =~ /\/yamlscript$/):
    then: "$CWD/ys"
    else: CWD

test::
- code: 'fs-d: CWD'
- code: 'fs-e: CWD'
- code: 'fs-f: "$base/test/std.t"'
- code: 'fs-l: "$base/test/a-symlink"'
- code: 'fs-r: CWD'
- code: 'fs-s: CWD'
- code: 'fs-w: CWD'
- code: 'fs-x: CWD'
- code: 'fs-z: "$base/test/empty-file"'

- code: fs-cwd()
  want:: CWD
- code: 'fs-which: "ys"'
  like: /ys$
- code: fs-mtime(CWD).str()
  like: ^\d{13}$

- code: ('foo' == 'oof'.reverse())
- code: ('foo' != 'bar')

- code: 'eq: +{:a 1 :b 2} {:b 2 :a 1}'
- code: 'ne: +{:a 1 :b 2} {:a 1}'
- code: 'gt: (2 + 3) 4'
- code: 'ge: (2 + 2) 4'
- code: 'lt: 4 (2 + 3)'
- code: 'le: 4 (2 + 2)'
- code: 'lt: 1 2 3 4'
- code: 'le: 1 2 2 3'

- code: uc('foo') == 'FOO'
- code: uc1('foo') == 'Foo'
- code: lc('FoOoO') == 'foooo'

- code: +[{"a" 1}{"a" 2}].map("a")
  want:: \'(1 2)
- code: 'map+ "a": q(({"a" 1}{"a" 2}))'
  want:: +[1 2]

- code: 'reduce+ + 0 (1 .. 5):'
  want: 15
- code: 'reduce+ 0 (1 .. 5): +'
  want: 15

done:
