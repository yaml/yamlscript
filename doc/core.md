---
title: Core Library Essentials
---

<!-- DO NOT EDIT — THIS FILE WAS GENERATED -->

YS has a very large set of useful functions that it inherits from Clojure's
[`clojure.core` library](
https://github.com/clojure/clojure/blob/clojure-1.12.0/src/clj/clojure/core.clj).

These functions are the bread and butter building blocks of YS
programming.

The functions are very well organized in the [Clojure Docs Quick Reference](
https://clojuredocs.org/quickref) and you can get to the documentation for each
function from there.

This document is a condensed quick reference of the functions that are most
commonly used in YS programming.

YS also has the [`ys::std`](ys-std.md) standard library that provides
additional functions.

The YS standard library replaces some Clojure functions with a version
more suited to YS.
In those cases, the original Clojure function is still available in the
[`ys::clj`](ys-clj.md) namespace.


## Number


### Arithmetic
Clojure Core:

* [+](https://clojuredocs.org/clojure.core/+), [-](https://clojuredocs.org/clojure.core/-), [*](https://clojuredocs.org/clojure.core/*), [/](https://clojuredocs.org/clojure.core//), [inc](https://clojuredocs.org/clojure.core/inc), [dec](https://clojuredocs.org/clojure.core/dec), [max](https://clojuredocs.org/clojure.core/max), [min](https://clojuredocs.org/clojure.core/min), [rand](https://clojuredocs.org/clojure.core/rand), [rand-int](https://clojuredocs.org/clojure.core/rand-int)


YS Std:

* `%`, `%%` infix operators
* `add`, `sub`, `mul`, `div` named math functions
* `sum`, `sqr`, `cube`, `sqrt`, `pow`, `abs`
* `add+`, `sub+`, `mul+`, `div+` polymorphic functions

See Also:

* [`clojure.math`](https://clojure.github.io/clojure/clojure.math-api.html)
  functions callable as `math/<func-name>` in YS.


### Comparison
Clojure Core:

* [<](https://clojuredocs.org/clojure.core/<), [>](https://clojuredocs.org/clojure.core/>), [<=](https://clojuredocs.org/clojure.core/<=), [>=](https://clojuredocs.org/clojure.core/>=)


YS Std:

* `==`, `!=` infix operators
* `eq`, `ne`, `lt`, `gt`, `le`, `ge` named comparison functions


### Cast
Clojure Core:

* [byte](https://clojuredocs.org/clojure.core/byte), [short](https://clojuredocs.org/clojure.core/short), [int](https://clojuredocs.org/clojure.core/int), [long](https://clojuredocs.org/clojure.core/long), [float](https://clojuredocs.org/clojure.core/float), [double](https://clojuredocs.org/clojure.core/double), [num](https://clojuredocs.org/clojure.core/num)


YS Std:

* `to-num`, `to-int`, `to-float` - polymorphic cast functions


### Test
Clojure Core:

* [zero?](https://clojuredocs.org/clojure.core/zero?), [pos?](https://clojuredocs.org/clojure.core/pos?), [neg?](https://clojuredocs.org/clojure.core/neg?), [even?](https://clojuredocs.org/clojure.core/even?), [odd?](https://clojuredocs.org/clojure.core/odd?), [number?](https://clojuredocs.org/clojure.core/number?)


## Boolean
Clojure Core:

* [nil?](https://clojuredocs.org/clojure.core/nil?), [true?](https://clojuredocs.org/clojure.core/true?), [false?](https://clojuredocs.org/clojure.core/false?), [boolean](https://clojuredocs.org/clojure.core/boolean)


YS Std:

* `truey?`, `falsey?`, `to-bool`, `to-booly`


## String
Clojure Core:

* [str](https://clojuredocs.org/clojure.core/str), [pr-str](https://clojuredocs.org/clojure.core/pr-str), [prn-str](https://clojuredocs.org/clojure.core/prn-str), [with-out-str](https://clojuredocs.org/clojure.core/with-out-str), [count](https://clojuredocs.org/clojure.core/count), [subs](https://clojuredocs.org/clojure.core/subs), [format](https://clojuredocs.org/clojure.core/format), [string?](https://clojuredocs.org/clojure.core/string?)


YS Std:

* `words`, `split`, `join`, `lines`, `text`, `replace`, `replace1`
* `chomp`, `trim`, `triml`, `trimr`, `lc`, `uc`, `uc1`, `index`, `pretty`

See Also:

* [`clojure.string`](https://clojure.github.io/clojure/clojure.string-api.html)
  functions callable as `str/<func-name>` in YS.


## Regular Expression
Clojure Core:

* [re-pattern](https://clojuredocs.org/clojure.core/re-pattern), [re-matches](https://clojuredocs.org/clojure.core/re-matches), [re-find](https://clojuredocs.org/clojure.core/re-find), [re-seq](https://clojuredocs.org/clojure.core/re-seq), [re-groups](https://clojuredocs.org/clojure.core/re-groups)


YS Std:

* `=~`, `!~` infix operators
* `/.../` regex literals
* `qr`


## Flow Control

## Boolean
Clojure Core:

* [not](https://clojuredocs.org/clojure.core/not), [and](https://clojuredocs.org/clojure.core/and), [or](https://clojuredocs.org/clojure.core/or)


YS Std:

* `||`, `&&`, `|||`, `&&&` infix operators
* `or?`, `and?` booly named functions


### Normal
Clojure Core:

* [if](https://clojuredocs.org/clojure.core/if), [when](https://clojuredocs.org/clojure.core/when), [if-not](https://clojuredocs.org/clojure.core/if-not), [when-not](https://clojuredocs.org/clojure.core/when-not), [if-let](https://clojuredocs.org/clojure.core/if-let), [when-let](https://clojuredocs.org/clojure.core/when-let), [if-some](https://clojuredocs.org/clojure.core/if-some), [when-some](https://clojuredocs.org/clojure.core/when-some), [cond](https://clojuredocs.org/clojure.core/cond), [condp](https://clojuredocs.org/clojure.core/condp), [case](https://clojuredocs.org/clojure.core/case), [do](https://clojuredocs.org/clojure.core/do), [eval](https://clojuredocs.org/clojure.core/eval), [loop](https://clojuredocs.org/clojure.core/loop), [recur](https://clojuredocs.org/clojure.core/recur), [while](https://clojuredocs.org/clojure.core/while)


YS Std:

* `if-lets`, `when-lets`, `call`, `each`, `exit`, `sleep`


### Exception
Clojure Core:

* [try](https://clojuredocs.org/clojure.core/try), [catch](https://clojuredocs.org/clojure.core/catch), [finally](https://clojuredocs.org/clojure.core/finally), [throw](https://clojuredocs.org/clojure.core/throw), [assert](https://clojuredocs.org/clojure.core/assert)


YS Std:

* `die`, `warn`, `exit`, `err`


## Function
Clojure Core:

* [fn](https://clojuredocs.org/clojure.core/fn), [defn](https://clojuredocs.org/clojure.core/defn), [defn-](https://clojuredocs.org/clojure.core/defn-), [identity](https://clojuredocs.org/clojure.core/identity), [comp](https://clojuredocs.org/clojure.core/comp), [partial](https://clojuredocs.org/clojure.core/partial), [complement](https://clojuredocs.org/clojure.core/complement), [constantly](https://clojuredocs.org/clojure.core/constantly), [->](https://clojuredocs.org/clojure.core/->), [->>](https://clojuredocs.org/clojure.core/->>), [apply](https://clojuredocs.org/clojure.core/apply), [fn?](https://clojuredocs.org/clojure.core/fn?), [ifn?](https://clojuredocs.org/clojure.core/ifn?)


YS Std:

* `\(...)` anonymous function syntax
* `.` dot chaining infix operator
* `value`, `call`


## Collection

### General
Clojure Core:

* [count](https://clojuredocs.org/clojure.core/count), [empty](https://clojuredocs.org/clojure.core/empty), [not-empty](https://clojuredocs.org/clojure.core/not-empty), [into](https://clojuredocs.org/clojure.core/into), [conj](https://clojuredocs.org/clojure.core/conj), [contains?](https://clojuredocs.org/clojure.core/contains?), [distinct?](https://clojuredocs.org/clojure.core/distinct?), [empty?](https://clojuredocs.org/clojure.core/empty?), [every?](https://clojuredocs.org/clojure.core/every?), [some](https://clojuredocs.org/clojure.core/some), [not-every?](https://clojuredocs.org/clojure.core/not-every?), [not-any?](https://clojuredocs.org/clojure.core/not-any?), [coll?](https://clojuredocs.org/clojure.core/coll?), [seq?](https://clojuredocs.org/clojure.core/seq?), [vector?](https://clojuredocs.org/clojure.core/vector?), [list?](https://clojuredocs.org/clojure.core/list?), [map?](https://clojuredocs.org/clojure.core/map?), [set?](https://clojuredocs.org/clojure.core/set?), [sorted?](https://clojuredocs.org/clojure.core/sorted?), [sequential?](https://clojuredocs.org/clojure.core/sequential?), [associative?](https://clojuredocs.org/clojure.core/associative?)


YS Std:

* `grep`, `has?`, `in?`


### Vector
Clojure Core:

* [vec](https://clojuredocs.org/clojure.core/vec), [vector](https://clojuredocs.org/clojure.core/vector), [vec-of](https://clojuredocs.org/clojure.core/vec-of), [conj](https://clojuredocs.org/clojure.core/conj), [peek](https://clojuredocs.org/clojure.core/peek), [pop](https://clojuredocs.org/clojure.core/pop), [get](https://clojuredocs.org/clojure.core/get)


### List
Clojure Core:

* [list](https://clojuredocs.org/clojure.core/list), [cons](https://clojuredocs.org/clojure.core/cons), [conj](https://clojuredocs.org/clojure.core/conj), [peek](https://clojuredocs.org/clojure.core/peek), [pop](https://clojuredocs.org/clojure.core/pop), [first](https://clojuredocs.org/clojure.core/first), [rest](https://clojuredocs.org/clojure.core/rest)


### Map
Clojure Core:

* [hash-map](https://clojuredocs.org/clojure.core/hash-map), [array-map](https://clojuredocs.org/clojure.core/array-map), [sorted-map](https://clojuredocs.org/clojure.core/sorted-map), [zipmap](https://clojuredocs.org/clojure.core/zipmap), [frequencies](https://clojuredocs.org/clojure.core/frequencies), [assoc](https://clojuredocs.org/clojure.core/assoc), [assoc-in](https://clojuredocs.org/clojure.core/assoc-in), [dissoc](https://clojuredocs.org/clojure.core/dissoc), [find](https://clojuredocs.org/clojure.core/find), [get-in](https://clojuredocs.org/clojure.core/get-in), [update-in](https://clojuredocs.org/clojure.core/update-in), [key](https://clojuredocs.org/clojure.core/key), [val](https://clojuredocs.org/clojure.core/val), [keys](https://clojuredocs.org/clojure.core/keys), [vals](https://clojuredocs.org/clojure.core/vals), [merge](https://clojuredocs.org/clojure.core/merge), [reduce-kv](https://clojuredocs.org/clojure.core/reduce-kv)


YS Std:

* `omap`, `get+`


### Sequence
Clojure Core:

* [seq](https://clojuredocs.org/clojure.core/seq), [repeat](https://clojuredocs.org/clojure.core/repeat), [range](https://clojuredocs.org/clojure.core/range), [iterate](https://clojuredocs.org/clojure.core/iterate), [cycle](https://clojuredocs.org/clojure.core/cycle), [interleave](https://clojuredocs.org/clojure.core/interleave), [interpose](https://clojuredocs.org/clojure.core/interpose), [first](https://clojuredocs.org/clojure.core/first), [second](https://clojuredocs.org/clojure.core/second), [last](https://clojuredocs.org/clojure.core/last), [rest](https://clojuredocs.org/clojure.core/rest), [next](https://clojuredocs.org/clojure.core/next), [butlast](https://clojuredocs.org/clojure.core/butlast), [nth](https://clojuredocs.org/clojure.core/nth), [take](https://clojuredocs.org/clojure.core/take), [drop](https://clojuredocs.org/clojure.core/drop), [take-while](https://clojuredocs.org/clojure.core/take-while), [drop-while](https://clojuredocs.org/clojure.core/drop-while), [conj](https://clojuredocs.org/clojure.core/conj), [concat](https://clojuredocs.org/clojure.core/concat), [map](https://clojuredocs.org/clojure.core/map), [filter](https://clojuredocs.org/clojure.core/filter), [remove](https://clojuredocs.org/clojure.core/remove), [sort](https://clojuredocs.org/clojure.core/sort), [shuffle](https://clojuredocs.org/clojure.core/shuffle), [flatten](https://clojuredocs.org/clojure.core/flatten), [for](https://clojuredocs.org/clojure.core/for), [doseq](https://clojuredocs.org/clojure.core/doseq), [dorun](https://clojuredocs.org/clojure.core/dorun), [doall](https://clojuredocs.org/clojure.core/doall), [mapcat](https://clojuredocs.org/clojure.core/mapcat), [reduce](https://clojuredocs.org/clojure.core/reduce), [keep](https://clojuredocs.org/clojure.core/keep)


YS Std:

* `..` infix rng operator
* `rng`, `reverse`


## Variable
Clojure Core:

* [def](https://clojuredocs.org/clojure.core/def), [intern](https://clojuredocs.org/clojure.core/intern), [declare](https://clojuredocs.org/clojure.core/declare), [binding](https://clojuredocs.org/clojure.core/binding), [gensym](https://clojuredocs.org/clojure.core/gensym), [var](https://clojuredocs.org/clojure.core/var), [var-get](https://clojuredocs.org/clojure.core/var-get), [resolve](https://clojuredocs.org/clojure.core/resolve), [find-var](https://clojuredocs.org/clojure.core/find-var), [alter-var-root](https://clojuredocs.org/clojure.core/alter-var-root), [var?](https://clojuredocs.org/clojure.core/var?), [bound?](https://clojuredocs.org/clojure.core/bound?)


YS Std:

* `=:`, `.=:`, `+=:`, `-=:`, `*=:`, `/=:` def/let syntax
* `value`


## I/O
Clojure Core:

* [print](https://clojuredocs.org/clojure.core/print), [printf](https://clojuredocs.org/clojure.core/printf), [println](https://clojuredocs.org/clojure.core/println), [pr](https://clojuredocs.org/clojure.core/pr), [prn](https://clojuredocs.org/clojure.core/prn), [print-str](https://clojuredocs.org/clojure.core/print-str), [println-str](https://clojuredocs.org/clojure.core/println-str), [pr-str](https://clojuredocs.org/clojure.core/pr-str), [prn-str](https://clojuredocs.org/clojure.core/prn-str), [newline](https://clojuredocs.org/clojure.core/newline), [flush](https://clojuredocs.org/clojure.core/flush), [slurp](https://clojuredocs.org/clojure.core/slurp), [spit](https://clojuredocs.org/clojure.core/spit), [with-out-str](https://clojuredocs.org/clojure.core/with-out-str), [with-open](https://clojuredocs.org/clojure.core/with-open), [with-in-str](https://clojuredocs.org/clojure.core/with-in-str)


YS Std:

* `say`, `out`, `warn`, `pp`
