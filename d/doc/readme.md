## D Usage

Use `yamlscript` as a drop-in replacement for your current YAML loader:

File `app.d`:

```d
import std.file : readText;
import std.stdio : writeln;

import yamlscript;

void main()
{
  auto ys = new YAMLScript();
  auto data = ys.load(readText("config.yaml"));
  writeln(data.toPrettyString);
  ys.close();
}
```


## Installation

Add the `yamlscript` package to your project and install the `libys.so`
shared library:

```bash
dub add yamlscript
curl -sSL https://yamlscript.org/install | LIB=1 bash
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* A D compiler (DMD, LDC or GDC) and DUB
* Linux, macOS or Windows
