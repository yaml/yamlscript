---
title: ys - The YS CLI Tool
talk: 0
---

The YS `ys` command line tool is the primary way to run, load and compile YS
programs.

!!! note

    Loading is essentially the same as running, but the result is output is
    printed as JSON.

Here's the `ys --help` output:

```text
$ ys --help

ys - The YS Command Line Tool - v0.3.0

Usage: ys [<option...>] [<file>]

Options:

  -e, --eval YSEXPR        Evaluate a YS expression
                             multiple -e values are joined by newline
  -l, --load               Output the (compact) JSON of YS evaluation
  -f, --file FILE          Explicitly indicate input file

  -c, --compile            Compile YS to source or an artifact

  -p, --print              Print the final evaluation result value
  -o, --output FILE        Output file for --load or --compile
  -s, --stream             Output all results from a multi-document stream

  -T, --to FORMAT          Output format for --load:
                             json, yaml, csv, tsv, edn
                           or target for --compile:
                             bb, clj, clj+, bin, go, dir, lib, so, dylib, dll, h, js, html, wasm
  -J, --json               Output (pretty) JSON for --load
  -Y, --yaml               Output YAML for --load
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

      --install            Install the libys shared library
      --upgrade            Upgrade both ys and libys
      --install-m2         Install the ys.v0 jars into ~/.m2

      --version            Print version and exit
  -h, --help               Print this help and exit
```

----

Let's start with a YAML file (`some.yaml`) that wants to use data from another
YAML file and also do some simple calculations:

```yaml
!ys-0:

::
  name =: "World"
  data =: load("data1.yaml")
  fruit =: data.food.fruit

num: 123
greet:: "$(data.hello.rand-nth()), $name!"
eat:: fruit.shuffle().first()
drink:: (["Bar"] * 3).join(', ').str('!!!')
```

Here's the other YAML file (`data1.yaml`):

```yaml
food:
  fruit:
  - apple
  - banana
  - cherry
  - date

hello:
- Aloha
- Bonjour
- Ciao
- Dzień dobry
```

We can "load" the YAML/YS file with the `ys` command and it will print the
result as JSON:

```bash
$ ys -l some.yaml
{"num":123,"greet":"Bonjour, World!","eat":"apple","drink":"Bar, Bar, Bar!!!"}
```

We can also format the output as YAML:

```bash
 ys -lY some.yaml
num: 123
greet: Ciao, World!
eat: cherry
drink: Bar, Bar, Bar!!!
```

----

Here's a tiny YS program called `program.ys`:

```yaml
!ys-0

defn main(name='world' n=3):
  greet: name n

defn greet(name, times=1):
  each [i (1 .. times)]:
    say: "$i) Hello, $name!"
```

We can run this program with the `ys` command:

```bash
$ time ys program.ys
1) Hello, world!
2) Hello, world!
3) Hello, world!

real	0m0.021s
user	0m0.014s
sys	0m0.007s

```

Pretty fast, right?

We can pass in arguments:

```bash
$ ys program.ys Bob 2
 ys program.ys Bob 2
1) Hello, Bob!
2) Hello, Bob!
```

----

To see what Clojure code is being generated under the hood:

```clojure
$ ys -c program.ys
(declare greet)
(defn main
  ([name n] (greet name n))
  ([name] (main name 3))
  ([] (main "world" 3)))
(defn greet
  ([name times] (each [i (rng 1 times)] (say (str i ") Hello, " name "!"))))
  ([name] (greet name 1)))
(apply main ARGS)
```

----

When debugging, you can see the output of each compilation stage by adding the
`-d` option:

