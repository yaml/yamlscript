YAMLScript
==========

Add Logic to Your YAML Files


## Synopsis

Load `file.yaml` with YAMLScript:
```yaml
!yamlscript/v0/

=>:
  names =: curl(
    "https://raw.githubusercontent.com/dominictarr/" +
    "random-name/master/first-names.json")
    .json/load()

name:: names.rand-nth()
aka:: names.rand-nth()
age:: 6 * 7
color: yellow
```

and get:
```json
{
  "name": "Anthea",
  "aka": "Patrizia",
  "age": 42,
  "color": "yellow"
}
```


## Description

[YAMLScript](https://yamlscript.org) is a functional programming language with a
clean YAML syntax.

YAMLScript can be used for enhancing ordinary [YAML](https://yaml.org) files
with functional operations, such as:

* Import parts of other YAML files to any node
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
CLI binary, `ys`, to run:

```text
$ ys --compile file.ys
(def names
  (_-> (curl (+_ "https://raw.githubusercontent.com/dominictarr/"
                 "random-name/master/first-names.json"))
       (list json/load)))
{"age" (*_ 6 7),
 "aka" (_-> names (list rand-nth)),
 "color" "yellow",
 "name" (_-> names (list rand-nth))}
```

```markys:include
!yamlscript/v0/
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
!yamlscript/v0/
file:: "$(ENV.ROOT)/$(ENV.YSLANG)/doc/authors.md"
```


## License & Copyright

Copyright 2022-2024 Ingy döt Net <ingy@ingy.net>

This project is licensed under the terms of the `MIT` license.
See [LICENSE](https://github.com/yaml/yamlscript/blob/main/License) for
more details.
