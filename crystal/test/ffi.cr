require "../src/yamlscript"

# Test basic YAMLScript loading - returns a single value
result = YAMLScript.load("!YS-v0\ninc: 41")
if result.as_i != 42
  puts "Error: Expected 42, got #{result.inspect}"
  exit(1)
end
puts "Test 1 passed: Got 42 from inc: 41"

# Test plain YAML loading - returns a hash
result = YAMLScript.load("foo: bar")
if !result.as_h.has_key?("foo") || result.as_h["foo"].as_s != "bar"
  puts "Error: Expected {\"foo\" => \"bar\"}, got #{result.inspect}"
  exit(1)
end
puts "Test 2 passed: Got {\"foo\" => \"bar\"} from plain YAML"

puts "Crystal binding FFI test PASSED!"