```text
$ ys -cd program.ys
*** parse output ***
({:+ "+MAP", :! "ys-0"}
 {:+ "=VAL", := "defn main(name='world' n=3)"}
 {:+ "+MAP"}
 {:+ "=VAL", := "greet"}
 {:+ "=VAL", := "name n"}
 {:+ "-MAP"}
 {:+ "=VAL", := "defn greet(name, times=1)"}
 {:+ "+MAP"}
 {:+ "=VAL", := "each [i (1 .. times)]"}
 {:+ "+MAP"}
 {:+ "=VAL", := "say"}
 {:+ "=VAL", :$ "$i) Hello, $name!"}
 {:+ "-MAP"}
 {:+ "-MAP"}
 {:+ "-MAP"}
 {:+ "-DOC"})

*** compose output ***
{:! "ys-0",
 :%
 [{:= "defn main(name='world' n=3)"}
  {:% [{:= "greet"} {:= "name n"}]}
  {:= "defn greet(name, times=1)"}
  {:%
   [{:= "each [i (1 .. times)]"}
    {:% [{:= "say"} {:$ "$i) Hello, $name!"}]}]}]}

*** resolve output ***
{:xmap
 [{:defn "defn main(name='world' n=3)"}
  {:xmap [{:expr "greet"} {:expr "name n"}]}
  {:defn "defn greet(name, times=1)"}
  {:xmap
   [{:expr "each [i (1 .. times)]"}
    {:xmap [{:expr "say"} {:xstr "$i) Hello, $name!"}]}]}]}

*** build output ***
{:xmap
 [[{:Sym defn} {:Sym main} nil]
  [{:Lst
    [{:Vec [{:Sym name} {:Sym n}]}
     {:xmap [{:Sym greet} [{:Sym name} {:Sym n}]]}]}
   {:Lst
    [{:Vec [{:Sym name}]} {:Lst [{:Sym main} {:Sym name} {:Int 3}]}]}
   {:Lst [{:Vec []} {:Lst [{:Sym main} {:Str "world"} {:Int 3}]}]}]
  [{:Sym defn} {:Sym greet} nil]
  [{:Lst
    [{:Vec [{:Sym name} {:Sym times}]}
     {:xmap
      [[{:Sym each}
        {:Vec [{:Sym i} {:Lst [{:Sym rng} {:Int 1} {:Sym times}]}]}]
       {:xmap
        [{:Sym say}
         {:Lst
          [{:Sym str}
           {:Sym i}
           {:Str ") Hello, "}
           {:Sym name}
           {:Str "!"}]}]}]}]}
   {:Lst
    [{:Vec [{:Sym name}]}
     {:Lst [{:Sym greet} {:Sym name} {:Int 1}]}]}]]}

*** transform output ***
{:xmap
 [[{:Sym defn} {:Sym main} nil]
  [{:Lst
    [{:Vec [{:Sym name} {:Sym n}]}
     {:xmap [{:Sym greet} [{:Sym name} {:Sym n}]]}]}
   {:Lst
    [{:Vec [{:Sym name}]} {:Lst [{:Sym main} {:Sym name} {:Int 3}]}]}
   {:Lst [{:Vec []} {:Lst [{:Sym main} {:Str "world"} {:Int 3}]}]}]
  [{:Sym defn} {:Sym greet} nil]
  [{:Lst
    [{:Vec [{:Sym name} {:Sym times}]}
     {:xmap
      [[{:Sym each}
        {:Vec [{:Sym i} {:Lst [{:Sym rng} {:Int 1} {:Sym times}]}]}]
       {:xmap
        [{:Sym say}
         {:Lst
          [{:Sym str}
           {:Sym i}
           {:Str ") Hello, "}
           {:Sym name}
           {:Str "!"}]}]}]}]}
   {:Lst
    [{:Vec [{:Sym name}]}
     {:Lst [{:Sym greet} {:Sym name} {:Int 1}]}]}]]}

*** construct output ***
{:Top
 [{:Lst [{:Sym declare} {:Sym greet}]}
  {:Lst
   [{:Sym defn}
    {:Sym main}
    nil
    {:Lst
     [{:Vec [{:Sym name} {:Sym n}]}
      {:Lst [{:Sym greet} {:Sym name} {:Sym n}]}]}
    {:Lst
     [{:Vec [{:Sym name}]} {:Lst [{:Sym main} {:Sym name} {:Int 3}]}]}
    {:Lst [{:Vec []} {:Lst [{:Sym main} {:Str "world"} {:Int 3}]}]}]}
  {:Lst
   [{:Sym defn}
    {:Sym greet}
    nil
    {:Lst
     [{:Vec [{:Sym name} {:Sym times}]}
      {:Lst
       [{:Sym each}
        {:Vec [{:Sym i} {:Lst [{:Sym rng} {:Int 1} {:Sym times}]}]}
        {:Lst
         [{:Sym say}
          {:Lst
           [{:Sym str}
            {:Sym i}
            {:Str ") Hello, "}
            {:Sym name}
            {:Str "!"}]}]}]}]}
    {:Lst
     [{:Vec [{:Sym name}]}
      {:Lst [{:Sym greet} {:Sym name} {:Int 1}]}]}]}
  {:Lst [{:Sym +++} {:Lst [{:Sym apply} {:Sym main} {:Sym ARGS}]}]}]}

*** print output ***
"(declare greet)(defn main  ([name n] (greet name n)) ([name] (main name 3))...

(declare greet)
(defn main
  ([name n] (greet name n))
  ([name] (main name 3))
  ([] (main "world" 3)))
(defn greet
  ([name times] (each [i (rng 1 times)] (say (str i ") Hello, " name "!"))))
  ([name] (greet name 1)))
(+++ (apply main ARGS))
```

