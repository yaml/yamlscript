# Copyright 2023-2026 Ingy dot Net
# This code is licensed under MIT license (See License for details)

## Nim binding/API for the libys shared library.
##
## This module is a Nim port of the Python 'yamlscript' module, which
## is the reference implementation for YAMLScript FFI bindings to
## libys.
##
## The current user facing API consists of a single type,
## `YAMLScript`, which has a single method: `load(string)`.
## The load() method takes a YAMLScript string as input and returns
## the JsonNode that the YAMLScript code evaluates to.

import std/[dynlib, json, os, strutils]

# This value is automatically updated by 'make bump'.
# The version number is used to find the correct shared library file.
# We currently only support binding to an exact version of libys.
const yamlscriptVersion* = "0.2.25"

# We currently only support platforms that GraalVM supports.
# Windows uses an unversioned file name, matching the Python binding:
when defined(linux):
  const libysName = "libys.so." & yamlscriptVersion
elif defined(macosx):
  const libysName = "libys.dylib." & yamlscriptVersion
elif defined(windows):
  const libysName = "libys.dll"
else:
  {.error: "Unsupported platform for yamlscript.".}

type
  ## Error raised by the YAMLScript loader:
  YAMLScriptError* = object of CatchableError

  # FFI signatures for the 3 libys functions used by this binding:
  CreateIsolateFn = proc (
    params: pointer, isolate: ptr pointer, thread: ptr pointer,
  ): cint {.cdecl.}
  TearDownIsolateFn = proc (thread: pointer): cint {.cdecl.}
  LoadYsToJsonFn = proc (
    thread: pointer, input: cstring,
  ): cstring {.cdecl.}

  ## The YAMLScript type is the main user facing API for this module.
  YAMLScript* = ref object
    lib: LibHandle
    isolateThread: pointer
    loadYsToJson: LoadYsToJsonFn
    tearDownIsolate: TearDownIsolateFn
    ## The error object from the last load() call, if any:
    error*: JsonNode

# Find the libys shared library file path.
# Search the platform library path entries, then common install
# locations:
proc findLibysPath(): string =
  when defined(windows):
    const envName = "PATH"
    const sep = ';'
  else:
    const envName = "LD_LIBRARY_PATH"
    const sep = ':'

  var paths: seq[string]
  let libraryPath = getEnv(envName)
  if libraryPath.len > 0:
    for dir in libraryPath.split(sep):
      if dir.len > 0:
        paths.add(dir)
  when not defined(windows):
    paths.add("/usr/local/lib")
  var home = getEnv("HOME")
  if home.len == 0:
    home = getEnv("USERPROFILE")
  if home.len > 0:
    paths.add(home / ".local" / "lib")

  for dir in paths:
    let path = dir / libysName
    if fileExists(path):
      return path

  raise newException(YAMLScriptError, """
Shared library file '$1' not found
Try: curl https://yamlscript.org/install | VERSION=$2 LIB=1 bash
See: https://github.com/yaml/yamlscript/wiki/Installing-YAMLScript
""" % [libysName, yamlscriptVersion])

# Load a symbol from libys or fail:
proc symbol(lib: LibHandle, name: string): pointer =
  result = lib.symAddr(name.cstring)
  if result == nil:
    raise newException(
      YAMLScriptError, "Symbol '" & name & "' not found in libys")

## Load libys and create a GraalVM isolate for the life of the
## YAMLScript instance.
proc newYAMLScript*(): YAMLScript =
  let lib = loadLib(findLibysPath())
  if lib == nil:
    raise newException(
      YAMLScriptError, "Failed to load shared library '" &
      libysName & "'")

  result = YAMLScript(lib: lib)
  let createIsolate =
    cast[CreateIsolateFn](lib.symbol("graal_create_isolate"))
  result.tearDownIsolate =
    cast[TearDownIsolateFn](lib.symbol("graal_tear_down_isolate"))
  result.loadYsToJson =
    cast[LoadYsToJsonFn](lib.symbol("load_ys_to_json"))

  # Create a new GraalVM isolatethread for the instance:
  if createIsolate(nil, nil, addr result.isolateThread) != 0:
    raise newException(YAMLScriptError, "Failed to create isolate")

## Compile and eval a YAMLScript string and return the result.
proc load*(ys: YAMLScript, input: string): JsonNode =
  # Reset any previous error:
  ys.error = nil

  # Call 'load_ys_to_json' function in libys shared library:
  let respPtr = ys.loadYsToJson(ys.isolateThread, input.cstring)
  if respPtr == nil:
    raise newException(YAMLScriptError, "Null response from 'libys'")

  # Decode the JSON response:
  let resp = parseJson($respPtr)

  # Check for libys error in JSON response:
  if resp.hasKey("error"):
    ys.error = resp["error"]
    raise newException(YAMLScriptError, ys.error["cause"].getStr)

  # Get the response object from evaluating the YAMLScript string:
  if not resp.hasKey("data"):
    raise newException(
      YAMLScriptError, "Unexpected response from 'libys'")

  # Return the response object:
  resp["data"]

## Tear down the GraalVM isolate and close libys.
proc close*(ys: YAMLScript) =
  if ys.lib != nil:
    if ys.tearDownIsolate(ys.isolateThread) != 0:
      raise newException(
        YAMLScriptError, "Failed to tear down isolate")
    unloadLib(ys.lib)
    ys.lib = nil
