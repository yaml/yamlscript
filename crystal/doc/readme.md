## Crystal Usage

Use `yamlscript` as a drop-in replacement for your current YAML loader:

```crystal
# program.cr
require "yamlscript"

# Load from file
input = File.read("config.yaml")
config = YAMLScript.load(input)

puts config.inspect
```


## Installation

Install YAMLScript for Crystal and the `libys.so` shared library:

```bash
# Add to your shard.yml:
dependencies:
  yamlscript:
    github: yaml/yamlscript-crystal

# Install dependencies
shards install

# Install shared library
curl -sSL https://yamlscript.org/install | bash
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* Crystal 1.0 or higher