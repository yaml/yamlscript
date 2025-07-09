<!-- DO NOT EDIT — THIS FILE WAS GENERATED -->

YS / YAMLScript
===============

Add Logic to Your YAML Files


## Synopsis

Load `file.yaml` with YS:

```yaml
!YS-v0:

# Get data from external sources:
names-url =:
  'github:dominictarr/random-name/first-names.json'

name-list =: names-url:curl:json/load

# Data object with literal keys and generated values:
name:: name-list:shuffle:first
aka:: name-list:rand-nth
age:: &num 2 * 3 * 7
color:: &hue
  rand-nth: qw(red green blue yellow)
title:: "$(*num) shades of $(*hue)."
```

and get:
```json
{
  "name": "Dolores",
  "aka": "Anita",
  "age": 42,
  "color": "green",
  "title": "42 shades of green."
}
```


## Description

[YS](https://yamlscript.org) is a functional programming language with a clean
YAML syntax.

YS can be used for enhancing ordinary [YAML](https://yaml.org) files with
functional operations, such as:

* Import (parts of) other YAML files to any node
* String interpolation including function calls
* Data transforms including ones defined by you

This YS library should be a drop-in replacement for your current YAML loader!

Most existing YAML files are already valid YS files.
This means that YS works as a normal YAML loader, but can also evaluate
functional expressions if asked to.

Under the hood, YS code compiles to the Clojure programming language.
This makes YS a complete functional programming language right out of the box.

Even though YS compiles to Clojure, and Clojure compiles to Java, there is no
dependency on Java or the JVM.
YS is compiled to a native shared library (`libys.so`) that can be used
by any programming language that can load shared libraries.

To see the Clojure code that YS compiles to, you can use the YS
CLI binary `ys` to run:

```text
$ ys --compile file.ys
(let
 [names-url "https://raw.githubusercontent.com/dominictarr/random-name/master/first-names.json"
  name-list (json/load (curl names-url))]
 (%
  "name" (first (shuffle name-list))
  "aka" (rand-nth name-list)
  "age" (_& 'num (mul+ 2 3 7))
  "color" (_& 'hue (rand-nth (qw red green blue yellow)))
  "title" (str (_** 'num) " shades of " (_** 'hue) ".")))
```

# YAMLScript C# Implementation Details

This document describes the implementation details of the YAMLScript C# binding.

## Architecture

The C# binding consists of several key components:

1. Native FFI Layer
- Uses C# P/Invoke to interface with YAMLScript core
- Handles memory management and resource cleanup
- Manages GraalVM isolates

2. High-Level API
- Provides an idiomatic C# interface
- Handles type conversions between C# and YAMLScript
- Implements proper error handling and exceptions

3. Testing Infrastructure
- Unit tests for API functionality
- Integration tests with YAMLScript core
- Memory leak detection
- Performance benchmarks

## Implementation Notes

### FFI Integration
The binding uses P/Invoke to interface with the YAMLScript core library.
Memory management is handled through proper disposal patterns and the
IDisposable interface.

### Type System
YAMLScript types are mapped to C# types as follows:
- YAMLScript null → C# null
- YAMLScript boolean → C# bool
- YAMLScript number → C# double
- YAMLScript string → C# string
- YAMLScript array → C# IList<object>
- YAMLScript object → C# IDictionary<string, object>

### Error Handling
Errors from the YAMLScript runtime are converted to appropriate C#
exceptions with meaningful stack traces and context information.

### Memory Management
The binding implements proper memory management through:
- Deterministic disposal of unmanaged resources
- Reference counting for shared resources
- Automatic cleanup of GraalVM isolates

## Build System
The project uses the standard .NET build system with MSBuild, integrated
with the YAMLScript common build infrastructure.

## Testing Strategy
Tests are implemented using xUnit and cover:
- API functionality
- Error conditions
- Memory management
- Performance benchmarks
- Edge cases

## Dependencies
- .NET 8.0 or later
- YAMLScript core library
- xUnit for testing

## See Also

* [YS Web Site](https://yamlscript.org)
* [YS Blog](https://yamlscript.org/blog)
* [YS Source Code](https://github.com/yaml/yamlscript)
* [YS Samples](https://github.com/yaml/yamlscript/tree/main/sample)
* [YS Programs](https://rosettacode.org/wiki/Category:YAMLScript)
* [YAML](https://yaml.org)
* [Clojure](https://clojure.org)


## Authors

# YAMLScript C# Binding Authors

## Maintainers

## Contributors

This file will be updated as contributors add to the C# binding.

## License & Copyright

Copyright 2022-2025 Ingy döt Net <ingy@ingy.net>

This project is licensed under the terms of the `MIT` license.
See [LICENSE](https://github.com/yaml/yamlscript/blob/main/License) for
more details.
