---
title: YeS Expressions
---

Lisp has the concept of [S-Expressions](
https://en.wikipedia.org/wiki/S-expression) (aka `sexpr`s), nested parenthesized
expressions where the parentheses contain a function (usually a symbol bound to
a function) followed by its arguments.

YS has a concept called "YeS Expressions" (aka `ysexpr`s) which offer alternate
ways to write sexprs in a style that feels more familiar to non-Lisp
language expressions.

!!! note

    In general, YS supports writing code as sexprs, ysexprs or as block mapping
    pairs.
    Nested expressions can combine any of those forms.
    It's up to the programmer to decide which works best for them in any given
    context.

```clojure
(def var1 (sqrt (+ (* 3 4) 5)))
```

The equivalent code in Python would be:

```python
var1 = sqrt(3 * 4 + 5)
```

In YS, we could write:

```yaml
var1 =: sqrt((3 * 4) + 5)
```

There are 3 notable transformations happening here:

* Functions named by a word can be placed before the opening paren instead of
  inside it.
  e.g. `a(b c)` translates to `(a b c)`.
  Note that no whitespace can come between the `a` and the `(`.
* Prefix operations can be written infix.
  e.g. `(a + b)` translates to `(+ a b)`.
  Triplets with an operator (punctuation) symbol in the middle get this
  treatment.
* Variable assignment (aka symbol binding) can be written using ` =: ` YAML
  mapping pairs.
  e.g. `a =: b + c` translates to `(def a (+ b c))`.
  Note that whitespace is required on both sides of the `=:`.

We'll discuss the a few more details of each of these YeS expression transforms
below.

!!! note

    YS has many other transformations that strive to make YS code clean and
    easy to read and understand.
    YeS expression transformations are among the most commonly used.


## Prefix Call Notation

The most common way to call a function in non-Lisp programming languages is
`a()`, `a(b)`, `a(b c)` etc where `a` is the name of a function and `b` and `c`
are arguments that the function is called with.
Lisp languages use the same format but put the `a` inside the parens.

YS lets you do either.
Again the `a` must be next to the `(`.
IOW, `a (b c)` is not the same as `a(b c)`!

Another way to write the function call `(a b c)` in YS is to use it in a
mapping pair context:

```yaml
a: b c
# Or sometimes:
a b: c
```


## Infix Operator Notation

Lisps write binary operator expressions like:

```clojure
(+ a 5)  ;; a + 5
(> a 5)  ;; a > 5
```

That makes sense because `+` and `>` are just ordinary Lisp symbols bound to
addition and greater-than functions.

One advantage of this prefix notation is that this operations can take more than
2 arguments:

```clojure
(+ a b c d)  ;; a + b + c + d
(> a b c d)  ;; a > b > c > d  or  (a > b) && (b > c) && (c > d)
```

YeS expressions allow you to do these things:

```yaml
=>: a + b        # (+ a b)
=>: a + b + c    # (+ a b c)
=>: a + b > c    # ERROR - no operator mixing; no implicit precedence in YS
=>: (a + c) > c  # (> (+ a b) c) - Fine with parens
```

Note that we didn't need any parentheses around `a + b`.
When a YAML plain scalar with an operator triplet (or multiple forms separated
by the same operator) the parentheses are implied and thus optional.
The parentheses are implied in a few other contexts as well like:

```yaml
if a > b: c d
# Equivalent to:
if (a > b): c d
```


### When Operators are Arguments

Every once in a while you might want to pass an operator as an argument to a
function call triplet like `(apply + numbers)` and YS would wrongly
translate that to `(+ apply numbers)`.

This is easily avoided by using the prefix call notation described above:
`apply(+ numbers)`.
It is also avoided when using the block mapping pair form:

```yaml
apply +: numbers
```

Also note that the operator switching only applies to triplet forms, so
expressions like `(a +)` and `(a + b c)` are never affected.


## Assignment (Symbol Binding) Expressions

In many programming languages, like Python for instance, it's common to see
things like:

```python
a = b(c)
```

where the evaluation result of the function `b(c)` is assigned (aka bound) to
the variable (or symbol) `a`.

In Clojure you would use a `def` form:

```clojure
(def a (b c))
```

Unless it was inside a function scope, in which case you would use a `let` form:

```clojure
(defn f [b c]
  (let [a (b c)]
    (d a)))
```

In YS you can write them both the same way:

```
a =: b(c)
# and
defn f(b c):
  a =: b(c)
  d: a
```

In Clojure you can define multiple `let` bindings in a single `let` form.

```clojure
(defn f [a]
  (let [b (inc a)
        c (dec a)]
    (d b c)))
```

In YS you can do the same thing:

```yaml
defn f(a):
  b =: inc(a)
  c =: dec(a)
  d: b c
```

The YS compiler joins consecutive `=:` pairs into a single `let` form,
resulting in the same Clojure code as the example above.

In Clojure you can bind multiple symbols at once using destructuring:

```clojure
(defn f []
  (let [[a b] (c)]
    (g a b)))
```

This binds `a` and `b` to the first two elements of the sequence returned by
calling `c`.

Note that this works for a `let` expression but not for a `def` expression:

```clojure
(def [a b] (c))  ;; Syntax error
```

In YS it works fine:

```yaml
-[a b] =: c()
```


## Conclusion

YeS Expressions are a key way to help you write YS code that looks more like
the code you're used to writing in non-Lisp languages, while still retaining
the full power of Lisp (Clojure).
It's your choice how you want your code to look, and YS gives you many options
for that.

No matter what your valid YS code looks like, it always compiles to valid Lisp,
which makes it extremely reliable to reason about.

The power of Lisp is that its input expressions almost completely match the
internal evaluation forms that it compiles to.
It's very obvious what's going on under the hood, because it's the same as what
you see on the outside.
This is called [homoiconicity](https://wikipedia.org/wiki/Homoiconicity).

In dynamic languages like JavaScript, Python, Ruby or Perl you don't really get
to see what your code turns into before it is evaluated.

YS tries to offer a very flexible set of coding styles that always compile to
Lisp's extremely robust evaluation form.
