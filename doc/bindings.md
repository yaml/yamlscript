---
title: YAMLScript Binding Libraries
---

YAMLScript intends to provide a loader library for every programming language
that uses YAML.

Currently we have working libraries for
[Clojure](https://clojars.org/org.yamlscript/clj-yamlscript),
[Go](https://github.com/yaml/yamlscript-go),
[Java](https://clojars.org/org.yamlscript/yamlscript),
[Julia](https://juliahub.com/ui/Packages/General/YAMLScript),
[NodeJS](https://www.npmjs.com/package/@yaml/yamlscript),
[Perl](https://metacpan.org/dist/YAMLScript/view/lib/YAMLScript.pod),
[Python](https://pypi.org/project/yamlscript/),
[Raku](https://raku.land/zef:ingy/YAMLScript),
[Ruby](https://rubygems.org/search?query=yamlscript) and
[Rust](https://crates.io/crates/yamlscript).

Install these libraries like you would any other library for your language.
You must also install the matching version of the `libyamlscript.so` shared
library.
See [Installing YAMLScript](/doc/install) for more info.

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
!yamlscript/v0/

=>:  # Set values to use in data
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
