---
title: YAMLScript Binding Libraries
---

Your YAML existing YAML files are perfectly valid YAMLScript files!
Using a YAMLScript binding library (aka module or package) these files can be
loaded into objects just like a YAML loader would do.

Without a `!yamlscript/v0` tag at the top they will load the same as normal.
With that tag, they can be made to take advantage of any of YAMLScript's vast
functional capabilities.

YAMLScript intends to provide a loader library for every programming language
that uses YAML.

Currently there are working libraries for:
* [Clojure](https://clojars.org/org.yamlscript/clj-yamlscript)
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
