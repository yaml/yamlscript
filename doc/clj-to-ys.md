---
title: Learning YS from Clojure
---

YS (YAMLScript) works by translating code written in YS to Clojure code, which
is then evaluated.

A good way to learn YS is to convert existing Clojure programs to YS.

This tutorial will guide you through the process of converting various Clojure
programs to idiomatic YS a step at a time.

For each Clojure program, we will:

* Start with a working Clojure program
* Refactor some parts of the program to YS
* Repeat until YS is idiomatic
* Have working YS code every step of the way

If you don't know Clojure, that's okay.
We're starting simple and you can learn 2 languages for the price of one.

Let's get started!


## Hello World

Since we are obligated as programmers to start with a "Hello, World!" program,
let's look at the simplest hello-world in Clojure:

```clojure
(println "Hello, World!")
```

It turns out the this program is already valid YS!

```sh
$ ys -e '(println "Hello, World!")'
Hello, World!
```

Let's save it to a file called `program.ys` and run it.

```yaml
(println "Hello, World!")
```

```sh
$ ys program.ys
$
```

Dang. Nothing happened.

Let's compile it to Clojure to see what's going on.

```sh
$ ys -c program.ys
"(println \"Hello, World!\")"
```

Ah! It compiled to a string, because we forgot to add `!yamlscript/v0` to the
top of the file.
All YAML files are valid YS files.
They won't evaluate any code unless you explicitly tell them to.

```yaml
!yamlscript/v0
(println "Hello, World!")
```

```sh
$ ys program.ys
Hello, World!
```

There we go!

Let's make it idiomatic now.

```yaml
!yamlscript/v0
say: 'Hello, World!'
```

Look mom, no parentheses!

We turned the YAML scalar into a single pair mapping, and we changed the
double-quoted string to a single-quoted string.

Single-quoted strings are preferred in YS unless you need interpolation
or special escaped characters.

We also changed the `println` function to the `say` function, because who has
time to type `println` when you just want to `say` something?!?!

Let's compile it back to Clojure to be honest with ourselves.

```sh
$ ys -c program.ys
(say "Hello, World!")
```

We got our Clojure parentheses and double-quoted string back.
Clojure doesn't use single quotes for strings.

We still have `say` instead of `println`, but that's okay because `ys -c`
compiles to Clojure code intended to be run by a YS runtime, and `say` is part
of the YS standard library.


## Hello 2.0

Let's make our little program a little more interesting.

```clojure
(defn hello
  ([name] (println (str "Hello, " name "!")))
  ([] (hello "World")))
(hello)
(hello "YS")
```

We've defined a function `hello` that takes an optional `name` argument.
If `name` is not provided, it defaults to `"World"`.

Let's convert this to YS, but change as little as possible.

```yaml
!yamlscript/v0
=>: !clj |
  (defn hello
    ([name] (println (str "Hello, " name "!")))
    ([] (hello "World")))
  (hello)
  (hello "YS")
```

Hmm. We added 2 lines to the top and then indented the Clojure code.
Does that work?

```sh
$ ys program.ys
Hello, World!
Hello, YS!
```

Apparently it does!

We already know about the first line.
The `=>` is a special key in YS, when you need to write a YAML key/value pair,
but you only have a value.
The compiler simply removes the `=>` and uses the value as the expression.

What did here is keep the entire Clojure code string intact using a YAML literal
scalar (think heredocs) and then use the `!clj` tag to tell the YS compiler to
treat the string as Clojure code.

The `!clj` tag is a way to write Clojure things that YS does not yet support.
But it can also be a good first step to converting Clojure code to YS.

Let's keep going by leaving the function defn alone but playing with the
function calls.

```yaml
!yamlscript/v0
=>: !clj |
  (defn hello
    ([name] (println (str "Hello, " name "!")))
    ([] (hello "World")))
hello:
hello: 'YS'
```

We made the 2 calls to `hello` into YAML mapping pairs.
The first one has no value, and that's valid in YS when a function has no
arguments.

Now let's convert the function defn to YS.

```yaml
!yamlscript/v0
defn hello:
 (name): (println (str "Hello, " name "!"))
 (): (hello "World")
hello:
hello: 'YS'
```

That's how you write a multi-arity function in YS.
It's advanced stuff and you already learned it during hello-world!
Take a moment! You deserve it!


```yaml
!yamlscript/v0
defn hello(name='world'):
  (println (str "Hello, " name "!"))
hello:
hello: 'YS'
```

Hey! What happened to our multi-arity accomplishment?
We don't need it here in YS, because YS has support for default function
arguments.

Let's finish up with a little interpolation.

```yaml
!yamlscript/v0
defn hello(name='world'):
  say: "Hello, $name!"
hello:
hello: 'YS'
```

That's some idiomatic YS if I've ever seen it!


## FizzBuzz

