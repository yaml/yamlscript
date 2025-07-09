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