## Standard Modules in Expressions

YAMLScript expressions supplied with `-e` have the standard module aliases
available automatically, including `fs`, `http`, `json`, and `yaml`.
Positional expression shorthand has the same behavior:

```bash
ys -pe 'json/dump({})'
```

This is runtime setup in the current namespace.
It preserves existing aliases and lets leading `ns` and `use` declarations
establish their own aliases first.
The input YAML, its mode and tags, and the generated compilation output are
unchanged; nothing is wrapped in `=>:` or inserted into the document stream.
Raw Clojure evaluation with `-C` keeps its existing behavior.

Script files and compiled programs opt in explicitly:

```yaml
!ys-0
use: v0
say: json/dump({})
```

`use: ys::v0` is equivalent to `use: v0`.
The umbrella import adds aliases for available public standard modules without
referring their functions into the current namespace.
It skips modules disabled by `YS_MODULES` or unavailable in the runtime,
including restricted WASI modules.
Explicit individual imports continue to report errors for those modules.
Repeating the umbrella import is harmless; modifiers are not supported.
When a file is followed by `-e`, automatic imports apply only while evaluating
the expressions, after the file has run.

## Compiling Programs

`ys -c` writes Clojure to standard output.
With `--output`, the filename selects the compilation target:

| Output | Target | Result |
|--------|--------|--------|
| `foo` or `foo.exe` | `bin` | Native executable |
| `foo.go` | `go` | Generated Go source |
| `foo/` | `dir` | Buildable Go project |
| `foo.so`, `foo.dylib`, `foo.dll` | `lib` | Shared library |
| `foo.h` | `h` | FFI header |
| `foo.js` | `js` | Browser-target Wasm bytes |
| `foo.html` | `html` | HTML runner and companion `foo.js` |
| `foo.wasm` | `wasm` | WASI preview 1 module |
| `foo.clj` | `clj` | Clojure source |
| `foo.bb` | `bb` | Executable Babashka script |

An explicit `--to` selects the target regardless of the filename and implies
`--compile`.
Unknown extensions require `--to`.
Binary, library, header, directory, and Wasm targets require an output path.
For artifact targets, a `.ys` input supplies a default output in the current
directory: its basename with the final `.ys` replaced by the target extension.
`--to=bin` removes `.ys`; `--to=dir` creates a directory with that basename.
`--to=wasm` writes `.wasm`, and `--to=js` writes `.js`.
`--to=so`, `--to=dylib`, and `--to=dll` are aliases for `--to=lib` that select
the corresponding default filename extension.
`--to=lib` chooses `.so`, `.dylib`, or `.dll` for the target platform.
Add `,h` to a library target to also write its matching header, for example
`-Tso,h` or `-Tdylib,h,darwin/amd64`.
An explicit header path in `--output` takes precedence over the matching name.
`--to=h` writes `.h`; `--to=html` writes `.html` and a companion `.js`.
Use `-Tjs,html` to request the same pair with `.js` as the primary output.
This also applies to cross-compilation.
For example, `ys sample/rosetta-code/99-bottles-of-beer.ys -cTbin` writes
`./99-bottles-of-beer`.
Using `-cTwasm` instead writes `./99-bottles-of-beer.wasm`.
Text targets `go`, `clj`, `clj+`, and `bb` continue to use standard output when
no output path is supplied.
Existing output files, directories (even empty ones), and symlinks cause an
error.
Normal data output without `--compile` retains its existing behavior.
Artifact compilation shows a progress line and elapsed time on standard error.
In a terminal, a dot appears each second and the final status replaces the line.
The success mark is green and the failure mark is red; `NO_COLOR` disables color.
Gloat's build diagnostics are shown only on failure.

