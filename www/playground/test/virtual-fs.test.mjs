import assert from "node:assert/strict";
import test from "node:test";

import {
  createVirtualFileSystem,
  installVirtualFileSystem,
} from "../virtual-fs.js";

const output = {
  writeSync(_fd, buffer) {
    return buffer.length;
  },
  write(_fd, buffer, _offset, length, _position, callback) {
    callback(null, Math.min(buffer.length, length));
  },
};

function call(fs, method, ...args) {
  return new Promise((resolve, reject) => {
    fs[method](...args, (error, ...values) => {
      if (error) reject(error);
      else resolve(values.length > 1 ? values : values[0]);
    });
  });
}

test("virtual filesystem preserves the runtime filesystem object", () => {
  const runtimeFs = {...output};
  const capturedFs = runtimeFs;
  const fs = installVirtualFileSystem(runtimeFs);
  assert.equal(fs, capturedFs);
  assert.equal(typeof capturedFs.mount, "function");
});

test("virtual filesystem reads project files", async () => {
  const fs = createVirtualFileSystem(output);
  fs.mount({
    "main.ys": "!ys-0\n",
    "data/value.json": "{\"answer\":42}",
  });
  const root = await call(fs, "stat", "/playground");
  assert.equal(root.isDirectory(), true);
  assert.deepEqual(
    (await call(fs, "readdir", "/playground")).sort(),
    ["data", "main.ys"],
  );
  const fd = await call(fs, "open", "/playground/data/value.json", 0, 0);
  const buffer = new Uint8Array(64);
  const count = await call(fs, "read", fd, buffer, 0, 64, null);
  assert.equal(new TextDecoder().decode(buffer.subarray(0, count)),
    "{\"answer\":42}");
  await call(fs, "close", fd);
});

test("virtual filesystem rejects missing files and writes", async () => {
  const fs = createVirtualFileSystem(output);
  fs.mount({"main.ys": "!ys-0\n"});
  await assert.rejects(
    call(fs, "stat", "/playground/missing.ys"),
    {code: "ENOENT"},
  );
  await assert.rejects(
    call(fs, "open", "/playground/main.ys", 1, 0),
    {code: "EROFS"},
  );
  await assert.rejects(
    call(fs, "stat", "/playground/../../secret"),
    {code: "ENOENT"},
  );
});
