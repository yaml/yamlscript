## Nim Usage

Use `yamlscript` as a drop-in replacement for your current YAML
loader:

File `main.nim`:

```nim
import std/json

import yamlscript

let ys = newYAMLScript()
let data = ys.load(readFile("config.yaml"))
echo data.pretty
ys.close()
```


## Installation

Install the `yamlscript` Nimble package and the `libys.so` shared
library:

```bash
nimble install yamlscript
curl -sSL https://yamlscript.org/install | LIB=1 bash
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* Nim 2.0 or higher
* Linux, macOS or Windows
