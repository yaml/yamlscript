import assert from "node:assert/strict";
import test from "node:test";

import {
  changedProjectFiles,
  ENVIRONMENT_PATH,
  normalizePreset,
  overlayProjectFiles,
  presetProject,
  projectModules,
  projectSize,
  validateProjectPath,
} from "../project.js";

test("presets normalize into projects", () => {
  const preset = normalizePreset({
    source: ["!ys-0:", "answer:: 42"],
    expected: ["answer: 42"],
    files: {"data/value.json": ["{\"answer\":42}"]},
  });
  assert.equal(preset.entry, "main.ys");
  assert.deepEqual(presetProject(preset), {
    entry: "main.ys",
    files: {
      "main.ys": "!ys-0:\nanswer:: 42",
      "data/value.json": "{\"answer\":42}",
    },
  });
});

test("preset environments become special project tabs", () => {
  const preset = normalizePreset({
    source: "!ys-0:\nvalue:: ENV.VALUE",
    environment: "VALUE=example\n",
  });
  assert.equal(preset.environment, undefined);
  assert.equal(
    presetProject(preset).files[ENVIRONMENT_PATH],
    "VALUE=example\n",
  );
  assert.throws(
    () => normalizePreset({
      source: "!ys-0:\n",
      environment: "VALUE=example\n",
      files: {[ENVIRONMENT_PATH]: "VALUE=other\n"},
    }),
    /Companion files contain Environment/,
  );
});

test("project paths stay relative and normalized", () => {
  assert.equal(validateProjectPath("lib/helpers.ys"), "lib/helpers.ys");
  for (const path of ["", "/main.ys", "../main.ys", "a//b", "a\\b"]) {
    assert.throws(() => validateProjectPath(path), /Invalid project path/);
  }
});

test("project changes overlay only known files", () => {
  const original = {"main.ys": "old", "data.yaml": "value: old"};
  const files = overlayProjectFiles(original, {"data.yaml": "value: new"});
  assert.deepEqual(files, {
    "main.ys": "old",
    "data.yaml": "value: new",
  });
  assert.deepEqual(changedProjectFiles(files, original), {
    "data.yaml": "value: new",
  });
  assert.throws(
    () => overlayProjectFiles(original, {"unknown.ys": "bad"}),
    /Unknown project file/,
  );
});

test("project size counts encoded file contents", () => {
  assert.equal(projectSize({"one.ys": "a", "two.json": "世"}), 4);
  assert.throws(
    () => normalizePreset({source: "x".repeat(100 * 1024 + 1)}),
    /Project must be under 100 KiB/,
  );
});

test("project modules come from companion source paths", () => {
  assert.deepEqual(projectModules({
    "main.ys": "",
    "lib/some_module.ys": "",
    "other.clj": "",
    "100-things.ys": "",
    "data.yaml": "",
  }, "main.ys"), [
    "lib.some_module",
    "lib.some-module",
    "other",
  ]);
});
