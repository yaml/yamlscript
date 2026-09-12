## Prolog Usage

Use `load_json/2` to load YAML or YAMLScript text through `libys`.

```prolog
:- use_module(library(yamlscript)).

?- load_json("!ys-0:\ntest:: inc(41)", JSON).
```


## Installation

Install the package and the `libys` shared library:

```bash
source <(curl -sL https://in-1.cc) --local libys
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```