Let's continue our journey of refactoring cliche coding conundrums with
the classic [FizzBuzz](https://wikipedia.org/wiki/Fizz_buzz).

Here's a working Clojure implementation I found at [Rosetta Code](
https://rosettacode.org/wiki/FizzBuzz#Clojure).

```clojure
(defn fizzbuzz [start finish]
  (map (fn [n]
         (cond
           (zero? (mod n 15)) "FizzBuzz"
           (zero? (mod n 3)) "Fizz"
           (zero? (mod n 5)) "Buzz"
           :else n))
    (range start finish)))

(doseq [x (fizzbuzz 1 101)]
  (println x))
```

We'll skip the `!clj` step this time and start by making this a top level YAML
mapping.

```yaml
!yamlscript/v0

defn fizzbuzz(start finish):
  (map (fn [n]
         (cond
           (zero? (mod n 15)) "FizzBuzz"
           (zero? (mod n 3)) "Fizz"
           (zero? (mod n 5)) "Buzz"
           :else n))
    (range start finish))

doseq [x (fizzbuzz 1 101)]:
  (println x)
```

It works! Trust me! Don't do that! Run it yourself! But it works! Trust me!

All we did was turn the top level expressions into YAML mapping pairs, by
removing the outer parentheses and adding a colon in the middle.

We also changed the defn args to use parens instead of square brackets.

Let's make more expressions into pairs now.

```yaml
!yamlscript/v0

defn fizzbuzz(start finish):
  map:
    fn(n):
      cond:
        (zero? (mod n 15)): "FizzBuzz"
        (zero? (mod n 3)): "Fizz"
        (zero? (mod n 5)): "Buzz"
        else: n
    range: start finish

doseq [x (fizzbuzz 1 101)]:
  println: x
```

We also changed `:else` to `else` because YS likes it that way.

Does it work?
You betcha!
Are we done?
Heck no!

```yaml
!yamlscript/v0

defn fizzbuzz(start finish):
  map _ (start .. finish):
    fn(n):
      cond:
        (zero? (mod n 15)): 'FizzBuzz'
        (zero? (mod n 3)): 'Fizz'
        (zero? (mod n 5)): 'Buzz'
        else: n

doseq [x (fizzbuzz 1 100)]:
  println: x
```

Ok, hmm. We moved the range up to the top of the map call but put a `_` right
before it.
And it's not a range call anymore, it's some operator expression.

`..` is the `rng` operator in YS and the end is inclusive so we didn't need to
say 101 when we meant 100.

The `_` is a placeholder for the pair value to go.
In Clojure, many functions take a function as the first argument.
If we need to actually define a big function there it would be nicer if we could
do it last.
In these cases `_` is our friend.

Double quotes to single. What's next?

```yaml
!yamlscript/v0

defn fizzbuzz(start finish):
  map _ (start .. finish):
    fn(n):
      cond:
        (mod n 15).!: 'FizzBuzz'
        (mod n 3).!:  'Fizz'
        (mod n 5).!:  'Buzz'
        else:         n

doseq x (fizzbuzz 1 100):
  say: x
```

We replaced the `zero?` calls with a `.!` (short for `.falsey?()`) call.
We also removed the square brackets from the `doseq` call because YS is cool
like that.

Getting better...

In YS, if you define a function called `main` it will be called automatically
when the program is run.

Let's try that.

```yaml
!yamlscript/v0

defn main(start=1 finish=100):
  each x (start .. finish):
    say:
      fizzbuzz: x

defn- fizzbuzz(n):
  cond:
    (mod n 15).!: 'FizzBuzz'
    (mod n 3).!:  'Fizz'
    (mod n 5).!:  'Buzz'
    else:         n
```

We moved the heavy lifting code into a private function called `fizzbuzz` and
simply call it from the `main` function for every number in our range.

We also made it so we can pass in the start and finish values as arguments:

```sh
$ ys program.ys 35 42
Buzz
Fizz
37
38
Fizz
Buzz
41
Fizz
```

Pretty sweet!

Let's tidy up this code a bit more, and come down from our fizzy buzz.

```yaml
!yamlscript/v0

defn main(start=1 finish=100):
  each x (start .. finish): x.fizzbuzz().say()

defn- fizzbuzz(n):
  cond:
    (n % 15).!: 'FizzBuzz'
    (n % 3).!:  'Fizz'
    (n % 5).!:  'Buzz'
    else:       n
```

We made the body of `main` into a single pair by chaining the `x`, `fizzbuzz()`,
and `say()` together.
I wouldn't say this is idiomatic YS, it's a little less readable imho, but
sometimes we got to show off a little.

This is probably how I'd write that part:

```yaml
  each x (start .. finish):
    say: fizzbuzz(x)
```

See how `fizzbuzz` comes before the paren, not inside it?
We call that a [YeS expression](yes.md) and it's definitely idiomatic!

Finally we replaced the `mod` calls with the `%` operator.
To be fair, `%` is the `rem` operator and `%%` is the `mod` operator in but with
positive numbers they do the same thing.

I'm fizzed out! Let's move on!


## To Be Continued...

We'll continue this journey soon. I promise.

I have lots more Idiomatic YS to show you.

Stay tuned!
