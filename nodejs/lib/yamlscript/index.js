const yamlscriptVersion = '0.1.51';

const ffi = require('ffi-napi');
const ref = require('ref-napi');
const path = require('path');
const fs = require('fs');
const os = require('os');

// Define the interface to the shared library
function defineInterface() {
  let libPath = findLibyamlscriptPath();
  return ffi.Library(libPath, {
    'print_hello': ['void', ['pointer']],
    'triple_num': ['int', ['pointer', 'int']],
    'load_ys_to_json': ['string', ['pointer', 'string']],
    'graal_create_isolate': ['int', ['pointer', 'pointer', 'pointer']],
    'graal_tear_down_isolate': ['int', ['pointer']]
  });
}

class YAMLScript {
  constructor(config = {}) {
    this.libyamlscript = defineInterface();
    this.isolatethread = ref.NULL_POINTER;

    console.log(
      "JS constructor - Isolate thread pointer before graal_create_isolate:",
      this.isolatethread.deref());

    let rc = this.libyamlscript.graal_create_isolate(
      null,
      null,
      this.isolatethread,
    );

    console.log('JS constructor - graal_isolate_create rc:', rc);
    console.log(
      "JS constructor - Isolate thread pointer after graal_create_isolate:",
      this.isolatethread.deref());

    if (rc !== 0) {
      throw new Error('Failed to create GraalVM isolate');
    }
  }

  hello() {
    console.log('JS hello - Calling libyamlscript.print_hello');
    let rc = this.libyamlscript.print_hello(this.isolatethread.deref());
    console.log('JS hello - return value:', rc);
  }

  triple(num) {
    console.log('JS triple - call:', num);
    let val = this.libyamlscript.triple_num(this.isolatethread.deref(), num);
    console.log('JS triple - return val:', val);
    return val;
  }

  load(input) {
    console.log('JS load - input:', input);
    console.log('JS load - isolatethread:', this.isolatethread.deref());
    let dataJson = this.libyamlscript.load_ys_to_json(
      this.isolatethread.deref(),
      input,
    );
    console.log('JS load - returned json buffer:', dataJson.toString());
    let resp = JSON.parse(dataJson);
    console.log('JS load - returned json value:', resp);

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
    let ret = this.libyamlscript.graal_tear_down_isolate(
      this.isolatethread.deref(),
    );
    if (ret !== 0) {
      throw new Error("Failed to tear down isolate.");
    }
  }
}

// Helper function to find the libyamlscript shared library path
function findLibyamlscriptPath() {
  let soExtension = os.platform() === 'linux' ? 'so' : 'dylib';
  let libyamlscriptName = `libyamlscript.${soExtension}.${yamlscriptVersion}`;
  console.log('JS - libyamlscriptName:', libyamlscriptName);
  let searchPaths = process.env.LD_LIBRARY_PATH
    ? process.env.LD_LIBRARY_PATH.split(':')
    : [];
  searchPaths.push('/usr/local/lib', path.join(os.homedir(), '.local', 'lib'));

  for (let p of searchPaths) {
    let fullPath = path.join(p, libyamlscriptName);
    if (fs.existsSync(fullPath)) {
      console.log('JS - libyamlscript full path:', fullPath);
      return fullPath;
    }
  }
  throw new Error(
`Shared library file '${libyamlscriptName}' not found
Try: curl -sSL yamlscript.org/install | VERSION=${yamlscriptVersion} LIB=1 bash
See: https://github.com/yaml/yamlscript/wiki/Installing-YAMLScript`);
}

module.exports = YAMLScript;
