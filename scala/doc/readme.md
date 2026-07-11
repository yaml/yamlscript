## Scala Usage

Use `YAMLScript.load` to load YAML or YAMLScript text through `libys`.

```scala
import org.yamlscript.YAMLScript

val data = YAMLScript.load("!ys-0:\ntest:: inc(41)")
```


## Installation

Install the package and the `libys` shared library:

```bash
curl -sSL https://yamlscript.org/install | LIB=1 bash
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```
