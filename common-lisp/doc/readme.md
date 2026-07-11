## Common Lisp Usage

Use `yamlscript:load-json` to load YAML or YAMLScript text through `libys`.

```lisp
(yamlscript:load-json "!ys-0:
test:: inc(41)")
```


## Installation

Install the package and the `libys` shared library:

```bash
curl -sSL https://yamlscript.org/install | LIB=1 bash
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```
