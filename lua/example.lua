#!/usr/bin/env lua

-- Example Lua script demonstrating YAMLScript binding usage
-- Run with: lua example.lua

local yamlscript = require("yamlscript")

-- Create a YAMLScript instance
local ys = yamlscript.new()

-- Example 1: Simple YAML parsing
print("=== Example 1: Simple YAML ===")
local data1 = ys:load([[
name: World
numbers: [1, 2, 3]
nested:
  key: value
]])
print("Name:", data1.name)
print("Numbers:", table.concat(data1.numbers, ", "))
print("Nested key:", data1.nested.key)
print()

-- Example 2: YAMLScript functionality
print("=== Example 2: YAMLScript Functions ===")
local data2 = ys:load([[
!YS-v0:
result:: inc(41)
list:: &list + [1 2 3 4 5]
sum:: a(*list).reduce(add)
]])
print("inc(41) =", data2.result)
print("sum([1,2,3,4,5]) =", data2.sum)
print()

-- Example 3: String interpolation
print("=== Example 3: String Interpolation ===")
local data3 = ys:load([[
!YS-v0:
name =: "Lua"
greeting:: "Hello, $name!"
calculation:: "2 + 2 = $(+ 2 2)"
]])
print(data3.greeting)
print(data3.calculation)
print()

-- Example 4: File loading
print("=== Example 4: File Loading ===")
-- Create a temporary file for demonstration
local temp_file = io.open("temp_data.yaml", "w")
temp_file:write([[
title: Sample Data
items:
  - apple
  - banana
  - cherry
]])
temp_file:close()

local data4 = ys:load([[
!YS-v0:
loaded-data =: load("temp_data.yaml")

loaded_data:: loaded-data
item_count:: loaded-data.items:count
]])
print("Loaded title:", data4.loaded_data.title)
print("Item count:", data4.item_count)

-- Clean up
os.remove("temp_data.yaml")
print()

print("All examples completed successfully!")
