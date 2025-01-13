#!/usr/bin/env ys-0

use ys::taptest: :all

hash =: -{
  \'aaa 111
  'bbb' 222
  :ccc 333
  444 444
  'nil' 555
  'true' 666
  }

keys1 =: q(aaa bbb ccc)

test::
- code: hash.slice(444 555)
  want:: -[444 nil]
- code: hash.slice(q(aaa bbb ccc))
  want:: -[111 222 333]
- code: hash.slice(q(aaa bbb ccc) 444 qw(nil true))
  want:: -[111 222 333 444 555 666]
- code: hash.slice(keys1)
  want:: -[111 222 333]

done: 4
