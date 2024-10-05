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
