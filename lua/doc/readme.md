## Lua Usage

File `prog.lua`:

```lua
local yamlscript = require("yamlscript")
local ys = yamlscript.new()
local input = io.open('file.ys'):read('*a')
local data = ys:load(input)
print(data)
```

File `file.ys`:

```yaml
!YS-v0:

name =: "World"

foo: [1, 2, ! inc(41)]
bar:: load("other.yaml")
baz:: "Hello, $name!"
```

File `other.yaml`:

```yaml
oh: Hello
```

Run:

```text
$ lua prog.lua
{foo={1,2,42}, bar={oh="Hello"}, baz="Hello, World!"}
```


## Installation

This Lua binding requires:

1. Standard Lua 5.1+ (not LuaJIT)
2. The `cffi-lua` library for FFI capabilities
3. The `cjson` library for JSON parsing
4. A system install of `libys.so`

To install the dependencies:

```bash
# Install Lua (if not already installed)
sudo apt-get install lua5.4

# Install LuaRocks (if not already installed)
sudo apt-get install luarocks

# Install required Lua libraries
luarocks install cffi-lua
luarocks install lua-cjson

# Install libys shared library
curl https://yamlscript.org/install | bash
```

> Note: The above command will install the latest version of the YAMLScript
command line utility, `ys`, and the shared library, `libys.so`, into
`~/local/bin` and `~/.local/lib` respectively.

To use the binding, add the `lib/` directory to your `LUA_PATH`:

```bash
export LUA_PATH="$(pwd)/lib/?.lua;;"
```

See <https://yamlscript.org/doc/install/> for more info.
