rockspec_format = "3.0"
package = "yamlscript"
version = "%VERSION%-%REVISION%"
source = {
   url = "https://github.com/yaml/yamlscript-lua/archive/refs/tags/v%VERSION%.tar.gz"
}
description = {
   summary = "Lua binding for YAMLScript",
   detailed = [[
      YAMLScript is a functional programming language with a clean YAML syntax.
      This Lua binding provides a native interface to the YAMLScript shared library.
   ]],
   license = "MIT",
   homepage = "https://yamlscript.org",
   maintainer = "Ingy döt Net <ingy@ingy.net>",
   labels = { "yaml", "yamlscript", "ffi", "binding" }
}
dependencies = {
   "lua >= 5.1",
   "cffi-lua >= 0.2.0",
   "lua-cjson >= 2.1.0"
}
build = {
   type = "builtin",
   modules = {
      yamlscript = "lib/yamlscript.lua"
   },
   copy_directories = { "test" },
   external_dependencies = {
      libys = "== %VERSION"
   }
}
