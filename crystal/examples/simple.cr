require "../src/yamlscript"

# Simple YAMLScript example
ys_simple = "!ys-0\ninc: 41"

# Try loading the YAMLScript
begin
  result1 = YAMLScript.load(ys_simple)
  puts "Simple YAMLScript result: #{result1}"
rescue ex : YAMLScript::Error
  puts "Error loading YAMLScript: #{ex.message}"
end

# Simple YAML example
yaml_simple = "foo: bar"

# Try loading plain YAML
begin
  result2 = YAMLScript.load(yaml_simple)
  puts "Plain YAML result:"
  puts "  foo: #{result2["foo"]}"
rescue ex : YAMLScript::Error
  puts "Error loading YAML: #{ex.message}"
end
