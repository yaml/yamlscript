#!/usr/bin/env ys-0

!yamlscript/v0

require ys::taptest: test done

VERSION =: '0.1.75'

ROOT =: -"$fs-dirname(FILE)/../.."

HELP =: |

  ys - The YAMLScript (YS) Command Line Tool - v$VERSION

  Usage: ys [<option...>] [<file>]

  Options:

        --run                Run a YAMLScript program file (default)
    -l, --load               Output (compact) JSON of YAMLScript evaluation
    -e, --eval YSEXPR        Evaluate a YAMLScript expression
                             multiple -e values joined by newline

    -c, --compile            Compile YAMLScript to Clojure
    -b, --binary             Compile to a native binary executable

    -p, --print              Print the result of --run in code mode
    -o, --output FILE        Output file for --load, --compile or --binary

    -T, --to FORMAT          Output format for --load:
                               json, yaml, edn
    -J, --json               Output (pretty) JSON for --load
    -Y, --yaml               Output YAML for --load
    -E, --edn                Output EDN for --load
    -U, --unordered          Mappings don't preserve key order (faster)

    -m, --mode MODE          Add a mode tag: code, data, or bare (for -e)
    -C, --clojure            Treat input as Clojure code

    -d                       Debug all compilation stages
    -D, --debug-stage STAGE  Debug a specific compilation stage:
                               parse, compose, resolve, build,
                               transform, construct, print
                             can be used multiple times
    -S, --stack-trace        Print full stack trace for errors
    -x, --xtrace             Print each expression before evaluation

        --install            Install the libyamlscript shared library
        --upgrade            Upgrade both ys and libyamlscript

        --version            Print version and exit
    -h, --help               Print this help and exit

#'

test::
- cmnd: ys --version
  want:: "YAMLScript $VERSION"

- cmnd: ys
  want:: HELP

- cmnd: ys -h
  want:: HELP

- cmnd: ys --help
  want:: HELP

- cmnd: "ys -ce '=>: 1 + 2'"
  want: (add+ 1 2)

- cmnd: "ys -pe '=>: 6 * 7'"
  want: '42'

- cmnd: ys -Cle '{:x 123}'
  want: '{"x":123}'

- cmnd: ys -pl ...
  want: 'Error: Options --print and --load are mutually exclusive.'

- cmnd: ys -cp ...
  want: 'Error: Options --print and --compile are mutually exclusive.'

- name: ys ys/test/hello.ys
  cmnd:: "ys $ROOT/ys/test/hello.ys"
  want: Hello

- name: ys --load ys/test/hello.ys
  cmnd:: "ys --load $ROOT/ys/test/hello.ys"
  want: |
    Hello
    12345

- cmnd: |-
    ys --eval 'say: "Hello, World!"'
  want: |
    Hello, World!

- cmnd: |-
    ys -e 'say: "Hello, World!"' -e 'identity: 12345'
  want: |
    Hello, World!

- cmnd: |-
    ys -pe 'range: 25'
  want: |
    (0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24)

- cmnd: |-
    ys -le 'range: 25'
  want: |
    [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24]

- cmnd: |-
    ys -Ye 'range: 5'
  want: |
    - 0
    - 1
    - 2
    - 3
    - 4

- cmnd: |-
    ys -Ee 'range: 5'
  want: |
    (0 1 2 3 4)

- name: ys --compile ys/test/hello.ys
  cmnd:: |-
    ys --compile $ROOT/ys/test/hello.ys
  want: |
    (say "Hello")
    (identity 12345)

- cmnd:: |-
    ys -ce 'say: "Hello, World!"'
  want: |
    (say "Hello, World!")

- cmnd:: |-
    ys -c -e 'say: "Hello, World!"' -e 'identity: 12345'
  want: |
    (say "Hello, World!")
    (identity 12345)

- name: ys -Y ys/test/loader.ys
  cmnd:: |-
    ys -Y $ROOT/ys/test/loader.ys
  want: |
    foo: This is a string
    bar:
      foo:
        bar:
        - aaa: 1
        - bbb: 2
    baz:
    - aaa: 1
    - bbb: 2

done:
