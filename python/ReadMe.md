<!-- DO NOT EDIT — THIS FILE WAS GENERATED -->

YAMLScript
==========

Add Logic to Your YAML Files


## Synopsis

Load `file.yaml` with YAMLScript:
```yaml
!yamlscript/v0/

=>:
  names-url =:
    ("https://raw.githubusercontent.com/dominictarr/" +
     "random-name/master/first-names.json")

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
(def names-url
  (+_ "https://raw.githubusercontent.com/dominictarr/"
      "random-name/master/first-names.json"))
(def name-list (_& 'first-names (json/load (curl names-url))))
{"age" (_& 'num (*_ 2 3 7)),
 "aka" (_-> name-list (list rand-nth)),
 "color" (_& 'hue (_-> (qw red green blue yellow) (list shuffle) (list first))),
 "name" (rand-nth (_** 'first-names)),
 "title" (str (_** 'num) " shades of " (_** 'hue) ".")}
```


## Python Usage

File `prog.py`:

```python
from yamlscript import YAMLScript
ys = YAMLScript()
input = open('file.ys').read()
data = ys.load(input)
print(data)
```

File `file.ys`:

```yaml
!yamlscript/v0

name =: "World"

=>::
  foo: [1, 2, ! inc(41)]
  bar:: load("other.yaml")
  baz:: "Hello, $name!"
```

File `other.yaml`:

```yaml
oh: Hello
```

Run:

```text
$ python prog.py
{'foo': [1, 2, 42], 'bar': {'oh': 'Hello'}, 'baz': 'Hello, World!'}
```


## Installation

You can install this module like any other Python module:

```bash
$ pip install yamlscript
```

but you will need to have a system install of `libyamlscript.so`.

One simple way to do that is with:

```bash
$ curl https://yamlscript.org/install | bash
```

> Note: The above command will install the latest version of the YAMLScript
command line utility, `ys`, and the shared library, `libyamlscript.so`, into
`~/local/bin` and `~/.local/lib` respectively.

See https://github.com/yaml/yamlscript?#installing-yamlscript for more info.


## See Also

* [YAMLScript Web Site](https://yamlscript.org)
* [YAMLScript Blog](https://yamlscript.org/blog)
* [YAMLScript Source Code](https://github.com/yaml/yamlscript)
* [YAMLScript Samples](https://github.com/yaml/yamlscript/tree/main/sample)
* [YAMLScript Programs](https://rosettacode.org/wiki/Category:YAMLScript)
* [YAML](https://yaml.org)
* [Clojure](https://clojure.org)


## Authors


* [Ingy döt Net](https://github.com/ingydotnet)


## License & Copyright

Copyright 2022-2024 Ingy döt Net <ingy@ingy.net>

This project is licensed under the terms of the `MIT` license.
See [LICENSE](https://github.com/yaml/yamlscript/blob/main/License) for
more details.
