// Copyright 2023-2026 Ingy dot Net
// This code is licensed under MIT license (See License for details)

/++
D binding/API for the libys shared library.

This module is a D port of the Python 'yamlscript' module, which is
the reference implementation for YAMLScript FFI bindings to libys.

The current user facing API consists of a single class, `YAMLScript`,
which has a single method: `.load(string)`.
The load() method takes a YAMLScript string as input and returns the
JSONValue that the YAMLScript code evaluates to.
+/
module yamlscript;

import std.conv : to;
import std.file : exists;
import std.json;
import std.process : environment;
import std.string : fromStringz, split, toStringz;

// This value is automatically updated by 'make bump'.
// The version number is used to find the correct shared library file.
// We currently only support binding to an exact version of libys.
enum yamlscriptVersion = "0.2.29";

// We currently only support platforms that GraalVM supports.
// Windows uses an unversioned file name, matching the Python binding:
version (linux)
  enum libysName = "libys.so." ~ yamlscriptVersion;
else version (OSX)
  enum libysName = "libys.dylib." ~ yamlscriptVersion;
else version (Windows)
  enum libysName = "libys.dll";
else
  static assert(0, "Unsupported platform for yamlscript.");

version (Posix)
{
  import core.sys.posix.dlfcn : dlopen, dlsym, dlclose, RTLD_NOW;
}
version (Windows)
{
  import core.sys.windows.winbase :
    LoadLibraryA, GetProcAddress, FreeLibrary;
}

// FFI signatures for the 3 libys functions used by this binding:
extern (C)
{
  alias CreateIsolateFn = int function(void*, void**, void**);
  alias TearDownIsolateFn = int function(void*);
  alias LoadYsToJsonFn = char* function(void*, const(char)*);
}

/// Exception thrown by the YAMLScript loader.
class YAMLScriptException : Exception
{
  this(string msg, string file = __FILE__, size_t line = __LINE__)
  {
    super(msg, file, line);
  }
}

// Find the libys shared library file path.
// Search the platform library path entries, then common install
// locations:
private string findLibysPath()
{
  version (Windows)
  {
    enum envName = "PATH";
    enum sep = ";";
    enum dirSep = "\\";
  }
  else
  {
    enum envName = "LD_LIBRARY_PATH";
    enum sep = ":";
    enum dirSep = "/";
  }

  string[] paths;
  const libraryPath = environment.get(envName);
  if (libraryPath !is null)
  {
    foreach (dir; libraryPath.split(sep))
      if (dir.length > 0)
        paths ~= dir;
  }
  version (Posix)
    paths ~= "/usr/local/lib";
  auto home = environment.get("HOME");
  if (home is null)
    home = environment.get("USERPROFILE");
  if (home !is null)
    paths ~= home ~ dirSep ~ ".local" ~ dirSep ~ "lib";

  foreach (dir; paths)
  {
    const path = dir ~ dirSep ~ libysName;
    if (path.exists)
      return path;
  }

  throw new YAMLScriptException(
    "Shared library file '" ~ libysName ~ "' not found\n" ~
    "Try: curl https://yamlscript.org/install | " ~
    "VERSION=" ~ yamlscriptVersion ~ " LIB=1 bash\n" ~
    "See: https://github.com/yaml/yamlscript/wiki/" ~
    "Installing-YAMLScript\n");
}

/++
The YAMLScript class is the main user facing API for this module.

Usage:
---
import yamlscript;
auto ys = new YAMLScript();
auto data = ys.load(readText("file.ys"));
---
+/
class YAMLScript
{
  private void* lib;
  private void* isolateThread;
  private LoadYsToJsonFn loadYsToJson;
  private TearDownIsolateFn tearDownIsolate;

  /// The error object from the last load() call, if any.
  JSONValue error;

  // Load libys and create a GraalVM isolate for the life of the
  // YAMLScript instance:
  this()
  {
    const path = findLibysPath();
    version (Posix)
      lib = dlopen(path.toStringz, RTLD_NOW);
    version (Windows)
      lib = LoadLibraryA(path.toStringz);
    if (lib is null)
      throw new YAMLScriptException(
        "Failed to load shared library '" ~ path ~ "'");

    auto createIsolate =
      cast(CreateIsolateFn) symbol("graal_create_isolate");
    tearDownIsolate =
      cast(TearDownIsolateFn) symbol("graal_tear_down_isolate");
    loadYsToJson =
      cast(LoadYsToJsonFn) symbol("load_ys_to_json");

    // Create a new GraalVM isolatethread for the instance:
    if (createIsolate(null, null, &isolateThread) != 0)
      throw new YAMLScriptException("Failed to create isolate");
  }

  private void* symbol(string name)
  {
    version (Posix)
      auto sym = dlsym(lib, name.toStringz);
    version (Windows)
      auto sym = GetProcAddress(lib, name.toStringz);
    if (sym is null)
      throw new YAMLScriptException(
        "Symbol '" ~ name ~ "' not found in libys");
    return cast(void*) sym;
  }

  /// Compile and eval a YAMLScript string and return the result.
  JSONValue load(string input)
  {
    // Reset any previous error:
    error = JSONValue.init;

    // Call 'load_ys_to_json' function in libys shared library:
    auto respPtr = loadYsToJson(isolateThread, input.toStringz);
    if (respPtr is null)
      throw new YAMLScriptException("Null response from 'libys'");

    // Decode the JSON response:
    const resp = respPtr.fromStringz.to!string.parseJSON;

    // Check for libys error in JSON response:
    if ("error" in resp)
    {
      error = resp["error"];
      throw new YAMLScriptException(error["cause"].str);
    }

    // Get the response object from evaluating the YAMLScript string:
    if ("data" !in resp)
      throw new YAMLScriptException("Unexpected response from 'libys'");

    // Return the response object:
    return resp["data"];
  }

  /// Tear down the GraalVM isolate and close libys:
  void close()
  {
    if (lib is null)
      return;
    if (tearDownIsolate(isolateThread) != 0)
      throw new YAMLScriptException("Failed to tear down isolate");
    version (Posix)
      dlclose(lib);
    version (Windows)
      FreeLibrary(lib);
    lib = null;
  }
}

unittest
{
  auto ys = new YAMLScript();
  scope (exit)
    ys.close();

  // Load YS code:
  auto data = ys.load("!ys-0:\ntest:: inc(41)");
  assert(data["test"].integer == 42);

  // Load plain YAML:
  data = ys.load("foo: bar");
  assert(data["foo"].str == "bar");

  // Load invalid input throws and sets error:
  bool threw = false;
  try
    ys.load(":");
  catch (YAMLScriptException)
    threw = true;
  assert(threw);
  assert(!ys.error.isNull);

  // Load multiple times on one instance:
  data = ys.load("!ys-0:\ntest:: inc(41)");
  assert(data["test"].integer == 42);
}
