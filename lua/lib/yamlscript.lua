-- YAMLScript.lua

local ffi = require("cffi")

-- Define the YAMLScript version
local yamlscript_version = "0.1.68"

-- Define the libyamlscript shared library name
local libyamlscript_name = "libyamlscript.dylib." .. yamlscript_version

-- Load the libyamlscript shared library
local libyamlscript = ffi.load(libyamlscript_name)

-- Define the load_ys_to_json function
local load_ys_to_json = libyamlscript.load_ys_to_json
load_ys_to_json = ffi.cast("char *(void *, char *)", load_ys_to_json)

-- Define the graal_create_isolate function
local graal_create_isolate = libyamlscript.graal_create_isolate
graal_create_isolate = ffi.cast("int (void *, void *, void **)", graal_create_isolate)

-- Define the graal_tear_down_isolate function
local graal_tear_down_isolate = libyamlscript.graal_tear_down_isolate
graal_tear_down_isolate = ffi.cast("int (void *)", graal_tear_down_isolate)

-- YAMLScript class
local YAMLScript = {}
YAMLScript.__index = YAMLScript

function YAMLScript.new(config)
  local self = setmetatable({}, YAMLScript)
  self.isolatethread = ffi.new("void *[1]")
  local rc = graal_create_isolate(nil, nil, self.isolatethread)
  if rc ~= 0 then error("Failed to create isolate") end
  return self
end

function YAMLScript:load(input)
  local data_json = load_ys_to_json(self.isolatethread[0], input)
  local resp = json.decode(data_json)
  if resp.error then error(resp.error.cause) end
  return resp.data
end

function YAMLScript:__gc()
  local rc = graal_tear_down_isolate(self.isolatethread[0])
  if rc ~= 0 then error("Failed to tear down isolate") end
end

return YAMLScript
