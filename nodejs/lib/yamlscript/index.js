const ffi = require('ffi-napi');
const ref = require('ref-napi');
const path = require('path');
const fs = require('fs');
const os = require('os');

// Helper function to find the libyamlscript shared library path
function findLibyamlscriptPath() {
  const yamlscriptVersion = '0.1.47';
  let soExtension = os.platform() === 'linux' ? 'so' : 'dylib';
  let libyamlscriptName = `libyamlscript.${soExtension}.${yamlscriptVersion}`;
  console.log('libyamlscriptName:', libyamlscriptName);
  let searchPaths = process.env.LD_LIBRARY_PATH
    ? process.env.LD_LIBRARY_PATH.split(':')
    : [];
  searchPaths.push('/usr/local/lib', path.join(os.homedir(), '.local', 'lib'));
  console.log('searchPaths:', searchPaths);

  for (let p of searchPaths) {
    let fullPath = path.join(p, libyamlscriptName);
    if (fs.existsSync(fullPath)) {
      console.log('fullPath:', fullPath);
      return fullPath;
    }
  }
  throw new Error(
`Shared library file '${libyamlscriptName}' not found
Try: curl -sSL yamlscript.org/install | VERSION=${yamlscriptVersion} LIB=1 bash
See: https://github.com/yaml/yamlscript/wiki/Installing-YAMLScript`);
}

// Define the interface to the shared library
let libPath = findLibyamlscriptPath();
let strPtr = ref.refType('string');
let libyamlscript = ffi.Library(libPath, {
  'load_ys_to_json': ['char *', ['pointer', 'string']],
  // 'load_ys_to_json': ['string', ['pointer', strPtr]],
  // 'load_ys_to_json': ['char *', ['pointer', 'char *']],
  // 'load_ys_to_json': ['char *', ['pointer', 'pointer']],
  'graal_create_isolate': ['int', ['pointer', 'pointer', 'pointer']],
  'graal_tear_down_isolate': ['int', ['pointer']]
});

class YAMLScript {
  constructor(config = {}) {
    this.isolatethread = ref.alloc('pointer');
    let rc = libyamlscript.graal_create_isolate(
      null,
      null,
      this.isolatethread,
    );
    console.log('rc:', rc);
    if (rc !== 0) {
      throw new Error('Failed to create GraalVM isolate');
    }
  }

  load(input) {
    console.log('input:', input);
    console.log('isolatethread:', this.isolatethread);
    let buffer = Buffer.from(input);
    let pointer = ref.alloc('pointer');
    let bufferPointer = ref.readPointer(pointer, 0, buffer.length);
    let dataJson = libyamlscript.load_ys_to_json(
      this.isolatethread,
      // input,
      Buffer.from(input, 'utf8'),
      // bufferPointer,
    );
    console.log('dataJson:', dataJson);
    let resp = JSON.parse(dataJson);

    if (resp.error) {
      throw new Error(resp.error.cause);
    }

    if (!('data' in resp)) {
      throw new Error("Unexpected response from 'libyamlscript'");
    }
    return resp.data;
  }

  // Ensure resources are freed
  close() {
    let ret = libyamlscript.graal_tear_down_isolate(this.isolatethread.deref());
    if (ret !== 0) {
      throw new Error("Failed to tear down isolate.");
    }
  }
}

module.exports = YAMLScript;
