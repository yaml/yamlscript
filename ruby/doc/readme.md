## Ruby Usage

Use `yamlscript` as a drop-in replacement for your current YAML loader:

```ruby
# program.rb
require 'yamlscript'
require 'json'

ys = YAMLScript.new

# Load from file
input = File.read('config.yaml')
config = ys.load(input)

# Convert to JSON
puts JSON.pretty_generate(config)
```


## Installation

Install YAMLScript for Ruby and the `libys.so` shared library:

```bash
gem install yamlscript
curl -sSL https://yamlscript.org/install | bash
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* Ruby 2.7 or higher