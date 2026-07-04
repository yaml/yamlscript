// Copyright 2023-2026 Ingy dot Net
// This code is licensed under MIT license (See License for details)

// Test the yamlscript Dart binding.
// Run with: dart run test/main.dart

import 'dart:io';

import 'package:yamlscript/yamlscript.dart';

var fails = 0;

void check(bool cond, String label) {
  if (cond) {
    print('ok - $label');
  } else {
    print('not ok - $label');
    fails++;
  }
}

void main() {
  final ys = YAMLScript();

  var data = ys.load('!ys-0:\ntest:: inc(41)');
  check(data['test'] == 42, 'load ys code');

  data = ys.load('foo: bar');
  check(data['foo'] == 'bar', 'load plain yaml');

  var threw = false;
  try {
    ys.load(':');
  } on YAMLScriptError {
    threw = true;
  }
  check(threw, 'load error throws');
  check(ys.error != null, 'error object is set');

  data = ys.load('!ys-0:\ntest:: inc(41)');
  check(data['test'] == 42, 'load multiple times');

  ys.dispose();

  if (fails > 0) {
    stderr.writeln('$fails test(s) failed');
    exit(1);
  }
}
