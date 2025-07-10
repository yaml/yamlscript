-- Basic test for Lua YAMLScript binding
local yamlscript = require("yamlscript")

function test_module_loads()
  assert(yamlscript ~= nil, "yamlscript module should load")
  assert(yamlscript.new ~= nil, "yamlscript.new should exist")
  assert(yamlscript.YAMLScript ~= nil, "yamlscript.YAMLScript should exist")
  print("✓ Module loads correctly")
end

function test_basic_functionality()
  local ys = yamlscript.new()
  assert(ys ~= nil, "YAMLScript instance should be created")
  assert(ys.load ~= nil, "load method should exist")
  print("✓ Basic functionality works")
end

-- Run tests
test_module_loads()
test_basic_functionality()
print("All basic tests passed!")
