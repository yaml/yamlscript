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
!YS-v0:

name =: "World"

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
