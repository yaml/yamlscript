#!/usr/bin/env ys-0

use ys::taptest: :all

VERSION =: '0.2.32'

ROOT =: +"$DIR/../.."

HELP =: |

  ys - The YS Command Line Tool - v$VERSION

  Usage: ys [<option...>] [<file>]

  Options:

    -e, --eval YSEXPR        Evaluate a YS expression
                               multiple -e values are joined by newline
    -l, --load               Output the (compact) JSON of YS evaluation
    -f, --file FILE          Explicitly indicate input file

#   -c, --compile            Compile YS to Clojure
#   -p, --print              Print the final evaluation result value
#   -o, --output FILE        Output file for --load or --compile
#   -s, --stream             Output all results from a multi-document stream

#   -T, --to FORMAT          Output format for --load:
#                              json, yaml, csv, tsv, edn
#                            or target for --compile:
#                              bb, clj, star
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

#       --install            Install the libys shared library
#       --upgrade            Upgrade both ys and libys

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

# -T clj emits the portable ys.v0 header with a JVM Clojure deps
# bootstrap (and implies -c)
- cmnd: "ys -T clj -e 'say: 1 + 2'"
  want: |
    (when-not (or (System/getProperty "babashka.version")
                  (System/getProperty "jolt.version"))
      (or (find-ns 'ys.v0)
          (try (require 'ys.v0) true (catch Exception _ false))
          (eval
            '(let [t (Thread/currentThread)
                   cl (clojure.lang.DynamicClassLoader.
                        (.getContextClassLoader t))]
               (.setContextClassLoader t cl)
               (with-bindings {(requiring-resolve 'clojure.core/*repl*) true}
                 ((requiring-resolve 'clojure.repl.deps/add-libs)
                  '{org.yamlscript/ys.v0 {:mvn/version "0.2.32"}}))
               (with-bindings {clojure.lang.Compiler/LOADER cl}
                 (require 'ys.v0)
                 (doseq [lib '[flatland.ordered.map clj-yaml.core
                               clojure.data.json clojure.data.csv
                               babashka.process babashka.http-client]]
                   (try (require lib) (catch Throwable _))))))))
    (ns main (:require ys.v0))
    (ys.v0/init)

    (say (add+ 1 2))

- cmnd: "ys -cT bb -e 'say: 123'"
  want: |
    (when (System/getProperty "babashka.version")
      (let [m2 (str (System/getProperty "user.home") "/.m2/repository/")
            jars [(str m2 "org/yamlscript/ys.v0/0.2.32/ys.v0-0.2.32.jar")
                  (str m2 "org/clojure/data.json/2.4.0/data.json-2.4.0.jar")]]
        (if (every? #(.exists (java.io.File. %)) jars)
          ((requiring-resolve 'babashka.classpath/add-classpath)
           (clojure.string/join java.io.File/pathSeparator jars))
          ((requiring-resolve 'babashka.deps/add-deps)
           '{:deps {org.yamlscript/ys.v0 {:mvn/version "0.2.32"}}}))))
    (ns main (:require ys.v0))
    (ys.v0/init)

    (say 123)

- cmnd: "ys -T star -e 'say: 123'"
  want: |
    (when-not (find-ns 'ys.v0)
      (require 'clojurestar.deps)
      ((resolve 'clojurestar.deps/add-deps)
       '{:deps {org.yamlscript/ys.v0 {:mvn/version "0.2.32"}}}))

    (ns main (:require ys.v0))
    (ys.v0/init)

    (say 123)

- cmnd: "ys -T bb -C -e 'say: 123'"
  what: err
  want: 'Error: Options --to=bb and --clojure are mutually exclusive.'

- cmnd: "ys -T bb -l -e 'say: 123'"
  what: err
  want: 'Error: Options --to=bb and --load are mutually exclusive.'

- cmnd: "ys -T star -l -e 'say: 123'"
  what: err
  want: 'Error: Options --to=star and --load are mutually exclusive.'

- cmnd: "ys -T frob -e 'say: 123'"
  what: err
  have: 'bb, clj, star (for --compile)'

- cmnd: "ys -T jolt -e 'say: 123'"
  what: err
  have: 'bb, clj, star (for --compile)'

- cmnd: "ys -T glj -e 'say: 123'"
  what: err
  have: 'bb, clj, star (for --compile)'

# -T bb with -o makes an executable bb script
- name: ys -T bb -o file
  cmnd: >-
    bash -c 'f=/tmp/ys-cli-usage-v0.clj;
    ys -T bb -e "say: 123" -o "$f" &&
    test -x "$f" && head -1 "$f"; rm -f "$f"'
  want: '#!/usr/bin/env bb'

- cmnd: "ys -pe '=>: 6 * 7'"
  want: '42'

- name: YS_PRINT enables result printing
  cmnd: "env YS_PRINT=1 ys -e '=>: 6 * 7'"
  want: '42'

- name: YS_FORMATTER runs for compilation
  cmnd: "env YS_FORMATTER=false ys -ce '=>: 6 * 7'"
  what: err
  have: "Compiler formatter error in 'false'"

- name: Debug stages include elapsed time
  cmnd: "ys -dc -e '=>: 6 * 7'"
  have: '*** parse     *** 0.'

- name: Definitions silently shadow referred vars
  cmnd: >-
    bash -c 'ys
    -e "name =: 1"
    -e "hash =: 2"
    -e "VERSION =: 3"
    -e "a =: 4"
    -e "say: +[name hash VERSION a]:joins"
    2>&1'
  want: 1 2 3 4

- cmnd: "ys -e 'say: \"Ingy döt Net ┌┼┐\"'"
  want: Ingy döt Net ┌┼┐

- cmnd: "ys -e \"if RUN.os: say('has-os') say('missing-os')\""
  want: has-os

- name: Public module requires import
  cmnd: "ys -e 'ys::fs/cwd()'"
  what: err
  want: 'Error: Could not resolve symbol: ys.fs/cwd'

- name: Plain use enables full module name
  cmnd: >-
    ys -e 'use: ys::str'
    -e 'say: ys::str/upper-case("used")'
  want: USED

- name: HTTP all imports curl
  cmnd: >-
    ys -e 'use ys::http: :all'
    -e 'say: fn?(curl)'
  want: 'true'

- name: Use alias enables short name
  cmnd: >-
    ys -e 'use ys::str: :as str'
    -e 'say: str/upper-case("aliased")'
  want: ALIASED

- name: Grouped use loads aliases
  cmnd: >-
    bash -c 'printf "%s\n" "!ys-0" "use:"
    "  ys::fs: :as fs" "  ys::str: :as str"
    "say: str/upper-case(fs/basename(CWD))" | ys -'
  want: YS

- name: Scalar short names create aliases
  cmnd: >-
    ys -e 'use: fs str'
    -e 'say: str/upper-case(fs/basename(CWD))'
  want: YS

- name: Scalar short names load all aliases
  cmnd: >-
    ys -e 'use: http fs ipc ys'
    -e 'say: and(fn?(http/get) fn?(fs/e) fn?(ipc/shell) fn?(ys/compile))'
  want: 'true'

- name: Filesystem short function has a module counterpart
  cmnd: >-
    ys -e 'use fs: :as fs'
    -e 'say: fs/e("../Meta")'
  want: 'true'

- name: Shell belongs to the IPC module
  cmnd: >-
    ys -e 'use ipc: :as ipc'
    -e 'say: fn?(ipc/shell)'
  want: 'true'

- name: Math module is available
  cmnd: >-
    ys -e 'use math: :as math'
    -e 'say: math/sqrt(81)'
  want: '9.0'

- name: Set module is available
  cmnd: >-
    ys -e 'use set: :as set'
    -e 'say: set/rename-keys({:old 42} {:old :new}).new'
  want: '42'

- name: CLI module is available
  cmnd: >-
    ys -e 'use cli: :as cli'
    -e "say: cli/parse-opts(['--help'], [['-h', '--help']]).options.help"
  want: 'true'

- name: Pprint module is available
  cmnd: >-
    ys -e 'use pprint: :as pprint'
    -e 'out: pprint/write([1 2 3] :stream nil)'
  want: '[1 2 3]'

- name: IO does not own pp
  cmnd: "ys -e 'use io: :as io' -e 'io/pp: 42'"
  what: err
  want: 'Error: Could not resolve symbol: io/pp'

- name: Filesystem passthrough is not standard
  cmnd: "ys -e 'fs-e: \"Meta\"'"
  what: err
  want: 'Error: Could not resolve symbol: fs-e'

- name: HTTP passthrough is not standard
  cmnd: "ys -e 'curl: \"https://example.com\"'"
  what: err
  want: 'Error: Could not resolve symbol: curl'

- name: Process passthrough is not standard
  cmnd: "ys -e 'shell: \"true\"'"
  what: err
  want: 'Error: Could not resolve symbol: shell'

- name: Plain use does not refer names
  cmnd: "ys -e 'use: ys::str' -e '=>: upper-case(\"missing\")'"
  what: err
  want: 'Error: Could not resolve symbol: upper-case'

- name: Require directs callers to use
  cmnd: "ys -e 'require: ys::str'"
  what: err
  want: "Error: The 'require' function is retired. Use 'use' instead."

- name: Deps use option is retired
  cmnd: "ys -e 'use foo::bar: :deps \"unused\"'"
  what: err
  want: "Error: Invalid 'use' option ':deps'"

- name: Module allowlist permits use
  cmnd: >-
    env YS_MODULES=str,io ys -e 'use: ys::str'
    -e 'say: ys::str/upper-case("allowed")'
  want: ALLOWED

- name: Module allowlist rejects use
  cmnd: "env YS_MODULES=str ys -e 'use: ys::fs'"
  what: err
  want: 'Error: ys.fs is disabled by YS_MODULES'

- name: Standard IO proxy honors module allowlist
  cmnd: "env YS_MODULES=str ys -e 'say: \"hidden\"'"
  what: err
  want: 'Error: ys.io is disabled by YS_MODULES'

- name: Standard filesystem proxy honors module allowlist
  cmnd: "env YS_MODULES=str ys -e 'read: \"Meta\"'"
  what: err
  want: 'Error: ys.fs is disabled by YS_MODULES'

- name: Standard pprint proxy honors module allowlist
  cmnd: "env YS_MODULES=io ys -e 'pp: 42'"
  what: err
  want: 'Error: ys.pprint is disabled by YS_MODULES'

- name: YS file loader honors module allowlist
  cmnd: >-
    env YS_MODULES=ys ys -e 'use: ys'
    -e 'ys/load-file: "Meta"'
  what: err
  want: 'Error: ys.fs is disabled by YS_MODULES'

- name: Short module name requires alias
  cmnd: "ys -e 'fs/cwd()'"
  what: err
  want: 'Error: Could not resolve symbol: fs/cwd'

- cmnd: ys -Cle '{:x 123}'
  want: '{"x":123}'

- cmnd: ys -pl ...
  what: err
  want: 'Error: Options --print and --load are mutually exclusive.'

- cmnd: ys -cp ...
  what: err
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

- note: >-
    Test -x flag with function declarations
    (declare form should be wrapped with TTT)
- cmnd: >-
    ys -xce 'defn main(): say(hello())'
    -e 'defn hello(): "Hello"'
  want: |
    (TTT (declare hello))
    (defn main [] (TTT (say (TTT (hello)))))
    (defn hello [] "Hello")
    (TTT (apply main ARGS))

- cmnd: >-
    ys -xce 'defn main(): say(hello() + world())'
    -e 'defn hello(): "Hello"'
    -e 'defn world(): "World"'
  want: |
    (TTT (declare hello world))
    (defn main [] (TTT (say (TTT (add+ (TTT (hello)) (TTT (world)))))))
    (defn hello [] "Hello")
    (defn world [] "World")
    (TTT (apply main ARGS))

- cmnd: >-
    ys -xce 'defn main(): say(hello())'
    -e 'defn hello(): "Hello"'
    -e 'defn helper(): "Helper"'
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
- cmnd: >-
    ys -xce 'defn main(): say(hello())'
    -e 'defn hello(): "Hello"'
    -e 'defn unused(): "Unused"'
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
