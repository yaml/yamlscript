#!/usr/bin/env ys-0

require ys::taptest: :all

yaml1 =: |
  !YS-v0
  ---
  foo: boom
  ---
  !data
  bar:: +++.$.foo
  baz: 123

test::
- code: yaml1:lines:rest:text:eval:yaml/dump
  want: |
    bar: boom
    baz: 123

- code: yaml1:ys/eval:yaml/dump
  want: |
    bar: boom
    baz: 123

- code: yaml1:ys/eval-stream:yaml/dump
  want: |
    - foo: boom
    - bar: boom
      baz: 123

done:
