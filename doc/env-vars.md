---
title: YS Environment Vars
talk: 0
---

YS has a number of environment variables that can be set to control its
behavior.
These variables each begin with `YS_` and are all uppercase with underscores
separating words.

* `YSPATH` - A colon-separated list of directories to search for YS library
  files loaded with the `use` function.
  This is the only YS environment variable that doesn't start with `YS_`, but
  there is an equivalent `YS_PATH` that can be used instead.

* `YS_PATH` - An alternative to `YSPATH` that is used if `YSPATH` is not set.

* `YS_MAVEN_REPOSITORY` - The local Maven repository used by `use :from`.

* `YS_GITLIBS_DIR` - The cache directory used by `use :from` for Gist and
  GitHub source files.

* `YS_MODULES` - A comma-separated or whitespace-separated allowlist of
  bundled modules that a program may load with `use`.
  Short names such as `fs` and full names such as `ys.fs` are accepted.
  When unset, all modules supported by the host platform may be loaded.
  Standard proxies such as `read`, `say`, and `pp` also require their owning
  `ys::fs`, `ys::io`, or `ys::pprint` module to be allowed.

* `YS_PRINT=1` - Same as `-p` (`--print`) command line option.

* `YS_STREAM=1` - Same as `-s` (`--stream`) command line option.

* `YS_OUTPUT=<file-name>` - Same as `-o` (`--output=<file-name>`) command line
  option.

* `YS_FORMAT=<yaml|json|edn|csv|tsv>` - Same as `-t` (`--to`).

* `YS_FORMATTER=<command>` - Filter compiled Clojure output through an external
  formatter command.
  When unset, `ys --compile` uses its built-in pretty printer.
  External formatters are not available in the WASI build.

* `YS_UNORDERED=1` - Same as `-u` (`--unordered`) command line option.

* `YS_XTRACE=1` - Same as `-x` (`--xtrace`) command line option.

* `YS_STACK_TRACE=1` - Same as `-S` (`--stack-trace`) command line option.

* `YS_SHOW_OPTS=1` - Print all the option values.

* `YS_SHOW_LEX=1` - Print the lexed tokens of each YS expression.

* `YS_SHOW_INPUT=1` - Print the input YS expressions.
