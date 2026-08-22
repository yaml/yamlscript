// Copyright 2023-2026 Ingy dot Net
// This code is licensed under MIT license (See License for details)

// Swift binding/API for the libys shared library.
//
// This module is a Swift port of the Python 'yamlscript' module, which
// is the reference implementation for YAMLScript FFI bindings to libys.
//
// The current user facing API consists of a single class, `YAMLScript`,
// which has a single method: `.load(string)`.
// The load() method takes a YAMLScript string as input and returns the
// object that the YAMLScript code evaluates to.

#if os(Linux)
    import Glibc
#elseif os(macOS)
    import Darwin
#else
    #error("Unsupported platform for yamlscript.")
#endif
import Foundation

// This value is automatically updated by 'make bump'.
// The version number is used to find the correct shared library file.
// We currently only support binding to an exact version of libys.
public let yamlscriptVersion = "0.2.31"

#if os(Linux)
    let libysName = "libys.so.\(yamlscriptVersion)"
#elseif os(macOS)
    let libysName = "libys.dylib.\(yamlscriptVersion)"
#endif

// FFI signatures for the 3 libys functions used by this binding:
private typealias CreateIsolateFn = @convention(c) (
    UnsafeMutableRawPointer?,
    UnsafeMutablePointer<UnsafeMutableRawPointer?>?,
    UnsafeMutablePointer<UnsafeMutableRawPointer?>?
) -> Int32
private typealias TearDownIsolateFn = @convention(c) (
    UnsafeMutableRawPointer?
) -> Int32
private typealias LoadYsToJsonFn = @convention(c) (
    UnsafeMutableRawPointer?, UnsafePointer<CChar>?
) -> UnsafePointer<CChar>?

/// Error thrown by the YAMLScript loader.
public struct YAMLScriptError: Error, CustomStringConvertible {
    public let message: String
    public var description: String { "YAMLScriptError: \(message)" }
}

// Find the libys shared library file path.
// Search LD_LIBRARY_PATH entries, then common install locations:
private func findLibysPath() throws -> String {
    var paths: [String] = []
    let env = ProcessInfo.processInfo.environment
    if let libraryPath = env["LD_LIBRARY_PATH"] {
        paths.append(
            contentsOf: libraryPath.split(separator: ":").map(String.init))
    }
    paths.append("/usr/local/lib")
    if let home = env["HOME"] {
        paths.append("\(home)/.local/lib")
    }

    for dir in paths {
        let path = "\(dir)/\(libysName)"
        if FileManager.default.fileExists(atPath: path) {
            return path
        }
    }

    throw YAMLScriptError(
        message: """
            Shared library file '\(libysName)' not found
            Try: curl https://yamlscript.org/install | \
            VERSION=\(yamlscriptVersion) LIB=1 bash
            See: https://github.com/yaml/yamlscript/wiki/\
            Installing-YAMLScript
            """)
}

/// The YAMLScript class is the main user facing API for this module.
///
/// Usage:
///     import YAMLScript
///     let ys = try YAMLScript()
///     let data = try ys.load(input)
public final class YAMLScript {
    private let handle: UnsafeMutableRawPointer
    private var isolateThread: UnsafeMutableRawPointer?
    private let loadYsToJson: LoadYsToJsonFn
    private let tearDownIsolate: TearDownIsolateFn

    /// The error object from the last load() call, if any.
    public private(set) var error: [String: Any]?

    // Load libys and create a GraalVM isolate for the life of the
    // YAMLScript instance:
    public init() throws {
        let path = try findLibysPath()
        guard let handle = dlopen(path, RTLD_NOW) else {
            throw YAMLScriptError(
                message: "Failed to load shared library '\(path)'")
        }
        self.handle = handle

        func symbol<T>(_ name: String, _ type: T.Type) throws -> T {
            guard let sym = dlsym(handle, name) else {
                throw YAMLScriptError(
                    message: "Symbol '\(name)' not found in libys")
            }
            return unsafeBitCast(sym, to: T.self)
        }

        let createIsolate = try symbol(
            "graal_create_isolate", CreateIsolateFn.self)
        tearDownIsolate = try symbol(
            "graal_tear_down_isolate", TearDownIsolateFn.self)
        loadYsToJson = try symbol(
            "load_ys_to_json", LoadYsToJsonFn.self)

        // Create a new GraalVM isolatethread for the instance:
        guard createIsolate(nil, nil, &isolateThread) == 0 else {
            throw YAMLScriptError(message: "Failed to create isolate")
        }
    }

    /// Compile and eval a YAMLScript string and return the result.
    public func load(_ input: String) throws -> Any? {
        // Reset any previous error:
        error = nil

        // Call 'load_ys_to_json' function in libys shared library:
        guard let respPtr = loadYsToJson(isolateThread, input) else {
            throw YAMLScriptError(message: "Null response from 'libys'")
        }

        // Decode the JSON response:
        let respData = Data(String(cString: respPtr).utf8)
        let resp = try JSONSerialization.jsonObject(
            with: respData, options: [.fragmentsAllowed]
        )
        guard let resp = resp as? [String: Any] else {
            throw YAMLScriptError(
                message: "Unexpected response from 'libys'")
        }

        // Check for libys error in JSON response:
        if let err = resp["error"] as? [String: Any] {
            error = err
            throw YAMLScriptError(
                message: err["cause"] as? String ?? "unknown error")
        }

        // Get the response object from evaluating the YAMLScript
        // string:
        guard resp.keys.contains("data") else {
            throw YAMLScriptError(
                message: "Unexpected response from 'libys'")
        }

        // Return the response object ('data' may be JSON null):
        return resp["data"]
    }

    deinit {
        // Tear down the isolate thread to free resources:
        if tearDownIsolate(isolateThread) != 0 {
            FileHandle.standardError.write(
                Data("Failed to tear down isolate\n".utf8))
        }
        dlclose(handle)
    }
}