```bash
ys foo.ys -c -o foo
ys foo.ys --to=bin -o foo.xyz
ys -ce 'say: 42' -o answer
ys -c - -o answer < foo.ys
ys foo.ys -c -o foo,darwin/amd64
ys foo.ys --to=bin,darwin/amd64 -o foo.xyz
ys foo.ys -c -o lib/foo.so,include/foo.h
ys foo.ys -c -o foo.so,.h,darwin/amd64
ys foo.ys -c -o foo.js,.html
ys foo.ys -c -Tjs,html
ys foo.ys -c -o assets/foo.js,pages/foo.html
ys foo.ys -c -Thtml,-Xserve
ys foo.ys -c -Tbin,-Xprune
ys foo.ys -c -o foo,-Xprune
```

Compilation specifications have the form `PRIMARY[,MODIFIER...]`.
Modifiers can select a companion output, an `OS/ARCH` platform, or a Gloat
processing extension written as `-Xname` or `-Xname=value`.
Modifiers can follow either `--to` or `--output`.
The old semicolon form is not supported.
Multiple Gloat extensions must each include `-X`, for example
`-Twasm,-Xprune,-Xgzip`.
They are passed unchanged to Gloat, which validates their names, values, and
target compatibility.
`-Xserve` and `-Xopen` imply an HTML companion.
Without an explicit output, they write persistent `foo/index.html` and
`foo/index.js` files and serve `http://localhost:8000/foo/index.html`.
An explicit output retains its exact name and writes its companion beside it.
Serving rejects explicitly selected JS and HTML files in different directories.
Program arguments belong in the page URL query, separated by commas.
Each argument is percent-decoded, so `?one,two` passes two arguments and
`?one%2Ctwo` passes one argument containing a comma.
The `-Xhtml`, `-Xserve`, and `-Xopen` extensions do not accept values.
YAMLScript publishes only the primary artifact and any declared companion.
Use the companion syntax when an additional generated file must be retained.
An extension-only companion replaces the primary extension and keeps its
location.
Shared libraries publish a header only when requested.
A standalone `.h` request builds a temporary shared library and retains its
header, using Gloat's `EXPORT` declarations and ABI.
Browser `.js` files contain Wasm, not JavaScript source.
Serve the generated HTML and Wasm through an HTTP server to run them.
HTML companions reference the final relative Wasm location.

Compilation uses Gloat's Glojure engine, from either native `ys` engine.
`YS_GLOAT` can select a specific Gloat executable.
Otherwise `ys` looks beside its executable and on `PATH`.
If Gloat is missing, `ys` must be installed under a writable `PREFIX/bin/`
directory; it installs Gloat into that prefix through `https://in-1.cc`.
The prefix restriction applies only when Gloat needs installation.
Gloat manages build dependencies; shared-library and header cross-compilation
also require the target C toolchain.

For development, `make -C ys test-compile` runs the compiler wrapper tests.
`make -C ys test-compile-real` also builds real artifacts, calls the shared
library, checks cross-compilation, and runs browser-target Wasm under Node
and WASI under Wasmtime.
Makes provisions the test tools.
Use `YAMLSCRIPT_ENGINE=graalvm` to select the GraalVM CLI for these checks.
Generated project Makefiles inherit Makes' restriction on paths containing
spaces; choose a directory without spaces when building through that Makefile.
