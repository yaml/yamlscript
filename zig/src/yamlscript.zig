// Copyright 2023-2026 Ingy dot Net
// This code is licensed under MIT license (See License for details)

//! Zig binding/API for the libys shared library.
//!
//! This module is a Zig port of the Python 'yamlscript' module, which is
//! the reference implementation for YAMLScript FFI bindings to libys.
//!
//! The current user facing API consists of a single struct, `YAMLScript`,
//! which has a single method: `.load(string)`.
//! The load() method takes a YAMLScript string as input and returns the
//! JSON value that the YAMLScript code evaluates to.

const std = @import("std");
const builtin = @import("builtin");

// This value is automatically updated by 'make bump'.
// The version number is used to find the correct shared library file.
// We currently only support binding to an exact version of libys.
pub const yamlscript_version = "0.2.27";

// We currently only support platforms that GraalVM supports.
// Windows uses an unversioned file name, matching the Python binding:
const libys_name = switch (builtin.os.tag) {
    .linux => "libys.so." ++ yamlscript_version,
    .macos => "libys.dylib." ++ yamlscript_version,
    .windows => "libys.dll",
    else => @compileError("Unsupported platform for yamlscript."),
};

const is_windows = builtin.os.tag == .windows;

// Windows finds DLLs via PATH; other platforms use LD_LIBRARY_PATH:
const lib_path_env = if (is_windows) "PATH" else "LD_LIBRARY_PATH";

// FFI signatures for the 3 libys functions used by this binding:
const CreateIsolateFn = *const fn (
    ?*anyopaque,
    ?*?*anyopaque,
    ?*?*anyopaque,
) callconv(.c) c_int;
const TearDownIsolateFn = *const fn (?*anyopaque) callconv(.c) c_int;
const LoadYsToJsonFn = *const fn (
    ?*anyopaque,
    [*:0]const u8,
) callconv(.c) ?[*:0]const u8;

pub const Error = error{
    LibysNotFound,
    SymbolNotFound,
    IsolateCreateFailed,
    NullResponse,
    BadResponse,
    YAMLScriptError,
};

/// The result of a successful YAMLScript.load() call.
/// Owns the JSON arena; call deinit() when done with the data.
pub const Result = struct {
    parsed: std.json.Parsed(std.json.Value),
    data: std.json.Value,

    pub fn deinit(self: Result) void {
        self.parsed.deinit();
    }
};

// Join a candidate path and return it (owned) if the file exists:
fn checkDir(allocator: std.mem.Allocator, dir: []const u8) ?[]u8 {
    const path = std.fs.path.join(
        allocator,
        &.{ dir, libys_name },
    ) catch return null;
    std.fs.cwd().access(path, .{}) catch {
        allocator.free(path);
        return null;
    };
    return path;
}

// Find the libys shared library file path (owned by caller).
// Search the platform library path entries, then common install
// locations:
fn findLibys(allocator: std.mem.Allocator) ?[]u8 {
    if (std.process.getEnvVarOwned(allocator, lib_path_env)) |paths| {
        defer allocator.free(paths);
        var dirs = std.mem.splitScalar(
            u8,
            paths,
            std.fs.path.delimiter,
        );
        while (dirs.next()) |dir| {
            if (dir.len == 0) continue;
            if (checkDir(allocator, dir)) |path| return path;
        }
    } else |_| {}

    if (!is_windows) {
        if (checkDir(allocator, "/usr/local/lib")) |path| return path;
    }

    for ([_][]const u8{ "HOME", "USERPROFILE" }) |env_name| {
        const home = std.process.getEnvVarOwned(
            allocator,
            env_name,
        ) catch continue;
        defer allocator.free(home);
        const dir = std.fs.path.join(
            allocator,
            &.{ home, ".local", "lib" },
        ) catch continue;
        defer allocator.free(dir);
        if (checkDir(allocator, dir)) |path| return path;
    }

    return null;
}

// Open the libys shared library or explain how to install it:
fn openLibys(allocator: std.mem.Allocator) !std.DynLib {
    const path = findLibys(allocator) orelse {
        std.log.err(
            \\Shared library file '{s}' not found
            \\Try: curl https://yamlscript.org/install | VERSION={s} LIB=1 bash
            \\See: https://github.com/yaml/yamlscript/wiki/Installing-YAMLScript
        , .{ libys_name, yamlscript_version });
        return Error.LibysNotFound;
    };
    defer allocator.free(path);
    return std.DynLib.open(path);
}

