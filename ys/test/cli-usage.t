#!/usr/bin/env ys-0

use ys::taptest: :all

VERSION =: '0.2.4'

ROOT =: -"$DIR/../.."

HELP =: |

  ys - The YS Command Line Tool - v0.2.4

  Usage: ys [<option...>] [<input-file>]

  Options:

    -l, --load          Evaluate input & print the result value
                          default output format is compact JSON
    -r, --run           Run the YS code (this is the default action)
    -c, --compile       Compile YS code to source code or binary
                          default output format is Clojure code

    -Y, --yaml          Short for --to=yaml
    -J, --json          Short for --to=json
    -T, --to FORMAT     Output format for --load or --compile:
                          load: yaml, json, xml, csv, tsv, edn
                          compile: clj, glj, go,
                                   graal-bin, go-bin, wasm

    -e, --eval YSEXPR   Evaluate a YS expression
                          enables --mode=code by default
                          multiple -e values are joined by newline
    -p, --print         Print the final evaluation result value
    -m, --mode MODE     Set input mode: code, data, or bare (for -e)

    -i, --input PATH    Explicitly indicate input path (file or dir)
    -o, --output PATH   Output path for --load or --compile

    -I, --include PATH  Add directories to the library search path
    -s, --stream        Output all results from a multi-document stream
    -U, --unordered     Mappings don't preserve key order (faster)
    -C, --clojure       Don't compile input. Treat as Clojure code

    -x, --xtrace        Print each expression before evaluation
    -S, --stack         Print full stack trace for errors
    -d                  Debug all compilation stages
    -D, --debug STAGE   Debug a specific compilation stage:
                          parse, compose, resolve, build,
                          transform, construct, print
                        can be used multiple times

        --install       Install the libys shared library
        --upgrade       Upgrade both ys and libys

        --version       Print version and exit
    -h, --help          Print this help and exit


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

- note: Test -x flag with function declarations (declare form should be wrapped with TTT)
- cmnd: |-
    ys -xce 'defn main(): say(hello())' -e 'defn hello(): "Hello"'
  want: |
    (TTT (declare hello))
    (defn main [] (TTT (say (TTT (hello)))))
    (defn hello [] "Hello")
    (TTT (apply main ARGS))

- cmnd: |-
    ys -xce 'defn main(): say(hello() + world())' -e 'defn hello(): "Hello"' -e 'defn world(): "World"'
  want: |
    (TTT (declare hello world))
    (defn main [] (TTT (say (TTT (add+ (TTT (hello)) (TTT (world)))))))
    (defn hello [] "Hello")
    (defn world [] "World")
    (TTT (apply main ARGS))

- cmnd: |-
    ys -xce 'defn main(): say(hello())' -e 'defn hello(): "Hello"' -e 'defn helper(): "Helper"'
  want: |
    (TTT (declare hello))
    (defn main [] (TTT (say (TTT (hello)))))
    (defn hello [] "Hello")
    (defn helper [] "Helper")
    (TTT (apply main ARGS))

- note: Test that -x flag without function declarations works normally
- cmnd: |-
    ys -xce 'say("Hello, World!")'
  want: |
    (TTT (say "Hello, World!"))

- note: Test that normal compilation without -x flag doesn't wrap declare forms
- cmnd: |-
    ys -ce 'defn main(): say(hello())' -e 'defn hello(): "Hello"'
  want: |
    (declare hello)
    (defn main [] (say (hello)))
    (defn hello [] "Hello")
    (apply main ARGS)

- note: Test that functions not referenced by main are not declared
- cmnd: |-
    ys -xce 'defn main(): say(hello())' -e 'defn hello(): "Hello"' -e 'defn unused(): "Unused"'
  want: |
    (TTT (declare hello))
    (defn main [] (TTT (say (TTT (hello)))))
    (defn hello [] "Hello")
    (defn unused [] "Unused")
    (TTT (apply main ARGS))

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
  stdi:: read("$DIR/animals.json")
  want: Meowsy
- cmnd: ys -Y '.0.name' -
  stdi:: read("$DIR/animals.json")
  want: Meowsy
- cmnd: ys -Y '.0.name' -
  stdi:: read("$DIR/animals.json")
  want: Meowsy
- cmnd: ys -Ye '.0.name' -
  stdi:: read("$DIR/animals.json")
  want: Meowsy
- cmnd: ys '.0' -
  stdi:: read("$DIR/animals.json")
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
  stdi:: read("$DIR/animals.json")
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
