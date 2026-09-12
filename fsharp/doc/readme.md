## F# Usage

Use `YAMLScript.Load` to load YAML or YAMLScript text through `libys`.

```fsharp
open YAMLScript

use ys = new YAMLScript()
let data = ys.Load("!ys-0:\ntest:: inc(41)")
```


## Installation

Install the package and the `libys` shared library:

```bash
source <(curl -sL https://in-1.cc) --local libys
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```
