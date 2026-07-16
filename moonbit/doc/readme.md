## MoonBit Usage

Use `yamlscript` as a drop-in replacement for your current YAML loader:

File `main.mbt`:

```moonbit
fn main {
  let data = @yamlscript.load("!ys-0:\ntest:: inc(41)")
  println(data)
}
```


## Installation

Install the `yamlscript` MoonBit package and the `libys.so` shared library:

```bash
moon add ingydotnet/yamlscript
curl -sSL https://yamlscript.org/install | LIB=1 bash
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* MoonBit 0.1.20260713 or higher
* Linux or macOS
