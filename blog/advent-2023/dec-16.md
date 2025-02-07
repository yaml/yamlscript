---
title: Let Lambda Come Over
# date: 2023-12-16
---

We know the names of Santa's reindeer: Dasher, Dancer, Prancer, Vixen, Comet,
Cupid, Donner, Blitzen and Rudolph.
And his elves: Alabaster Snowball, Bushy Evergreen, Pepper Minstix, Shinny
Upatree, Sugarplum Mary, Wunorse Openslae, and the head elf, Bernard.

But we don't know the names of Santa's lambda reindeer and elves.
Why would we?
They are anonymous!
They're also the hardest working of the bunch.


### Welcome to Day 16 of the YS Advent Calendar

Today we're going to talk about YS lambdas and their frontman named "let".

It's a kinda of a big deal in your programming journey when you realize the
power of *NOT* naming things, especially functions.
Next you learn about functions that take functions and turn them into other
functions and your head explodes.
(Not literally because then you are dead and can't enjoy the power of lambdas).

A lambda is a general term for an anonymous function, ie one that is not named.
In Clojure you create can create them in two different ways.
Here's a simple lambda that squares a number:

```clojure
;; Using the fn keyword
(fn [x] (* x x))
;; Using the #() reader macro
#(* %1 %1)
```

Note: Clojure just calls them "anonymous functions".

Here's both of those in YS:

```yaml
# Using the fn keyword
fn(x): x * x
# Using the #() reader macro
\(%1 * %1)
```

The latter method is shorter but the former is more flexible.

Typically you use lambdas to return closures, which are functions that have
access to the variables in the scope where they were created.

Here's a function the returns a function that adds a number to another number:

```yaml
defn adder(x): \(%1 + x)
add-5 =: adder(5)
add-5: 10           # => 15
```

In the `adder` function, the `x` variable gets created in the scope of the
function and is not available outside of it.
But we returned a function that "closed over" it so that it could be used by
the returned function later on.


## let in the lexicals!

In the `adder` function, the `x` variable was a parameter to the function and
created a lexical variable whose scope was the function body.
How do we create more lexical variables in to use in a function?

In Python you we get the lexicals `x`, `y` and `z` by defining them in the
function body:

```python
def foo(x):
    y = x + 1
    z = y / 2
    return z
```

Pretty clean and simple.

In Clojure we use the `let` keyword to create more lexical variables:

```clojure
(defn foo [x]
  (let [y (+ x 1)
        z (/ y 2)]
    z))
```

The `let` keyword takes a vector (array in `[]`) of bindings and then a body
of code to execute.
The binding vector contains one or more pairs of a name (symbol) and an
expression that evaluates to a value.

The Clojure code looks a little more complicated than the Python code.
That's the price for needing to write everything the same way with
parenthesized lists.

Let's see how YS does it:

```yaml
defn foo(x):
  y =: x + 1
  z =: y / 2
  =>: z
```

How can that be the same as the Clojure code?!?!
There's no `let` keyword.
Let's try compiling it and see what happens:

```bash
$ ys -c let.ys
(defn foo [x] (let [y (_+ x 1) z (/ y 2)] z))
```

Well look at that!
It generated the `let` keyword for us.

The `a =: b` YS syntax has super powers.
When used at th top level in a program it simply creates a `def` expression like
`(def a b)`, but when used inside another YS mapping it turns into a `let`!
Not only that, but when their are multiple `x := y` expressions in a row, they
are all put into the same `let` binding vector, just like you'd do if you wrote
the Clojure code by hand.

The big win here is that the code you write looks clean like the Python code,
but works perfectly as Clojure code.
It's one of my favorite features of YS.
The need for lexical variables is constant and this makes it painless.


### LoL!!!

Let and Lambdas are 2 of the most important features for Lisp (thus Clojure)
programmers.
There's a famous book called "Let Over Lambda" that is a must read for any
serious Lisp programmer.
(I haven't read it yet, but I am serious, therefore it's a must!)

Here's a great snippet from the book:

!!! quote

    Sometimes it's called a closure, other times a saved lexical environment.
    Or, as some of us like to say, let over lambda.
    Whatever terminology you use, mastering this concept of a closure is the
    first step to becoming a professional lisp programmer.
    In fact, this skill is vital for the proper use of many modern programming
    languages, even ones that don't explicitly contain let or lambda, such as
    Perl or JavaScript.

Let over Lambda (LoL!) is built around this simple Lisp idiom:

```lisp
(let ((x 0))
  (lambda () x))
```

But there' a little more to it than that.
In most Lisps the `x` value is mutable.
That means the lambda can change the value of `x` when it is called.
This is a very powerful feature because it allows you to create functions that
can hold state.
You can use this to create iterators, generators, and even object oriented
systems.

But in Clojure all values are immutable.
This is foundational to making Clojure a functional and thread safe language.
But Clojure is also famous for being a **practical** (functional and thread
safe) language.
It has a kind of value that _is_ mutable.
These are called atoms.

Let's make the quintessential LoL example, a counter, in Clojure:

```clojure
(defn new-counter [n]
  (let [x (atom n)]
    (fn [] (swap! x inc))))
(def counter (new-counter 10))
(println (counter)) # => 11
(println (counter)) # => 12
(println (counter)) # => 13
```

Writing this in YS is a simple port:

```yaml
!yamlscript/v0
defn new-counter(n):
  x =: atom(n)
  =>: \(swap! x inc)
counter =: new-counter(10)
say: counter() # => 11
say: counter() # => 12
say: counter() # => 13
```

Someone told me the other day that YS was a "lol language".
This must be what they meant.

The lols continue tomorrow, on Day 17 of the YS Advent Calendar!
