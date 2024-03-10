// Copyright 2023-2024 Ingy dot Net

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

import { ptr, suffix } from "bun:ffi";
import koffi from "koffi";
import * as fs from "fs";
import * as os from "os";
import * as path from "path";

const yamlscriptVersion = "0.1.41";

function findLibyamlscriptPath() {
  // bun:ffi's `suffix` is automatically os-specific.
  const libyamlscriptName = `libyamlscript.${suffix}.${yamlscriptVersion}`;

  const ldLibraryPath = process.env["LD_LIBRARY_PATH"] || "";
  const ldLibraryPaths = ldLibraryPath.split(":");
  ldLibraryPaths.push("/usr/local/lib");
  ldLibraryPaths.push(path.join(os.homedir(), ".local", "lib"));

  for (const ldPath of ldLibraryPaths) {
    const libyamlscriptPath = path.join(ldPath, libyamlscriptName);
    if (fs.existsSync(libyamlscriptPath)) {
      return libyamlscriptPath;
    }
  }

  throw new Error(`Shared library file '${libyamlscriptName}' not found.`);
}






const lib = koffi.load(findLibyamlscriptPath());

const graal_create_isolate_params_t = koffi.struct(
  "graal_create_isolate_params_t",
  {
    version: "int",
    reserved_address_space_size: "long",
    auxiliary_image_path: "char",
    auxiliary_image_reserved_space_size: "long",

    _reserved_1: "int",
    _reserved_2: "char",
    pkey: "int",
    _reserved_3: "char",
    _reserved_4: "char",
  }
);
const graal_isolate_t = koffi.struct("graal_isolate_t", {
  version: "int",
});
const graal_isolatethread_t = koffi.struct("graal_isolatethread_t", {
  version: "int",
});

const graal_create_isolate = lib.func(
  // "int graal_create_isolate(graal_create_isolate_params_t* params, graal_isolate_t** isolate, graal_isolatethread_t** thread)"
  // "int graal_create_isolate(void* params, int** isolate, int** thread)"
  "int graal_create_isolate(void* params, void* isolate, int** thread)"
);
const graal_tear_down_isolate = lib.func(
  "int graal_tear_down_isolate(int* isolateThread)"
);
const load_ys_to_json = lib.func(
  "char* load_ys_to_json(long long int, const char*)"
);



export class YAMLScript {
  private isolate: Uint8Array;
  private isolateThread: Uint8Array;
  private error?: null | {
    cause: string;
    type: string;
    trace: string;
  };

  constructor() {
    this.isolate = new Uint8Array(200);
    this.isolateThread = new Uint8Array(200);

    console.log("pre graal_create_isolate:", this.isolate, this.isolateThread);
    const createIsolateRet = graal_create_isolate(
      undefined,
      undefined,
      this.isolateThread
    );
    console.log(
      "post graal_create_isolate:",
      createIsolateRet,
      this.isolate,
      this.isolateThread
    );

    if (createIsolateRet !== 0) {
      throw new Error("Failed to create isolate.");
    }
  }

  load(input: string) {
    this.error = null;

    const inputBuffer = Buffer.alloc(20);
    inputBuffer.fill(0);
    inputBuffer.write(input, 0, "utf8");

    console.log(
      "input: ",
      input,
      Buffer.from(input, "utf8"),
      ptr(Buffer.from(input, "utf8")),
      inputBuffer
    );
    console.log("pre load");
    const dataJson = load_ys_to_json(
      this.isolateThread,
      input
    );

    console.log("string JSON", dataJson);
    const resp = JSON.parse(dataJson.toString());
    console.log("parsed JSON'd resp:", resp);

    this.error = resp.error;
    if (this.error) {
      throw new Error(this.error.cause);
    }

    if (!("data" in resp)) {
      throw new Error("Unexpected response from 'libyamlscript'");
    }

    return resp.data;
  }

  destructor() {
    console.log("pre_graal_tear_down_isolate");
    const tearDownIsolateRet = graal_tear_down_isolate(this.isolateThread);
    if (tearDownIsolateRet !== 0) {
      throw new Error("Failed to tear down isolate.");
    }
  }
}

export function load(input: string) {
  console.log("pre constructor");
  const ys = new YAMLScript();
  try {
    return ys.load(input);
  } catch (e) {
    console.error(e);
    throw e;
  } finally {
    console.log("pre destructor");
    ys.destructor();
  }
}


const yamlStr = "a: 1";
console.log(yamlStr);
console.log(load(yamlStr));
