#!/usr/bin/env ys-0

use ys::taptest: :all

test::
- code: '1'
  want: 1
- code: '0'
  want: 0
- code: '-0'
  want: 0
- code: '1.2'
  want: 1.2
- code: '-1.2'
  want: -1.2
- code: 5:inc
  want: 6
- code: 3.4.5
  what: error
  want: 'Invalid number: 3.4.5'
- code: 5.inc()
  want: 6
- code: 55.5.inc()
  want: 56.5
- code: 55.5:inc
  want: 56.5
- code: 3.4.5:inc
  what: error
  want: 'Invalid number: 3.4.5'
- code: 3.4.5.inc()
  what: error
  want: 'Invalid number: 3.4.5'
- code: 3/44+5.inc()
  what: error
  want: 'Invalid number: 3/44+5'
- code: 35.inc
  want:: nil
- code: 35.inc + 10
  what: error
  want: "Can't convert a nil value to a number"
- code: -1.inc()
  want: 0
- code: -1.5.inc()
  want: -0.5
- code: -1:inc
  want: 0
- code: -1.5:inc
  want: -0.5
- code: 5.in?(1 .. 10)
  want: true
- code: 5.5.in?(1 .. 10)
  want: false

done: 21
