#!/usr/bin/env yamlscript

use:
- YS-TestYAMLScript

main():
- var =: abcdefghijklmnop

- !is
  - len: $var
  - 16
  - Unquoted string is interpolated

- !is
  - len: "$var"
  - 4
  - Quoted string is not interpolated

# vim: ft=yaml:
