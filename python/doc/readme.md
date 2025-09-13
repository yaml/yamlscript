## Python Usage

Use `yamlscript.py` as a drop-in replacement for your current YAML loader:

```python
# program.py
from yamlscript import YAMLScript
import json

ys = YAMLScript()

# Load from file
input = open('config.yaml').read()
config = ys.load(input)

# Convert to JSON
print(json.dumps(config, indent=2))
```


## Installation

Install YAMLScript for Python and the `libys.so` shared library:

```bash
pip install yamlscript
curl -sSL https://yamlscript.org/install | bash
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* Python 3.8 or higher
