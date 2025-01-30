YAMLScript
==========

Add Logic to Your YAML Files


## Synopsis

Load `file.yaml` with YAMLScript:
```yaml
!YS v0:

# Get data from external sources:
names-url =:
  "https://raw.githubusercontent.com/dominictarr/\
   random-name/master/first-names.json"

name-list =: &first-names json/load(curl(names-url))

# Data object with literal keys and generated values:
name:: rand-nth(*first-names)
aka:: name-list.rand-nth()
age:: &num 2 * 3 * 7
color:: &hue qw(red green blue yellow)
          .shuffle()
          .first()
title:: "$(*num) shades of $(*hue)."
```

and get:
```json
{
  "name": "Dolores",
  "aka": "Anita",
  "age": 42,
  "color": "green",
  "title": "42 shades of green."
}
```


## Description

[YAMLScript](https://yamlscript.org) is a functional programming language with a
clean YAML syntax.

YAMLScript can be used for enhancing ordinary [YAML](https://yaml.org) files
with functional operations, such as:

* Import (parts of) other YAML files to any node
* String interpolation including function calls
* Data transforms including ones defined by you

This YAMLScript library should be a drop-in replacement for your current YAML
loader!

Most existing YAML files are already valid YAMLScript files.
This means that YAMLScript works as a normal YAML loader, but can also evaluate
functional expressions if asked to.

Under the hood, YAMLScript code compiles to the Clojure programming language.
This makes YAMLScript a complete functional programming language right out of
the box.

Even though YAMLScript compiles to Clojure, and Clojure compiles to Java, there
is no dependency on Java or the JVM.
YAMLScript is compiled to a native shared library (`libyamlscript.so`) that can
be used by any programming language that can load shared libraries.

To see the Clojure code that YAMLScript compiles to, you can use the YAMLScript
CLI binary `ys` to run:

```text
$ ys --compile file.ys
(let
 [names-url "https://raw.githubusercontent.com/dominictarr/random-name/master/first-names.json"
  name-list (_& 'first-names (json/load (curl names-url)))]
 (%
  "name" (rand-nth (_** 'first-names))
  "aka" (rand-nth name-list)
  "age" (_& 'num (mul+ 2 3 7))
  "color" (_& 'hue (first (shuffle (qw red green blue yellow))))
  "title" (str (_** 'num) " shades of " (_** 'hue) ".")))
```

```markys:include
!YS v0:
file:: "$(ENV.ROOT)/$(ENV.YSLANG)/doc/readme.md"
```


## See Also

* [YAMLScript Web Site](https://yamlscript.org)
* [YAMLScript Blog](https://yamlscript.org/blog)
* [YAMLScript Source Code](https://github.com/yaml/yamlscript)
* [YAMLScript Samples](https://github.com/yaml/yamlscript/tree/main/sample)
* [YAMLScript Programs](https://rosettacode.org/wiki/Category:YAMLScript)
* [YAML](https://yaml.org)
* [Clojure](https://clojure.org)


## Authors

```markys:include
!YS v0:
file:: "$(ENV.ROOT)/$(ENV.YSLANG)/doc/authors.md"
```


## License & Copyright

Copyright 2022-2025 Ingy döt Net <ingy@ingy.net>

This project is licensed under the terms of the `MIT` license.
See [LICENSE](https://github.com/yaml/yamlscript/blob/main/License) for
more details.
