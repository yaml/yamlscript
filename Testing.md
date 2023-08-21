Testing Instructions
====================

Ways to test YAMLScript


## Prerequisites

The following coomand line tools are required for YAMLScript testing:

* GNU `make`
* `clojure`
  * Clojure's `lein`
* `python3`
* `perl`
* `bash` (Not required to be your shell, just installed)
* `curl`
* `tar`

Java, the JDK and GraalVM do **NOT** need to be installed.
The test system will download the correct Java tools in `/tmp/` and use those.


## Testing the Python binding to libyamlscript

First run:

    $ source .rc

This will set the environment the following environment variables:

* `YAMLSCRIPT_ROOT`
* `GRAALVM_HOME`
* `JAVA_HOME`
* `PYTHONPATH`
* Adds to `PATH` if needed

It will also define these shell functions:

* `test-ys-string`
  * `python3 -c 'import sys,yamlscript; print(yamlscript.load(sys.argv[1]))'`
* `test-ys-file`
  * `python3 -c 'import sys,yamlscript; print(yamlscript.load(open(sys.argv[1], "r")))'`

Next run:

    $ make clean build

This will compile the `libyamlscript` shared library.

Now you can run commands like:

```
$ test-ys-string 'inc: 41'
$ test-ys-string 'identity: (hash-map :a 1 :b 2 :c 3)'
$ test-ys-string 'range: 10'
$ test-ys-string 'list: (str +)'

$ test-ys-file <(echo 'range: 10')
$ echo 'range: 10' | test-ys-file /dev/stdin
```


## REPL testing for `libyamlscript`

Clojure REPL test the `libyamlscript.core` library:
```
$ make -C libyamlscript/ repl
libyamlscript.core=> (->> "range: 10" libyamlscript.core/-evalYsToJson)
"[0,1,2,3,4,5,6,7,8,9]"
libyamlscript.core=> (->> "range: 10" libyamlscript.core/-evalYsToJson println)
[0,1,2,3,4,5,6,7,8,9]
nil
libyamlscript.core=>
```

Clojure REPL test the `yamlscript.core` library:
```
$ make -C clojure/ repl
yamlscript.core=> (->> "range: 10" yamlscript.core/ys-to-clj)
"(range 10)\n"
yamlscript.core=> (->> "range: 10" yamlscript.core/ys-to-clj println)
(range 10)

nil
yamlscript.core=>
```


## Running Unit Tests
```
$ make test                 # Run all unit tests
$ make -C clojure/ test     # Run Clojure unit tests
$ make -C perl/ test        # Run Perl unit tests
$ make -C python/ test      # Run Python unit tests
```
