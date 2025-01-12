---
title: YAML, Python and the Holy Graal
date: '2023-12-21'
tags: [blog, advent-2023]
permalink: '{{ page.filePathStem }}/'
author:
  name: Ingy döt Net
  url: /about/#ingydotnet
---

Which has a greater airspeed velocity... an unladen swallow or Santa's sleigh?

Well, that depends... are we talking about an African or European swallow?

&nbsp;
<details><summary><strong style="color:red">Huh?</strong></summary>
&nbsp;
<iframe width="560" height="315"
  src="https://www.youtube.com/embed/uio1J2PKzLI?si=QA1x920QfN1GlkRs"
  title="YouTube video player"
  frameborder="0"
  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
  allowfullscreen></iframe>
</details>

### Welcome to Day 21 of the YAMLScript Advent Blog!

So far we've been using the YAMLScript CLI `ys` to run (or load) our YAMLScript
programs.
YAML users are used to using a YAML framework module inside their programs.
For example, in Python you might do:

```python
import yaml

yaml_text = """
- 40
- 50
- 60
"""

data = yaml.safe_load(yaml_text)

print(data)
# => [40, 50, 60]
```

Wouldn't it be nice if we could do the same thing in YAMLScript?
As of today, we can!

```python
import yamlscript

yaml_text = """
- 40
- 50
- 60
"""

data = yamlscript.YAMLScript().load(yaml_text)

print(data)
# => [40, 50, 60]
```

The only thing that changed was the name of the module.

But this module has super powers.

```python
import yamlscript

yaml_text = """
!yamlscript/v0
mapv \(% * 10): 4..6
"""

data = yamlscript.YAMLScript().load(yaml_text)

print(data)
# => [40, 50, 60]
```

We can use YAMLScript functions in our YAML text to generate or manipulate data.

That example was a bit contrived, but I just wanted to show how easy it is to
load plain old YAML or super powered YAML with the new `yamlscript` Python
module.

Here's an example that might be more exciting.

Say we have this normal YAML file with some data in it:

```yaml
# db.yaml
cars:
- make: Ford
  model: Mustang
  year: 1967
  color: red
- make: Dodge
  model: Charger
  year: 1969
  color: orange
- make: Chevrolet
  model: Camaro
  year: 1969
  color: blue
```

We could have another YAML file that uses YAMLScript:

```yaml
# racers.yaml
!yamlscript/v0

db =: load("db.yaml")

=>: !
- name: Ingy döt Net
  car:: db.cars.0
- name: Santa Claus
  car:: db.cars.1
- name: Sir Lancelot
  car:: db.cars.2
```

Then we could load the data into Python and print it out:

```python
# race-report.py
import yaml, yamlscript

data = yamlscript.load('racers.yaml')

print(yaml.dump(data))
```

And we get:

```yaml
- car:
    color: red
    make: Ford
    model: Mustang
    year: 1967
  name: Ingy dot Net
- car:
    color: orange
    make: Dodge
    model: Charger
    year: 1969
  name: Santa Claus
- car:
    color: blue
    make: Chevrolet
    model: Camaro
    year: 1969
  name: Sir Lancelot
```

Pretty cool, huh?

There's no end to the things you can do with this.

Today we're showing off the [Python YAMLScript module](
https://pypi.org/project/yamlscript/) but soon this module will be available in
every language that has a need for it.


### Installing the `yamlscript` Python Module

Normally to install a Python module you would do:

```sh
pip install a-python-module
```

With YAMLScript, you do the same:

```sh
pip install yamlscript
```

But you also need to install the `libyamlscript` shared library.

You can do that with:

```sh
curl https://yamlscript.org/install | bash
```

That will install the latest version of `libyamlscript` for your platform in
`/usr/local/lib`.
You'll need to have root access to do this.

If you want to install it somewhere else, you can set the `PREFIX` option:

```sh
curl https://yamlscript.org/install | PREFIX=~/ys bash
```

But then you'll need to set the `LD_LIBRARY_PATH` environment variable to point
to it:

```sh
export LD_LIBRARY_PATH=~/ys/lib
```

Eventually we may package the yamlscript.py module with wheels (binary assets)
for libyamlscript, but for now you'll need to install it yourself.

If you're a polyglot like me, at least you only have to install it once. :- )


### The Holy Graal

This magic is all possible because of the [GraalVM](https://www.graalvm.org/)
project.
Not only does GraalVM's `native-image` tool compile to binary executables, it
also can compile to shared libraries.

YAMLScript generates and publishes the `libyamlscript` shared library and then
offers binding modules for it in many languages.

----

I hope you are starting to see the power of YAMLScript.
Not only as a new programming language, but also as a new way to work with YAML
files that you already have.

Join me tomorrow for Day 22 of the YAMLScript Advent Blog!
