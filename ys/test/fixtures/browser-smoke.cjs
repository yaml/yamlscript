const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const htmlPath = process.argv[2];
const html = fs.readFileSync(htmlPath, 'utf8');
const runtime = html.match(/<script>([\s\S]*?)<\/script>/)[1];
const wasmURL = html.match(/fetch\('([^']+)'\)/)[1];
const wasmPath = path.resolve(path.dirname(htmlPath), decodeURIComponent(wasmURL));
vm.runInThisContext(runtime);
const go = new Go();
WebAssembly.instantiate(fs.readFileSync(wasmPath), go.importObject)
  .then(({instance}) => {
    go.run(instance).catch(error => {
      console.error(error);
      process.exit(1);
    });
    assert.deepEqual(globalThis.gloat.exports.answer(), {ok: true, value: 42});
    process.exit(0);
  })
  .catch(error => {
    console.error(error);
    process.exit(1);
  });
setTimeout(() => process.exit(1), 30000);
