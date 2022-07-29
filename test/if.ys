#!/usr/bin/env yamlscript

use: YS-TestYAMLScript

main():

- if:
  - (==): [foo, foo]
  - pass: Testing 'if' statement with (==)

- if:
  - (=~): [barge, bar]
  - pass: Testing 'if' statement with (=~)

- if:
  - (!~): [barge, bar]
  - fail: Oops
  - pass: Testing 'if/else' statement with (!~)

# vim: ft=yaml:
