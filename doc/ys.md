---
title: ys - The YAMLScript Command Line Tool
---

The YAMLScript `ys` command line tool is the primary way to run, load and
compile YAMLScript programs.

> Loading is essentially the same as running, but the result is output is
printed as JSON.

Here's the `ys --help` output:

```text
$ ys --help

ys - The YAMLScript (YS) Command Line Tool - v0.1.72

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
```

----

Let's start with a YAML file (`some.yaml`) that wants to use data from another
YAML file and also do some simple calculations:

```yaml
!yamlscript/v0/

=>:
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

We can "load" the YAML/YAMLScript file with the `ys` command and it will print
the result as JSON:

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

Here's a tiny YAMLScript program called `program.ys`:

```yaml
!yamlscript/v0

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

You can compile the program to a native binary executable:

```text
$ time ys -b program.ys 
* Compiling YAMLScript 'program.ys' to 'program' executable
* Setting up build env in '/tmp/tmp.xU8K3OPymt'
* This may take a few minutes...
[1/8] Initializing		(2.8s @ 0.14GB)
[2/8] Performing analysis		(9.1s @ 0.33GB)
[3/8] Building universe		(1.2s @ 0.39GB)
[4/8] Parsing methods		(1.4s @ 0.41GB)
[5/8] Inlining methods		(0.9s @ 0.49GB)
[6/8] Compiling methods		(10.6s @ 0.50GB)
[7/8] Layouting methods		(1.0s @ 0.50GB)
[8/8] Creating image		(1.5s @ 0.44GB)
* Compiled YAMLScript 'program.ys' to 'program' executable

real	0m36.340s
user	4m34.165s
sys	0m3.915s

$ time ./program Bob 2
1) Hello, Bob!
2) Hello, Bob!

real	0m0.007s
user	0m0.003s
sys	0m0.004s
```

As you can see, the native binary is faster than the interpreted version, but
the compilation takes quite a long time.

----

When debugging, you can see the output of each compilation stage by adding the
`-d` option:

```text
$ ys -cd program.ys
*** parse output ***
({:+ "+MAP", :! "yamlscript/v0"}
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
{:! "yamlscript/v0",
 :%
 [{:= "defn main(name='world' n=3)"}
  {:% [{:= "greet"} {:= "name n"}]}
  {:= "defn greet(name, times=1)"}
  {:%
   [{:= "each [i (1 .. times)]"}
    {:% [{:= "say"} {:$ "$i) Hello, $name!"}]}]}]}

*** resolve output ***
{:pairs
 [{:defn "defn main(name='world' n=3)"}
  {:pairs [{:exp "greet"} {:exp "name n"}]}
  {:defn "defn greet(name, times=1)"}
  {:pairs
   [{:exp "each [i (1 .. times)]"}
    {:pairs [{:exp "say"} {:vstr "$i) Hello, $name!"}]}]}]}

*** build output ***
{:pairs
 [[{:Sym defn} {:Sym main} nil]
  [{:Lst
    [{:Vec [{:Sym name} {:Sym n}]}
     {:pairs [{:Sym greet} [{:Sym name} {:Sym n}]]}]}
   {:Lst
    [{:Vec [{:Sym name}]} {:Lst [{:Sym main} {:Sym name} {:Int 3}]}]}
   {:Lst [{:Vec []} {:Lst [{:Sym main} {:Str "world"} {:Int 3}]}]}]
  [{:Sym defn} {:Sym greet} nil]
  [{:Lst
    [{:Vec [{:Sym name} {:Sym times}]}
     {:pairs
      [[{:Sym each}
        {:Vec [{:Sym i} {:Lst [{:Sym rng} {:Int 1} {:Sym times}]}]}]
       {:pairs
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
{:pairs
 [[{:Sym defn} {:Sym main} nil]
  [{:Lst
    [{:Vec [{:Sym name} {:Sym n}]}
     {:pairs [{:Sym greet} [{:Sym name} {:Sym n}]]}]}
   {:Lst
    [{:Vec [{:Sym name}]} {:Lst [{:Sym main} {:Sym name} {:Int 3}]}]}
   {:Lst [{:Vec []} {:Lst [{:Sym main} {:Str "world"} {:Int 3}]}]}]
  [{:Sym defn} {:Sym greet} nil]
  [{:Lst
    [{:Vec [{:Sym name} {:Sym times}]}
     {:pairs
      [[{:Sym each}
        {:Vec [{:Sym i} {:Lst [{:Sym rng} {:Int 1} {:Sym times}]}]}]
       {:pairs
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
