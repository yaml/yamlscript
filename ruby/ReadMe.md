YAMLScript
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


## Installing `yamlscript`

You can install this module from https://rubygems.org like any other Ruby library,
but you will need to have a system install of `libyamlscript.so`.

One simple way to do that is with:

```
curl https://yamlscript.org/install-libyamlscript | sudo bash
```

See: https://github.com/yaml/yamlscript for more info


## API

Use the `yamlscript` library in your Ruby program like this:

```ruby
require 'yamlscript'

ys_code = IO.read('file.ys')

# Class method
data = YAMLScript.load(ys_code)

# Instance method
ys = YAMLScript.new
data = ys.load(ys_code)

# Error handling
begin
  ys.load("a: b: c")
rescue Exception => e:
  puts e
end
```


# Authors

* Ingy döt Net <ingy@ingy.net>
* Delon Newman <contact@delonnewman.name>


# Copyright and License

Copyright 2022-2024 by Ingy döt Net

This is free software, licensed under:

The MIT (X11) License
