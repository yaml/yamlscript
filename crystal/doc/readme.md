## Crystal Usage

Here's a simple example of using the YAMLScript Crystal binding:

```crystal
require "yamlscript"

# YAMLScript code
ys_code = <<-YS
!YS-v0
inc: 41
YS

# Load and evaluate the YAMLScript
result = YAMLScript.load(ys_code)
puts "Result: #{result}"  # Output: Result: 42

# Regular YAML also works
yaml_code = "foo: bar"
result = YAMLScript.load(yaml_code)
puts "YAML result: #{result["foo"]}"  # Output: YAML result: bar
```

## Development

To build and run tests:

```bash
# Clone the repository
git clone https://github.com/yaml/yamlscript.git
cd yamlscript/crystal

# Build the binding and copy required libraries
make build

# Run the tests
make test

# Run the example
make run-example
```
