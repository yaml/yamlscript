## PowerShell Usage

Use `Invoke-YAMLScript` to load YAML or YAMLScript text through `libys`.

```powershell
Import-Module YAMLScript

$data = Invoke-YAMLScript "!ys-0:`ntest:: inc(41)"
```


## Installation

Install the package and the `libys` shared library:

```bash
curl -sSL https://yamlscript.org/install | LIB=1 bash
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```
