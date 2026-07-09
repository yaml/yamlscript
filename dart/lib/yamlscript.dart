// Copyright 2023-2026 Ingy dot Net
// This code is licensed under MIT license (See License for details)

/// Dart binding/API for the libys shared library.
///
/// This module is a Dart port of the Python 'yamlscript' module, which is
/// the reference implementation for YAMLScript FFI bindings to libys.
///
/// The current user facing API consists of a single class, `YAMLScript`,
/// which has a single method: `.load(string)`.
/// The load() method takes a YAMLScript string as input and returns the
/// Dart object that the YAMLScript code evaluates to.
library;

import 'dart:convert';
import 'dart:ffi';
import 'dart:io';

import 'package:ffi/ffi.dart';

// This value is automatically updated by 'make bump'.
// The version number is used to find the correct shared library file.
// We currently only support binding to an exact version of libys.
const String yamlscriptVersion = '0.2.27';

typedef _CreateIsolateC = Int32 Function(
  Pointer<Void>,
  Pointer<Pointer<Void>>,
  Pointer<Pointer<Void>>,
);
typedef _CreateIsolateDart = int Function(
  Pointer<Void>,
  Pointer<Pointer<Void>>,
  Pointer<Pointer<Void>>,
);
typedef _TearDownIsolateC = Int32 Function(Pointer<Void>);
typedef _TearDownIsolateDart = int Function(Pointer<Void>);
typedef _LoadYsToJsonC = Pointer<Utf8> Function(Pointer<Void>, Pointer<Utf8>);
typedef _LoadYsToJsonDart = Pointer<Utf8> Function(
  Pointer<Void>,
  Pointer<Utf8>,
);

/// Error thrown by the YAMLScript loader.
class YAMLScriptError implements Exception {
  final String message;
  YAMLScriptError(this.message);
  @override
  String toString() => 'YAMLScriptError: $message';
}

// Find the libys shared library file path:
String _findLibysPath() {
  // We currently only support platforms that GraalVM supports.
  // Windows uses an unversioned file name, matching the Python binding:
  final String libysName;
  if (Platform.isLinux) {
    libysName = 'libys.so.$yamlscriptVersion';
  } else if (Platform.isMacOS) {
    libysName = 'libys.dylib.$yamlscriptVersion';
  } else if (Platform.isWindows) {
    libysName = 'libys.dll';
  } else {
    throw YAMLScriptError(
      "Unsupported platform '${Platform.operatingSystem}' for yamlscript.",
    );
  }

  // Use the platform library path plus common install locations:
  final envName = Platform.isWindows ? 'PATH' : 'LD_LIBRARY_PATH';
  final sep = Platform.isWindows ? ';' : ':';
  final paths = <String>[];
  final libraryPath = Platform.environment[envName];
  if (libraryPath != null) {
    paths.addAll(libraryPath.split(sep).where((p) => p.isNotEmpty));
  }
  if (!Platform.isWindows) {
    paths.add('/usr/local/lib');
  }
  final home =
      Platform.environment['HOME'] ?? Platform.environment['USERPROFILE'];
  if (home != null) {
    paths.add(
      [home, '.local', 'lib'].join(Platform.pathSeparator),
    );
  }

  for (final dir in paths) {
    final path = '$dir${Platform.pathSeparator}$libysName';
    if (File(path).existsSync()) return path;
  }

  throw YAMLScriptError('''
Shared library file '$libysName' not found
Try: curl https://yamlscript.org/install | VERSION=$yamlscriptVersion LIB=1 bash
See: https://github.com/yaml/yamlscript/wiki/Installing-YAMLScript
''');
}

/// The YAMLScript class is the main user facing API for this module.
///
/// Usage:
///   import 'package:yamlscript/yamlscript.dart';
///   final ys = YAMLScript();
///   final data = ys.load(File('file.ys').readAsStringSync());
class YAMLScript {
  late final DynamicLibrary _libys;
  late final _LoadYsToJsonDart _loadYsToJson;
  late final _TearDownIsolateDart _tearDownIsolate;
  late final Pointer<Void> _isolateThread;

  /// The error object from the last load() call, if any.
  Map<String, dynamic>? error;

  /// Load libys and create a GraalVM isolate for the life of the
  /// YAMLScript instance.
  YAMLScript() {
    _libys = DynamicLibrary.open(_findLibysPath());

    final createIsolate =
        _libys.lookupFunction<_CreateIsolateC, _CreateIsolateDart>(
            'graal_create_isolate');
    _tearDownIsolate =
        _libys.lookupFunction<_TearDownIsolateC, _TearDownIsolateDart>(
            'graal_tear_down_isolate');
    _loadYsToJson = _libys
        .lookupFunction<_LoadYsToJsonC, _LoadYsToJsonDart>('load_ys_to_json');

    // Create a new GraalVM isolatethread for the instance:
    final threadPtr = calloc<Pointer<Void>>();
    try {
      final rc = createIsolate(nullptr, nullptr, threadPtr);
      if (rc != 0) {
        throw YAMLScriptError('Failed to create isolate');
      }
      _isolateThread = threadPtr.value;
    } finally {
      calloc.free(threadPtr);
    }
  }

  /// Compile and eval a YAMLScript string and return the result.
  dynamic load(String input) {
    // Reset any previous error:
    error = null;

    // Call 'load_ys_to_json' function in libys shared library:
    final inputPtr = input.toNativeUtf8();
    final String dataJson;
    try {
      final respPtr = _loadYsToJson(_isolateThread, inputPtr);
      dataJson = respPtr.toDartString();
    } finally {
      calloc.free(inputPtr);
    }

    // Decode the JSON response:
    final resp = jsonDecode(dataJson) as Map<String, dynamic>;

    // Check for libys error in JSON response:
    final err = resp['error'];
    if (err != null) {
      error = err as Map<String, dynamic>;
      throw YAMLScriptError(error!['cause'] as String);
    }

    // Get the response object from evaluating the YAMLScript string:
    if (!resp.containsKey('data')) {
      throw YAMLScriptError("Unexpected response from 'libys'");
    }

    // Return the response object:
    return resp['data'];
  }

  /// Tear down the GraalVM isolate to free resources:
  void dispose() {
    final rc = _tearDownIsolate(_isolateThread);
    if (rc != 0) {
      throw YAMLScriptError('Failed to tear down isolate');
    }
  }
}
