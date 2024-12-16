#!/usr/bin/env ys-0

require ys::taptest: :all

yaml1 =: |
  ---
  foo: boom
  --- !yamlscript/v0:
  bar:: +++.$.foo
  baz: 123

test::
- code: yaml1:eval:yaml/dump
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
