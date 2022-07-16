#!/usr/bin/env yamlscript

use:
- YS-TestYAMLScript

main():
- var =: abcdefghijklmnop

- !is
  - len: $var
  - 16
  - Unquoted string IS interpolated

- !is
  - len: "$var"
  - 4
  - Quoted string IS NOT interpolated

- !is
  - len: ! "$var"
  - 16
  - Tagged quoted string IS interpolated

- ignore: # WIP
  - return: 0

  - template =: |
      Hello $name,
      How is your $topic?
  - want =:
    - - |
        Hello Ingy,
        How is your YAML?
      - |
        Hello Tina,
        How is your Perl?
      - |
        Hello Panto,
        How is your C?
  - map:
    - - is:
        - !fn interpolate: $_
        - drop: $want
        - Template render is ok
      - - [Ingy, YAML]
        - [Tina, Perl]
        - [Panto, C]


# vim: ft=yaml sw=2:
