YAMLScript
==========

Program in YAML


# Synopsis

A YAMLScript program `99-bottles.ys`:

```
#!/usr/bin/env yamlscript

defn main(number=99):
  map(say):
    map(paragraph):
      (number .. 1)

defn paragraph(num): |
  $(bottles num) of beer on the wall,
  $(bottles num) of beer.
  Take one down, pass it around.
  $(bottles (num - 1)) of beer on the wall.

defn bottles(n):
  ???:
    (n == 0) : "No more bottles"
    (n == 1) : "1 bottle"
    :else    : "$n bottles"
```

Run: `yamlscript 99-bottles.ys 3`

```
3 bottles of beer on the wall,
3 bottles of beer.
Take one down, pass it around.
2 bottles of beer on the wall.

2 bottles of beer on the wall,
2 bottles of beer.
Take one down, pass it around.
1 bottle of beer on the wall.

1 bottle of beer on the wall,
1 bottle of beer.
Take one down, pass it around.
No more bottles of beer on the wall.
```

Use the YAMLScript REPL:

```
$ yamlscript
Welcome to YAMLScript [perl]

user=> nums =: (1 .. 3)
user/nums
user=> nums
(1 2 3)
user=> map(inc nums)
(2 3 4)
user=> <CTL-D>         # to exit
$
```


# Status

This is ALPHA software.
Expect things to change.


# Description

**YAMLScript** is a programming language that uses YAML as a base syntax.

See https://yamlscript.org for more info.

Proper docs coming soon.


# See Also

* [YAMLScript Site](https://yamlscript.org)
* [YAML](https://yaml.org)
* [Clojure](https://clojure.org)


# Authors

* Ingy döt Net <ingy@ingy.net>
* José Joaquín Atria <jjatria@cpan.org>


# Copyright and License

Copyright 2022-2023 by Ingy döt Net

This is free software, licensed under:

The MIT (X11) License
