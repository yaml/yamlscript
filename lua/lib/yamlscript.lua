-- Copyright 2023-2025 Ingy dot Net
-- This code is licensed under MIT license (See License for details)

--[[
Lua binding/API for the libys shared library.

This module provides a Lua interface to the YAMLScript shared library.
It uses FFI to call the libys.so shared library functions.

The current user facing API consists of a single class, `YAMLScript`, which
has a single method: `:load(string)`.
The load() method takes a YAMLScript string as input and returns the Lua
table that the YAMLScript code evaluates to.
--]]

-- Try to load FFI - prefer cffi-lua for standard Lua, fallback to LuaJIT FFI
local ffi
local ffi_loaded = false

-- First try cffi-lua (for standard Lua)
local success, cffi = pcall(require, "cffi")
if success then
  ffi = cffi
  ffi_loaded = true
else
  -- Fallback to LuaJIT FFI if available
  success, cffi = pcall(require, "jit.ffi")
  if success then
    ffi = cffi
    ffi_loaded = true
  end
end

if not ffi_loaded then
  error([[
FFI library not found. This binding requires either:
1. cffi-lua library (for standard Lua): luarocks install cffi-lua
2. LuaJIT with FFI support
]])
end

local json = require("cjson")

-- This value is automatically updated by 'make bump'.
-- The version number is used to find the correct shared library file.
-- We currently only support binding to an exact version of libys.
local yamlscript_version = '0.2.4'

-- Find the libys shared library file path
local function find_libys_path()
  -- We currently only support platforms that GraalVM supports.
  -- And Windows is not yet implemented...
  -- Confirm platform and determine file extension:
  local so
  if ffi.os == "Linux" then
    so = "so"
  elseif ffi.os == "OSX" then
    so = "dylib"
  else
    error("Unsupported platform '" .. ffi.os .. "' for yamlscript.")
  end

  -- We currently bind to an exact version of libys.
  local libys_name = string.format("libys.%s.%s", so, yamlscript_version)

  -- Use LD_LIBRARY_PATH to find libys shared library, or default to
  -- '/usr/local/lib' (where it is installed by default):
  local ld_library_path = os.getenv('LD_LIBRARY_PATH')
  local ld_library_paths = {}
  if ld_library_path then
    for path in ld_library_path:gmatch("[^:]+") do
      table.insert(ld_library_paths, path)
    end
  end
  table.insert(ld_library_paths, '/usr/local/lib')
  table.insert(ld_library_paths, os.getenv('HOME') .. '/.local/lib')

  local libys_path = nil
  for _, path in ipairs(ld_library_paths) do
    local full_path = path .. '/' .. libys_name
    local file = io.open(full_path, "r")
    if file then
      file:close()
      libys_path = full_path
      break
    end
  end

  if not libys_path then
    error(string.format([[
Shared library file '%s' not found
Try: curl https://yamlscript.org/install | VERSION=%s LIB=1 bash
See: https://github.com/yaml/yamlscript/wiki/Installing-YAMLScript
]], libys_name, yamlscript_version))
  end

  return libys_path
end

-- Define FFI C function signatures
ffi.cdef[[
typedef struct graal_isolate_t graal_isolate_t;
typedef struct graal_isolatethread_t graal_isolatethread_t;

int graal_create_isolate(void* params, graal_isolate_t** isolate,
                        graal_isolatethread_t** thread);
int graal_tear_down_isolate(graal_isolatethread_t* thread);

const char* load_ys_to_json(graal_isolatethread_t* thread, const char* s);
]]

-- Load libys shared library
local libys = ffi.load(find_libys_path())

-- The YAMLScript class is the main user facing API for this module
local YAMLScript = {}
YAMLScript.__index = YAMLScript

--[[
Interface with the libys shared library.

Usage:
  local yamlscript = require("yamlscript")
  local ys = yamlscript.new()
  local data = ys:load(io.open('file.ys'):read('*a'))
--]]

-- YAMLScript instance constructor
function YAMLScript.new(config)
  config = config or {}

  local self = setmetatable({}, YAMLScript)

  -- Create a new GraalVM isolate
  local isolate = ffi.new("graal_isolate_t*[1]")
  local thread = ffi.new("graal_isolatethread_t*[1]")

  local rc = libys.graal_create_isolate(nil, isolate, thread)

  if rc ~= 0 then
    error("Failed to create isolate")
  end

  self.isolatethread = thread[0]
  self.error = nil

  return self
end

-- Compile and eval a YAMLScript string and return the result
function YAMLScript:load(input)
  -- Reset any previous error
  self.error = nil

  -- Call 'load_ys_to_json' function in libys shared library
  local data_json = ffi.string(libys.load_ys_to_json(
    self.isolatethread,
    input
  ))

  -- Decode the JSON response
  local resp = json.decode(data_json)

  -- Check for libys error in JSON response
  self.error = resp.error
  if self.error then
    error(self.error.cause)
  end

  -- Get the response object from evaluating the YAMLScript string
  if not resp.data then
    error("Unexpected response from 'libys'")
  end

  -- Return the response object
  return resp.data
end

-- Manual cleanup method for the isolate
function YAMLScript:close()
  if self.isolatethread then
    -- Tear down the isolate thread to free resources
    local rc = libys.graal_tear_down_isolate(self.isolatethread)
    if rc ~= 0 then
      error("Failed to tear down isolate")
    end
    self.isolatethread = nil
  end
end

-- Module exports
local M = {}
M.YAMLScript = YAMLScript
M.new = YAMLScript.new

return M
