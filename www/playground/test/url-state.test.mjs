import assert from "node:assert/strict";
import test from "node:test";

import {
  buildPlaygroundURL,
  encodeContent,
  parsePlaygroundState,
  serializePlaygroundState,
} from "../url-state.js";

test("playground links carry selected and edited inputs", () => {
  const url = buildPlaygroundURL(
    "https://yamlscript.org/?example=old&demo=older",
    "/play/",
    {
      example: "using-variables",
      files: {
        "main.ys": "!ys-0:\nname:: 'new'\n",
        "data.yaml": "name: old\n",
      },
      active: "data.yaml",
      entry: "main.ys",
      args: "one two",
      originalFiles: {
        "main.ys": "!ys-0:\nname: old\n",
        "data.yaml": "name: old\n",
      },
    },
  );
  const parsed = new URL(url);
  assert.equal(parsed.pathname, "/play/");
  assert.equal(parsed.search, "?e=using-variables");
  assert.deepEqual(parsePlaygroundState(parsed.hash), {
    ok: true,
    state: {
      files: {"main.ys": "!ys-0:\nname:: 'new'\n"},
      active: "data.yaml",
      args: "one two",
    },
  });
});

test("playground links omit unchanged inputs", () => {
  const url = buildPlaygroundURL(
    "https://yamlscript.org/?e=old&example=older&demo=oldest",
    "/play/",
    {
      example: "using-variables",
      files: {"main.ys": "!ys-0:\n"},
      active: "main.ys",
      entry: "main.ys",
      args: "",
      originalFiles: {"main.ys": "!ys-0:\n"},
    },
  );
  assert.equal(url, "https://yamlscript.org/play/");
});

test("playground links remove the example when edits are reverted", () => {
  const url = buildPlaygroundURL(
    "https://yamlscript.org/?e=using-variables#v=1&z=old",
    "/",
    {
      example: "using-variables",
      files: {"main.ys": "!ys-0:\n"},
      active: "main.ys",
      entry: "main.ys",
      args: "",
      originalFiles: {"main.ys": "!ys-0:\n"},
    },
  );
  assert.equal(url, "https://yamlscript.org/");
});

test("playground URL state round trips edited projects", () => {
  const files = {
    "main.ys": "!ys-0\nsay: 'Hello, 世界'\n",
    "data.json": "{\"answer\":42}\n",
  };
  const hash = serializePlaygroundState({
    files,
    active: "data.json",
    entry: "main.ys",
    args: "one two",
  });
  assert.match(hash, /^#v=2&z=[A-Za-z0-9_-]+&a=one\+two$/);
  assert.deepEqual(parsePlaygroundState(hash), {
    ok: true,
    state: {files, active: "data.json", args: "one two"},
  });
});

test("playground URL state accepts version 1 source links", () => {
  const source = "!ys-0:\nanswer:: 42\n";
  const legacy = `#v=1&z=${encodeContent(source)}`;
  assert.deepEqual(parsePlaygroundState(legacy), {
    ok: true,
    state: {source},
  });
});

test("playground URL state omits unchanged inputs", () => {
  assert.equal(serializePlaygroundState(), "");
  assert.deepEqual(parsePlaygroundState(""), {ok: true, state: {}});
});

test("playground URL state rejects malformed content", () => {
  const parsed = parsePlaygroundState("#v=1&z=not-gzip");
  assert.equal(parsed.ok, false);
  assert.deepEqual(parsed.state, {});
});
