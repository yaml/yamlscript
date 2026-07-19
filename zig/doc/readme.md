## Zig Usage

Use `yamlscript` as a drop-in replacement for your current YAML loader:

File `main.zig`:

```zig
const std = @import("std");
const yamlscript = @import("yamlscript");

pub fn main() !void {
    const allocator = std.heap.page_allocator;

    const input = try std.fs.cwd().readFileAlloc(
        allocator, "config.yaml", 1 << 20);
    defer allocator.free(input);

    var ys = try yamlscript.YAMLScript.init(allocator);
    defer ys.deinit();

    var result = try ys.load(input);
    defer result.deinit();

    std.debug.print("{f}\n", .{std.json.fmt(result.data, .{})});
}
```


## Installation

Add the `yamlscript` package to your project and install the `libys.so`
shared library:

```bash
zig fetch --save \
  https://github.com/yaml/yamlscript-zig/archive/refs/tags/v0.2.29.tar.gz
curl -sSL https://yamlscript.org/install | LIB=1 bash
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```

Wire the module into your `build.zig`:

```zig
const yamlscript = b.dependency("yamlscript", .{
    .target = target,
    .optimize = optimize,
});
exe.root_module.addImport("yamlscript", yamlscript.module("yamlscript"));
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* Zig 0.15.2 or higher
* Linux, macOS or Windows
