---
title: Core Library Essentials
---


YAMLScript has a very large set of useful functions that it inherits from
Clojure's [`clojure.core` library](
https://github.com/clojure/clojure/blob/clojure-1.12.0/src/clj/clojure/core.clj).

These functions are the bread and butter building blocks of YAMLScript
programming.

The functions are very well organized in the [Clojure Docs Quick Reference](
https://clojuredocs.org/quickref) and you can get to the documentation for each
function from there.

This document is a condensed quick reference of the functions that are most
commonly used in YAMLScript programming.

YAMLScript also has the [`ys::std`](/doc/ys-std) standard library that provides
additional functions.

The YAMLScript standard library replaces some Clojure functions with a version
more suited to YAMLScript.
In those cases, the original Clojure function is still available in the
[`ys::clj`](/doc/ys-clj) namespace.


## Number


### Arithmetic

```markys:quick-ref
+ - * / inc dec max min rand rand-int
```

YAMLScript Std:

* `%`, `%%` infix operators
* `add`, `sub`, `mul`, `div` named math functions
* `sum`, `sqr`, `cube`, `sqrt`, `pow`, `abs`
* `add+`, `sub+`, `mul+`, `div+` polymorphic functions

See Also:

* [`clojure.math`](https://clojure.github.io/clojure/clojure.math-api.html)
  functions callable as `math/<func-name>` in YAMLScript.


### Comparison

```markys:quick-ref
< > <= >=
```

YAMLScript Std:

* `==`, `!=` infix operators
* `eq`, `ne`, `lt`, `gt`, `le`, `ge` named comparison functions


### Cast

```markys:quick-ref
byte short int long float double num
```

YAMLScript Std:

* `to-num`, `to-int`, `to-float` - polymorphic cast functions


### Test

```markys:quick-ref
zero? pos? neg? even? odd? number?
```


## Boolean

```markys:quick-ref
nil? true? false? boolean
```

YAMLScript Std:

* `truey?`, `falsey?`, `to-bool`, `to-booly`


## String

```markys:quick-ref
str pr-str prn-str with-out-str count subs format string?
```

YAMLScript Std:

* `words`, `split`, `join`, `lines`, `text`, `replace`, `replace1`
* `chomp`, `trim`, `triml`, `trimr`, `lc`, `uc`, `uc1`, `index`, `pretty`

See Also:

* [`clojure.string`](https://clojure.github.io/clojure/clojure.string-api.html)
  functions callable as `str/<func-name>` in YAMLScript.


## Regular Expression

```markys:quick-ref
re-pattern re-matches re-find re-seq re-groups
```

YAMLScript Std:

* `=~`, `!~` infix operators
* `/.../` regex literals
* `qr`


## Flow Control

## Boolean

```markys:quick-ref
not and or
```

YAMLScript Std:

* `||`, `&&`, `|||`, `&&&` infix operators
* `or?`, `and?` booly named functions


### Normal

```markys:quick-ref
if when if-not when-not if-let when-let if-some when-some
cond condp case do eval loop recur while
```

YAMLScript Std:

* `if-lets`, `when-lets`, `call`, `each`, `exit`, `sleep`


### Exception

```markys:quick-ref
try catch finally throw assert
```

YAMLScript Std:

* `die`, `warn`, `exit`, `err`


## Function

```markys:quick-ref
fn defn defn- identity comp partial complement constantly
-> ->> apply fn? ifn?
```

YAMLScript Std:

* `\(...)` anonymous function syntax
* `.` dot chaining infix operator
* `value`, `call`


## Collection

### General

```markys:quick-ref
count empty not-empty into conj
contains? distinct? empty? every? some not-every? not-any?
coll? seq? vector? list? map? set? sorted? sequential? associative?
```

YAMLScript Std:

* `grep`, `has?`, `in?`


### Vector

```markys:quick-ref
vec vector vec-of conj peek pop get
```


### List

```markys:quick-ref
list cons conj peek pop first rest
```


### Map

```markys:quick-ref
hash-map array-map sorted-map zipmap frequencies
assoc assoc-in dissoc find get-in update-in
key val keys vals merge reduce-kv
```

YAMLScript Std:

* `omap`, `get+`


### Sequence

```markys:quick-ref
seq repeat range iterate cycle interleave interpose
first second last rest next butlast nth take drop take-while drop-while
conj concat map filter remove sort shuffle flatten
for doseq dorun doall mapcat reduce keep
```

YAMLScript Std:

* `..` infix rng operator
* `rng`, `reverse`


## Variable

```markys:quick-ref
def intern declare binding gensym
var var-get resolve find-var alter-var-root
var? bound?
```

YAMLScript Std:

* `=:`, `.=:`, `+=:`, `-=:`, `*=:`, `/=:` def/let syntax
* `value`


## I/O

```markys:quick-ref
print printf println pr prn
print-str println-str pr-str prn-str
newline flush slurp spit
with-out-str with-open with-in-str
```

YAMLScript Std:

* `say`, `out`, `warn`, `pp`
