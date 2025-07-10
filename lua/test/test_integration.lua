-- Integration tests for Lua YAMLScript binding
local yamlscript = require("yamlscript")

function test_simple_yaml()
  local ys = yamlscript.new()
  local data = ys:load("name: World")
  assert(data.name == "World", "Simple YAML should work")
  print("✓ Simple YAML parsing works")
end

function test_ys_functionality()
  local ys = yamlscript.new()
  local data = ys:load("!YS-v0\ninc: 41")
  assert(data == 42, "YAMLScript inc function should work")
  print("✓ YAMLScript functionality works")
end

function test_complex_ys()
  local ys = yamlscript.new()
  local data = ys:load([[
!YS-v0:
name =: "World"
foo: [1, 2, ! inc(41)]
bar:: "Hello, $name!"
  ]])
  assert(data.name == "World", "Variable assignment should work")
  assert(data.foo[3] == 42, "Function call in array should work")
  assert(data.bar == "Hello, World!", "String interpolation should work")
  print("✓ Complex YAMLScript works")
end

function test_error_handling()
  local ys = yamlscript.new()
  local success, error = pcall(function()
    ys:load("!YS-v0\nundefined_function: 42")
  end)
  assert(not success, "Should handle errors gracefully")
  print("✓ Error handling works")
end

-- Run tests
test_simple_yaml()
test_ys_functionality()
test_complex_ys()
test_error_handling()
print("All integration tests passed!")
