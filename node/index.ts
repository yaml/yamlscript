// Copyright 2023-2024 Ingy dot Net
// This code is licensed under MIT license (See License for details)

/**
 * TypeScript binding/API for the libyamlscript shared library.
 *
 * This module can be considered the reference implementation for YAMLScript
 * FFI bindings to libyamlscript.
 *
 * The current user facing API consists of a single class, `YAMLScript`, which
 * has a single method: `.load(string)`.
 * The load() method takes a YAMLScript string as input and returns the Python
 * object that the YAMLScript code evaluates to.
 */

import { dlopen, FFIType, suffix, ptr } from "bun:ffi";
import * as os from "os";
import * as fs from "fs";
import * as path from "path";

// This value is automatically updated by 'make bump'.
// TODO: ^ this is not actually true yet, make it so.
//
// The version number is used to find the correct shared library file.
// We currently only support binding to an exact version of libyamlscript.
const yamlscriptVersion = '0.1.41';

// Find the libyamlscript shared library file path:
function findLibyamlscriptPath() {
  // bun:ffi's `suffix` is automatically os-specific.
  const libyamlscriptName = `libyamlscript.${suffix}.${yamlscriptVersion}`;

  const ldLibraryPath = process.env["LD_LIBRARY_PATH"] || '';
  const ldLibraryPaths = ldLibraryPath.split(':');
  ldLibraryPaths.push('/usr/local/lib');
  ldLibraryPaths.push(path.join(os.homedir(), '.local', 'lib'));

  for (const ldPath of ldLibraryPaths) {
    const libyamlscriptPath = path.join(ldPath, libyamlscriptName);
    if (fs.existsSync(libyamlscriptPath)) {
      return libyamlscriptPath;
    }
  }

  throw new Error(`Shared library file '${libyamlscriptName}' not found.`);
}

// Load libyamlscript shared library:
const { symbols: {
  graal_create_isolate,
  load_ys_to_json,
  graal_tear_down_isolate,
} } = dlopen(findLibyamlscriptPath(), {
  load_ys_to_json: { // (isolateThread, ysString) => jsonString
    args: [FFIType.ptr, FFIType.cstring],
    returns: FFIType.cstring,
  },
  graal_create_isolate: { // (options, noop?, isolateThread) => unknown?
    args: [FFIType.ptr, FFIType.ptr, FFIType.ptr],
    returns: FFIType.int,
  },
  graal_tear_down_isolate: { // (isolateThread) => unknown?
    args: [FFIType.ptr],
    returns: FFIType.int,
  }
});

export class YAMLScript {
  private isolatethread: Uint8Array;
  private error?: null | {
    cause: string,
    type: string,
    trace: string
  };

  constructor() {
    // this.isolatethread = Buffer.alloc(0);
    this.isolatethread = new Uint8Array(8);

    console.log('this.isolateThread: ', this.isolatethread)
    console.log('pre graal_create_isolate')
    graal_create_isolate(
      null,
      null,
      this.isolatethread
    );
    console.log('post graal_create_isolate:', this.isolatethread)
  }

  load(input: string) {
    this.error = null;

    console.log('input: ', input);
    // FIXME: seg fault here
    const dataJson = load_ys_to_json(
      this.isolatethread,
      ptr(Buffer.from(input, "utf8"))
    );

    console.log('string JSON', dataJson);
    const resp = JSON.parse(dataJson.toString());
    console.log('parsed JSON\'d resp:');

    this.error = resp.error;
    if (this.error) {
      throw new Error(this.error.cause);
    }

    if (!('data' in resp)) {
      throw new Error("Unexpected response from 'libyamlscript'");
    }

    return resp.data;
  }

  destructor() {
    console.log('pre_graal_tear_down_isolate');
    const ret = graal_tear_down_isolate(this.isolatethread);
    if (ret !== 0) {
      throw new Error("Failed to tear down isolate.");
    }
  }
}

export function load(input: string) {
  console.log("pre constructor");
  const ys = new YAMLScript();
  try {
    return ys.load(input);
  } finally {
    console.log("pre destructor")
    ys.destructor();
  }
}

// Example usage:

const yamlFilePath = path.join(__dirname, './../sample/data.ys');
const yamlStr = fs.readFileSync(yamlFilePath, 'utf8');
console.log(yamlStr);
console.log(load(yamlStr))
