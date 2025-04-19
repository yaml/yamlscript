---
title: YS Binding Libraries
talk: 0
---

Your existing YAML files are perfectly valid YS files!
Using a YS binding library (aka module or package) these files can be loaded
into native objects just like a YAML loader would do.

Without a `!YS-v0` tag at the top they will load the same as normal.
With that tag, they can be made to take advantage of any of the vast YS
functional capabilities.

YS intends to provide a YS capable YAML loader library (module/package) for
every programming language that uses YAML.
These libraries are meant to be full replacements for the existing YAML loaders
in that language.

!!! note

    YS loaders only return data values that adhere to the **JSON** data model.
    While that model is a subset of what can be represented in YAML 1.2, it is
    what most users of YAML expect.
    In other words, YS is targeted at YAML's most popular use cases.


## Advantages of using YS

YS YAML loaders have major advantages over the existing YAML loaders:

* Same API and capabilities regardless of the programming language.
* New features and bux fixes released to all languages at the same time.
* Highly configurable. Limit YAML and YS capabilities to your exact needs.
* Created by a [YAML Specification](https://yaml.org/spec/1.2.2/)
  [creator & maintainer](../ingydotnet.md).


## Currently Available Libraries

Currently there are working libraries for:

* [Clojure](https://clojars.org/org.yamlscript/clj-yamlscript)
* [Crystal](https://github.com/yaml/yamlscript-crystal)
* [Go](https://github.com/yaml/yamlscript-go)
* [Java](https://clojars.org/org.yamlscript/yamlscript)
* [Julia](https://juliahub.com/ui/Packages/General/YAMLScript)
* [NodeJS](https://www.npmjs.com/package/@yaml/yamlscript)
* [Perl](https://metacpan.org/dist/YAMLScript/view/lib/YAMLScript.pod)
* [Python](https://pypi.org/project/yamlscript/)
* [Raku](https://raku.land/zef:ingy/YAMLScript)
* [Ruby](https://rubygems.org/search?query=yamlscript)
* [Rust](https://crates.io/crates/yamlscript)

Install these libraries like you would any other library for your language.
You must also install the matching version of the `libyamlscript.so` shared
library.
See [Installing YS](install.md) for more info.

You can use these libraries like any other YAML loader.
Here's an example usage in Python:

File `program.py`:

```python
from yamlscript import YAMLScript
ys = YAMLScript()
input = open('file.ys').read()
data = ys.load(input)
print(data)
```

File `file.ys`:

```yaml
!YS-v0:

::  # Set values to use in data
  name =: "World"
  other =: load("other.yaml")

foo:: -[(6 * 7), inc(41), 43.--, (3 .. 9):sum]
bar:: other.stuff:shuffle.take(3)
baz:: "Hello, $name!"
```

File `other.yaml`:

```yaml
stuff:
- ark
- banana
- cat
- doll
- electron
- flan
- golf ball
```

Run:

```text
$ python prog.py
{'foo': [42, 42, 42, 42], 'bar': ['cat', 'flan', 'doll'], 'baz': 'Hello, World!'}
```
