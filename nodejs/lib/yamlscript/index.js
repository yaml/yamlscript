const yamlscriptVersion = '0.1.97';

const ffi = require('ffi-napi');
const ref = require('ref-napi');
const path = require('path');
const fs = require('fs');
const os = require('os');

function defineForeignFunctionInterface() {
  let libPath = findLibysPath();

  return ffi.Library(libPath, {
    'graal_create_isolate': ['int', ['pointer', 'pointer', 'pointer']],
    'graal_tear_down_isolate': ['int', ['pointer']],
    'load_ys_to_json': ['string', ['pointer', 'string']],
  });
}

class YAMLScript {
  constructor(config = {}) {
    this.libys = defineForeignFunctionInterface();
    this.isolatethread = ref.NULL_POINTER;

//    console.log(
//      "JS constructor - Isolate thread pointer before graal_create_isolate:",
//      this.isolatethread.deref());

    let rc = this.libys.graal_create_isolate(
      null,
      null,
      this.isolatethread,
    );

//    console.log('JS constructor - graal_isolate_create rc:', rc);

    if (rc !== 0) {
      throw new Error('Failed to create GraalVM isolate');
    }

//    console.log(
//      "JS constructor - Isolate thread pointer after graal_create_isolate:",
//      this.isolatethread.deref());
  }

  load(input) {
//    console.log('JS load - input:', input);
//    console.log('JS load - isolatethread:', this.isolatethread.deref());

    let dataJson = this.libys.load_ys_to_json(
      this.isolatethread.deref(),
      input,
    );

//    console.log('JS load - returned json buffer:', dataJson.toString());

    let resp = JSON.parse(dataJson);

//    console.log('JS load - returned json value:', resp);

    if (resp.error) {
      throw new Error(resp.error.cause);
    }

    if (!('data' in resp)) {
      throw new Error("Unexpected response from 'libys'");
    }

    return resp.data;
  }

  close() {
    let ret = this.libys.graal_tear_down_isolate(
      this.isolatethread.deref(),
    );

    if (ret !== 0) {
      throw new Error("Failed to tear down isolate.");
    }
  }
}

// Helper function to find the libys shared library path
function findLibysPath() {
  let soExtension = os.platform() === 'linux' ? 'so' : 'dylib';
  let libysName = `libys.${soExtension}.${yamlscriptVersion}`;

//  console.log('JS - libysName:', libysName);

  let searchPaths = process.env.LD_LIBRARY_PATH
    ? process.env.LD_LIBRARY_PATH.split(':')
    : [];
  searchPaths.push('/usr/local/lib', path.join(os.homedir(), '.local', 'lib'));

  for (let p of searchPaths) {
    let fullPath = path.join(p, libysName);
    if (fs.existsSync(fullPath)) {

//      console.log('JS - libys full path:', fullPath);

      return fullPath;
    }
  }

  throw new Error(
`Shared library file '${libysName}' not found
Try: curl https://yamlscript.org/install | VERSION=${yamlscriptVersion} LIB=1 bash
See: https://github.com/yaml/yamlscript/wiki/Installing-YAMLScript`);
}

module.exports = YAMLScript;
