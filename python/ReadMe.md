YAMLScript
==========

Program in YAML


## Synopsis

```yaml
#!/usr/bin/env ys-0

defn main(name='world'):
  say: "Hello, $name!"
```


## Description

YAMLScript is a functional programming language with a stylized YAML syntax.

YAMLScript can be used for:

* Writing new programs and applications
  * Run with `ys file.ys`
  * Or compile to binary executable with `ys -C file.ys`
* Enhancing ordinary YAML files with new functional magics
  * Import parts of other YAML files to any node
  * String interpolation including function calls
  * Any other functionality you can dream up!
* Writing reusable shared libraries
  * High level code instead of C
  * Bindable to almost any programming language

YAMLScript should be a drop-in replacement for your YAML loader!

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
command line utility, `ys`, to run:

```text
$ ys --compile file.ys
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
$ curl -sSL yamlscript.org/install | bash
```

> Note: The above command will install the latest version of the YAMLScript
command line utility, `ys`, and the shared library, `libyamlscript.so`, into
`~/local/bin` and `~/.local/lib` respectively.

See https://github.com/yaml/yamlscript?#installing-yamlscript for more info.


## See Also

* [The YAMLScript Web Site](https://yamlscript.org)
* [The YAMLScript Blog](https://yamlscript.org/blog)
* [The YAMLScript Source Code](https://github.com/yaml/yamlscript)
* [YAML](https://yaml.org)
* [Clojure](https://clojure.org)


## Authors

* [Ingy döt Net](https://github.com/ingydotnet)


## License & Copyright

Copyright 2022-2024 Ingy döt Net <ingy@ingy.net>

This project is licensed under the terms of the `MIT` license.
See [LICENSE](https://github.com/yaml/yamlscript/blob/main/License) for
more details.
