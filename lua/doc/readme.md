## Lua Usage

Use `yamlscript` as a drop-in replacement for your current YAML loader:

```lua
-- program.lua
local yamlscript = require("yamlscript")

local ys = yamlscript.new()

-- Load from file
local file = io.open("config.yaml", "r")
local input = file:read("*a")
file:close()

local config = ys:load(input)
print(config)
```


## Installation

Install YAMLScript for Lua and the `libys.so` shared library:

```bash
# Install dependencies
luarocks install cffi-lua
luarocks install lua-cjson

# Install shared library
curl -sSL https://yamlscript.org/install | bash

# Add to LUA_PATH
export LUA_PATH="$(pwd)/lib/?.lua;;"
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* Lua 5.1 or higher
* cffi-lua library
* lua-cjson library