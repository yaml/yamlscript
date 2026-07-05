# Copyright 2023-2026 Ingy dot Net
# This code is licensed under MIT license (See License for details)

# Test the yamlscript Nim binding.

import std/json

import yamlscript

var fails = 0

proc check(cond: bool, label: string) =
  if cond:
    echo "ok - ", label
  else:
    echo "not ok - ", label
    inc fails

let ys = newYAMLScript()

# Load YS code:
var data = ys.load("!ys-0:\ntest:: inc(41)")
check(data["test"].getInt == 42, "load ys code")

# Load plain YAML:
data = ys.load("foo: bar")
check(data["foo"].getStr == "bar", "load plain yaml")

# Load invalid input raises and sets error:
var threw = false
try:
  discard ys.load(":")
except YAMLScriptError:
  threw = true
check(threw, "load error raises")
check(ys.error != nil, "error object is set")

# Load multiple times on one instance:
data = ys.load("!ys-0:\ntest:: inc(41)")
check(data["test"].getInt == 42, "load multiple times")

ys.close()

if fails > 0:
  echo fails, " test(s) failed"
  quit(1)
