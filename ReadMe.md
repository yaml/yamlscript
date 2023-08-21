YAMLScript
==========

Program in YAML


## Abstract

YAMLScript is programming language with a stylized YAML syntax.

YAMLScript can be used for:

* Writing entire programs (apps)
* Writing reusable libraries
* Embedding in YAML data files


## Synopsis

A complete YAMLScript program `99-bottles.ys`:

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


## YAMLScript Example Programs

The YAMLScript source repository contains [example YAMLScript programs](
https://github.com/yaml/yamlscript/tree/main/perl/eg).

These programs are also available on RosettaCode.org [here](
https://rosettacode.org/wiki/Category:YAMLScript).


## YAMLScript Documentation

* [Test::More::YAMLScript](https://metacpan.org/pod/Test::More::YAMLScript)


## Authors

* Ingy döt Net <ingy@ingy.net>


## Copyright and License

Copyright 2022-2023 by Ingy döt Net

This is free software, licensed under:

The MIT (X11) License
