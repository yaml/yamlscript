#!/usr/bin/env ys-0

use ys::taptest: :all

VERSION =: '0.1.94'

ROOT =: -"$DIR/../.."

HELP =: |

  ys - The YS Command Line Tool - v$VERSION

  Usage: ys [<option...>] [<file>]

  Options:

    -e, --eval YSEXPR        Evaluate a YS expression
                               multiple -e values are joined by newline
    -l, --load               Output the (compact) JSON of YS evaluation
    -f, --file FILE          Explicitly indicate input file

#   -c, --compile            Compile YS to Clojure
#   -b, --binary             Compile to a native binary executable

#   -p, --print              Print the final evaluation result value
#   -o, --output FILE        Output file for --load, --compile or --binary
#   -s, --stream             Output all results from a multi-document stream

#   -T, --to FORMAT          Output format for --load:
#                              json, yaml, csv, tsv, edn
#   -J, --json               Output (pretty) JSON for --load
#   -Y, --yaml               Output YAML for --load
#   -U, --unordered          Mappings don't preserve key order (faster)

#   -m, --mode MODE          Add a mode tag: code, data, or bare (for -e)
#   -C, --clojure            Treat input as Clojure code

#   -d                       Debug all compilation stages
#   -D, --debug-stage STAGE  Debug a specific compilation stage:
#                              parse, compose, resolve, build,
#                              transform, construct, print
#                            can be used multiple times
#   -S, --stack-trace        Print full stack trace for errors
#   -x, --xtrace             Print each expression before evaluation

#       --install            Install the libyamlscript shared library
#       --upgrade            Upgrade both ys and libyamlscript

#       --version            Print version and exit
#   -h, --help               Print this help and exit

#'

test::
- cmnd: ys --version
  want:: "YS (YAMLScript) $VERSION"

- cmnd: ys
  have:: HELP
# want:: HELP

- cmnd: ys -h
  have:: HELP
# want:: HELP

- cmnd: ys --help
  have:: HELP
# want:: HELP

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
    ys -T edn -e 'range: 5'
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

- cmnd: |-
    ys -x -e 'each i (1 .. 3): say("$i) Hello \#$i")'
  what: err
  want: |
    +1 >>> each([i (rng 1 3)], (say (str i ") Hello #" i)))
    +2 >>> rng(1, 3)
    +3 >>> str(1, ") Hello #", 1)
    +4 >>> say("1) Hello #1")
    +5 >>> str(2, ") Hello #", 2)
    +6 >>> say("2) Hello #2")
    +7 >>> str(3, ") Hello #", 3)
    +8 >>> say("3) Hello #3")

- note: Test compiling YS scripts in the repo
- cmnd:: "ys -c $ROOT/util/brew-update"
  have: apply main
- cmnd:: "ys -c $ROOT/util/mdys"
  have: apply main
- cmnd:: "ys -c $ROOT/util/release-yamlscript"
  have: apply main
- cmnd:: "ys -c $ROOT/util/version-bump"
  have: apply main

- note: Test 'ys' commands with and without -e
- cmnd:: "ys -Y $DIR/animals.json -e '.0.name'"
  want: Meowsy
- cmnd:: "ys -Ye '.0.name' $DIR/animals.json"
  want: Meowsy
- cmnd:: "ys -Y '.0.name' $DIR/animals.json"
  want: Meowsy
- cmnd: ys -Y '.0.name'
  stdi:: slurp("$DIR/animals.json")
  want: Meowsy
- cmnd: ys -Y '.0.name' -
  stdi:: slurp("$DIR/animals.json")
  want: Meowsy
- cmnd: ys -Y '.0.name' -
  stdi:: slurp("$DIR/animals.json")
  want: Meowsy
- cmnd: ys -Ye '.0.name' -
  stdi:: slurp("$DIR/animals.json")
  want: Meowsy
- cmnd: ys '.0' -
  stdi:: slurp("$DIR/animals.json")
  want: |
    name: Meowsy
    species: cat
    foods:
      likes:
      - tuna
      - catnip
      dislikes:
      - ham
      - zucchini
- cmnd: ys -Y
  stdi:: slurp("$DIR/animals.json")
  have: |
    - name: Meowsy
      species: cat
      foods:
        likes:
        - tuna
        - catnip
        dislikes:
        - ham
        - zucchini
- cmnd: test/shebang1 --version
  want: --version

done:
