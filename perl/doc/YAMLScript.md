YAMLScript
==========

Program in YAML


# Synopsis

A YAML file:

```yaml
# food.yaml
fruit:
- apple
- banana
nuts:
- cashew
- drupe
```

A YAMLScript file:

```yaml
# data.ys
!yamlscript/v0/data

foods:: load("food.yaml")
numbers:: 6..9
```

Perl code:

```perl
# script.pl
use strict; use warnings;
use YAMLScript;
use IO::All;
use Data::Dumper qw(Dumper);
$Data::Dumper::Indent = 1;
$Data::Dumper::Terse = 1;

my $ys = io('data.ys')->all;
my $data = YAMLScript->load($ys);

print Dumper($data);
```

Run it:

```text
$ perl script.pl
{
  'foods' => {
    'fruit' => [
      'apple',
      'banana'
    ],
    'nuts' => [
      'cashew',
      'drupe'
    ]
  },
  'numbers' => [
    6,
    7,
    8,
    9
  ]
}
```


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