/// The YAMLScript struct is the main user facing API for this module.
///
/// Usage:
///   var ys = try yamlscript.YAMLScript.init(allocator);
///   defer ys.deinit();
///   var result = try ys.load(input);
///   defer result.deinit();
///
/// A GraalVM isolate is thread-affine, so an instance must be used from
/// the thread that created it.
pub const YAMLScript = struct {
    allocator: std.mem.Allocator,
    lib: std.DynLib,
    isolate_thread: ?*anyopaque = null,
    load_ys_to_json: LoadYsToJsonFn,
    tear_down_isolate: TearDownIsolateFn,
    // The 'cause' message of the last YAMLScriptError (owned):
    error_cause: ?[]u8 = null,

    /// Load libys and create a GraalVM isolate for the life of the
    /// YAMLScript instance.
    pub fn init(allocator: std.mem.Allocator) !YAMLScript {
        var lib = try openLibys(allocator);
        errdefer lib.close();

        const create_isolate = lib.lookup(
            CreateIsolateFn,
            "graal_create_isolate",
        ) orelse return Error.SymbolNotFound;

        var self = YAMLScript{
            .allocator = allocator,
            .lib = lib,
            .load_ys_to_json = lib.lookup(
                LoadYsToJsonFn,
                "load_ys_to_json",
            ) orelse return Error.SymbolNotFound,
            .tear_down_isolate = lib.lookup(
                TearDownIsolateFn,
                "graal_tear_down_isolate",
            ) orelse return Error.SymbolNotFound,
        };

        if (create_isolate(null, null, &self.isolate_thread) != 0)
            return Error.IsolateCreateFailed;

        return self;
    }

    /// Compile and eval a YAMLScript string and return the Result.
    /// On Error.YAMLScriptError the message is in self.error_cause.
    pub fn load(self: *YAMLScript, input: []const u8) !Result {
        // Reset any previous error:
        if (self.error_cause) |cause| {
            self.allocator.free(cause);
            self.error_cause = null;
        }

        const input_z = try self.allocator.dupeZ(u8, input);
        defer self.allocator.free(input_z);

        // Call 'load_ys_to_json' function in libys shared library.
        // The returned C string is owned by the GraalVM heap:
        const resp_ptr = self.load_ys_to_json(
            self.isolate_thread,
            input_z,
        ) orelse return Error.NullResponse;

        // Decode the JSON response:
        var parsed = try std.json.parseFromSlice(
            std.json.Value,
            self.allocator,
            std.mem.span(resp_ptr),
            .{},
        );
        errdefer parsed.deinit();

        const resp = switch (parsed.value) {
            .object => |object| object,
            else => return Error.BadResponse,
        };

        // Check for libys error in JSON response:
        if (resp.get("error")) |err| {
            if (err == .object) {
                if (err.object.get("cause")) |cause| {
                    if (cause == .string) {
                        self.error_cause =
                            try self.allocator.dupe(u8, cause.string);
                    }
                }
            }
            return Error.YAMLScriptError;
        }

        // Get the data value from evaluating the YAMLScript string:
        const data = resp.get("data") orelse return Error.BadResponse;

        return Result{ .parsed = parsed, .data = data };
    }

    /// Tear down the GraalVM isolate and close libys:
    pub fn deinit(self: *YAMLScript) void {
        if (self.tear_down_isolate(self.isolate_thread) != 0)
            std.log.warn("Failed to tear down isolate", .{});
        self.lib.close();
        if (self.error_cause) |cause| self.allocator.free(cause);
        self.* = undefined;
    }
};

test "load ys code" {
    var ys = try YAMLScript.init(std.testing.allocator);
    defer ys.deinit();

    var result = try ys.load("!ys-0:\ntest:: inc(41)");
    defer result.deinit();

    try std.testing.expectEqual(
        @as(i64, 42),
        result.data.object.get("test").?.integer,
    );
}

test "load plain yaml" {
    var ys = try YAMLScript.init(std.testing.allocator);
    defer ys.deinit();

    var result = try ys.load("foo: bar");
    defer result.deinit();

    try std.testing.expectEqualStrings(
        "bar",
        result.data.object.get("foo").?.string,
    );
}

test "load error" {
    var ys = try YAMLScript.init(std.testing.allocator);
    defer ys.deinit();

    try std.testing.expectError(Error.YAMLScriptError, ys.load(":"));
    try std.testing.expect(ys.error_cause != null);
}

test "load multiple times" {
    var ys = try YAMLScript.init(std.testing.allocator);
    defer ys.deinit();

    for (0..2) |_| {
        var result = try ys.load("!ys-0:\ntest:: inc(41)");
        defer result.deinit();

        try std.testing.expectEqual(
            @as(i64, 42),
            result.data.object.get("test").?.integer,
        );
    }
}
