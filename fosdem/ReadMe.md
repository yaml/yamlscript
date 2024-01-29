Using YAMLScript
================

Download this gist.

Try the example `make` commands.


## About YAMLScript

* YAMLScript is a new functional, general-purpose programming language
  * Syntax is YAML based
  * Clean, expressive, mixes well with YAML data
  * Execution speed is on par with Python or Ruby

* YAMLScript is also a new YAML 1.2 loader
  * Regular YAML files load as expected
  * No YAML 1.1 annoyances like "The Norway Problem"
  * Files with `!yamlscript/v0` can call functions

* Available as a YS loader module in various programming languages
  * Perl, Python, Raku, Ruby, Rust currently
  * All major languages soon

* Useful YAMLScript features for YAML users:
  * Load data from external files to specific places in your YAML
  * String interpolation
  * `merge`, `concat`, and 100s more useful functions

* YS programs compile to Clojure code
  * Makes YAMLScript a very complete language
  * No Java or JVM involved

* `ys` is the YAMLScript CLI
  * Try it using:

      . <(curl -sL yamlscript.org/try-ys)

  * Install it using:

      curl -sL yamlscript.org/install | bash


## Using the Makefile

* `make dependencies`

  Install needed programs and modules locally in current directory.

* `make data-ys`

  Load the example YS program `data.ys` using the `ys` CLI
  Try:

    make data-ys n=3

* `make data-python`

  Load the example YS program `data.ys` using Python's `yamlscript.py` module.
