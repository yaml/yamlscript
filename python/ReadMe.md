yamlscript
==========

Program in YAML


## Synopsis

```
#!/usr/bin/env ys-0

defn main(name):
  say: "Hello, $name!"
```


## Description

YAMLScript is a functional programming language with a stylized YAML syntax.

YAMLScript can be used for:

* Writing new programs and applications
  * Run with `ys file.ys`
  * Or compile to binary with `ys -C file.ys`
* Writing reusable shared libraries
  * Bindable to almost any programming language
* Using as a YAML loader module in many programming languages
  * Plain / existing YAML files
  * YAML files with new functional magics


## Installing `yamlscript.py`

You can install this module from https://pypi.org like any other Python module,
but you will need to have a system install of `libyamlscript.so`.

One simple way to do that is with:

```
curl https://yamlscript.org/install-libyamlscript | sudo bash
```

See: https://github.com/yaml/yamlscript for more info


## API

Us the `yamlscript` module in your Python program like this:

```python
import yamlscript

ys_file = open('file.ys')
ys_code = open('file.ys').read()

# Class method
data = yamlscript.load(ys_file)
data = yamlscript.load(ys_code)

# Instance method
ys = yamlscript.YAMLScript()
data = ys.load(ys_file)
data = ys.load(ys_code)

# Error handling
try:
    yamlscript.load("a: b: c")
except Exception as e:
    print(e)
    print(ys.error['cause'])
    print(ys.error['trace'])
```


## License & Copyright

This project is licensed under the terms of the `MIT` license.
See [LICENSE](https://github.com/yaml/pyyaml-future/blob/main/LICENSE) for
more details.

Copyright 2022-2023 Ingy döt Net <ingy@ingy.net>
