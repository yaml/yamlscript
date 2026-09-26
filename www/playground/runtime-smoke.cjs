const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");

const [wasmPath, supportPath] = process.argv.slice(2);
vm.runInThisContext(fs.readFileSync(supportPath, "utf8"));

let stdout = "";
let stderr = "";
let streams = [];
let virtualFs = null;
let projectModules = null;
const originalWrite = globalThis.fs.writeSync.bind(globalThis.fs);
globalThis.fs.writeSync = (fd, buffer) => {
  const text = new TextDecoder().decode(buffer);
  const previous = streams.at(-1);
  if (previous?.fd === fd) {
    previous.text += text;
  } else if (fd === 1 || fd === 2) {
    streams.push({fd, text});
  }
  if (fd === 1) {
    stdout += text;
    return buffer.length;
  }
  if (fd === 2) {
    stderr += text;
    return buffer.length;
  }
  return originalWrite(fd, buffer);
};

function request(values) {
  stdout = "";
  stderr = "";
  streams = [];
  const project = values.project || {
    entry: "main.ys",
    files: {"main.ys": values.source},
  };
  const files = {...project.files};
  const environment = values.environment || files.Environment || "";
  delete files.Environment;
  virtualFs.mount(files);
  const runtimeValues = {
    ...values,
    environment,
    file: `/playground/${project.entry}`,
    projectModules: projectModules(files, project.entry),
    source: files[project.entry],
  };
  delete runtimeValues.project;
  const response = globalThis.gloat.exports.run(
    JSON.stringify(runtimeValues),
  );
  return {response, stdout, stderr, streams};
}

function playgroundPresets() {
  const page = fs.readFileSync("src/play.md", "utf8");
  const playground = page.split("data-ys-playground", 2)[1];
  const json = playground
    .split('<script type="application/json">', 2)[1]
    .split("</script>", 1)[0];
  return JSON.parse(json).map((preset) => {
    for (const key of ["source", "expected"]) {
      if (Array.isArray(preset[key])) {
        preset[key] = preset[key].join("\n");
      }
    }
    return preset;
  });
}

async function main() {
  const {installVirtualFileSystem} = await import("./virtual-fs.js");
  ({projectModules} = await import("./project.js"));
  virtualFs = installVirtualFileSystem(globalThis.fs);
  globalThis.process.cwd = () => "/playground";
  const go = new Go();
  go.argv = ["ys-playground-test"];
  go.env = {
    YS_MODULES: "std,clj,csv,fs,json,math,set,str,walk,yaml",
  };
  const bytes = fs.readFileSync(wasmPath);
  const {instance} = await WebAssembly.instantiate(bytes, go.importObject);
  go.run(instance).catch((error) => {
    console.error(error);
    process.exit(1);
  });

  let result = request({
    mode: "load",
    source: "!ys-0:\nanswer:: 42\n",
    args: [],
    format: "yaml",
    formats: ["yaml", "json"],
  });
  assert.equal(result.response.ok, true, result.response.error);
  let decoded = JSON.parse(result.response.value);
  assert.equal(decoded.output, "answer: 42\n");
  assert.deepEqual(decoded.outputs, {
    yaml: "answer: 42\n",
    json: '{"answer":42}',
  });
  assert.match(decoded.compiled, /answer/);

  result = request({
    mode: "load",
    source: [
      "!ys-0:",
      "::",
      "  a: 123 .>",
      "  warn: 'oops'",
      "value: done",
      "",
    ].join("\n"),
    args: [],
    format: "yaml",
  });
  assert.equal(result.response.ok, true, result.response.error);
  assert.equal(result.stderr, ">>>123<<<\noops\n");

  result = request({
    mode: "load",
    source: [
      "!ys-0:",
      "::",
      "  say: 123",
      "  warn: 456",
      "  say: 768",
      "value: done",
      "",
    ].join("\n"),
    args: [],
    format: "yaml",
  });
  assert.equal(result.response.ok, true, result.response.error);
  assert.deepEqual(result.streams, [
    {fd: 1, text: "123\n"},
    {fd: 2, text: "456\n"},
    {fd: 1, text: "768\n"},
  ]);

  result = request({
    mode: "load",
    source: "!ys-0:\nvalue:: ENV.VALUE\n",
    environment: "# Local values\nVALUE=example\n",
    args: [],
    format: "yaml",
  });
  assert.equal(result.response.ok, true, result.response.error);
  decoded = JSON.parse(result.response.value);
  assert.equal(decoded.output, "value: example\n");

  result = request({
    mode: "load",
    source: "!ys-0:\nvalue:: ENV.VALUE || 'missing'\n",
    environment: "",
    args: [],
    format: "yaml",
  });
  assert.equal(result.response.ok, true, result.response.error);
  decoded = JSON.parse(result.response.value);
  assert.equal(decoded.output, "value: missing\n");

  result = request({
    mode: "load",
    source: "!ys-0:\nvalue:: 1\n",
    environment: "not valid\n",
    args: [],
    format: "yaml",
  });
  assert.equal(result.response.ok, false);
  assert.match(
    result.response.error,
    /Invalid Environment entry on line 1/,
  );

  for (const [format, output] of [
    ["json", '{"answer":42}'],
    ["edn", '{"answer" 42}'],
  ]) {
    result = request({
      mode: "load",
      source: "!ys-0:\nanswer:: 42\n",
      args: [],
      format,
    });
    assert.equal(result.response.ok, true, result.response.error);
    decoded = JSON.parse(result.response.value);
    assert.equal(decoded.output, output, format);
  }

  result = request({
    mode: "query",
    input: "values: [1, 2, 3, 4]\n",
    source: "_.values.filter(\\(_ > 2))",
    args: [],
    format: "yaml",
  });
  assert.equal(result.response.ok, true, result.response.error);
  decoded = JSON.parse(result.response.value);
  assert.equal(decoded.output, "- 3\n- 4\n");
  assert.match(decoded.compiled, /filter/);

  result = request({
    mode: "run",
    source: [
      "!ys-0",
      "defn main(n=3):",
      "  loop a 0, b 1, xs []:",
      "    if xs.# < n:",
      "      recur: b, (a + b), xs.conj(a)",
      "      else: xs",
    ].join("\n"),
    args: ["4"],
    format: "yaml",
  });
  assert.equal(result.response.ok, true, result.response.error);
  decoded = JSON.parse(result.response.value);
  assert.equal(decoded.output, "- 0\n- 1\n- 1\n- 2\n");
  assert.match(decoded.compiled, /\(defn\s+main/);
  assert.equal(result.stdout, "");

  const missingFileSource = "!ys-0\nload: 'vars.yaml'\n";
  result = request({
    mode: "run",
    source: missingFileSource,
    args: [],
    format: "text",
  });
  assert.equal(result.response.ok, false);
  assert.match(result.response.error, /vars\.yaml/);
  const compilation = globalThis.gloat.exports["compile-source"](
    missingFileSource,
  );
  assert.equal(compilation.ok, true, compilation.error);
  assert.match(compilation.value, /vars\.yaml/);

  for (const module of [
    "http",
    "io",
    "ipc",
    "pods",
    "pprint",
    "process",
    "ys",
  ]) {
    result = request({
      mode: "load",
      source: `!ys-0\nuse: ${module}\n`,
      args: [],
      format: "text",
    });
    assert.equal(result.response.ok, false, module);
    assert.match(result.response.error, /disabled by YS_MODULES/, module);
  }

  result = request({
    mode: "run",
    project: {
      entry: "main.ys",
      files: {
        "main.ys": [
          "!ys-0",
          "use remote: :url 'https://example.com/remote.ys'",
        ].join("\n"),
        "remote.ys": "!ys-0\nns: remote\n",
      },
    },
    args: [],
    format: "text",
  });
  assert.equal(result.response.ok, false);
  assert.match(result.response.error, /disabled by YS_MODULES/);

  result = request({
    mode: "run",
    source: "!ys-0\nwrite 'created.txt': 'nope'\n",
    args: [],
    format: "text",
  });
  assert.equal(result.response.ok, false);
  assert.match(result.response.error, /[Rr]ead-only file system/);

  result = request({
    mode: "load",
    source: "!ys-0\nprivate-value =: 42\n",
    args: [],
    format: "text",
  });
  assert.equal(result.response.ok, true, result.response.error);
  result = request({
    mode: "load",
    source: "!ys-0\nprivate-value\n",
    args: [],
    format: "text",
  });
  assert.equal(result.response.ok, false);

  result = request({
    mode: "load",
    source: "!ys-0\nuse:\n  ys::json: :all\ndump: 42\n",
    args: [],
    format: "text",
  });
  assert.equal(result.response.ok, true, result.response.error);
  result = request({
    mode: "load",
    source: "!ys-0\ndump: 42\n",
    args: [],
    format: "text",
  });
  assert.equal(result.response.ok, false);

  const smokePrograms = new Set([
    "conways-game-of-life",
    "dragon-curve",
    "factorial",
    "fibonacci-sequence",
    "fizzbuzz",
    "floyds-triangle",
    "hundred-doors",
    "ninety-nine-bottles",
    "yaml-to-pretty-json",
    "yin-and-yang",
  ]);
  const smokePresets = playgroundPresets().filter((preset) =>
    preset.category === "code" || smokePrograms.has(preset.id));
  for (const preset of smokePresets) {
    result = request({
      mode: preset.mode,
      project: {
        entry: preset.entry,
        files: {
          [preset.entry]: preset.source,
          ...preset.files,
        },
      },
      input: preset.input || "",
      args: (preset.args || "").split(/\s+/).filter(Boolean),
      format: preset.format,
      environment: preset.environment,
      ignoreTerminalEffects: preset.ignoreTerminalEffects || false,
    });
    assert.doesNotMatch(result.stdout, /already refers to/, preset.id);
    assert.doesNotMatch(result.stderr, /already refers to/, preset.id);
    assert.equal(
      result.response.ok,
      true,
      `${preset.id}: ${result.response.error}`,
    );
    try {
      decoded = JSON.parse(result.response.value);
    } catch (error) {
      throw new Error(`${preset.id}: ${error.message}`);
    }
    assert.ok(decoded.compiled.length > 0, preset.id);
    if (preset.id === "ninety-nine-bottles") {
      assert.doesNotMatch(decoded.compiled, /\(defn\n/);
      assert.match(decoded.compiled, /\(declare bottles\)\n\n\(defn main\n/);
      assert.match(decoded.compiled, /\n\n\(defn bottles \[n\]\n/);
    }
    const actual = (result.stdout || decoded.output).trimEnd();
    if (preset.id === "yaml-to-pretty-json") {
      assert.equal(actual, preset.expected.trimEnd(), preset.id);
    } else if (preset.category === "program") {
      assert.ok(actual.length > 0, preset.id);
    } else {
      assert.equal(actual, preset.expected.trimEnd(), preset.id);
    }
  }

  process.exit(0);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});

setTimeout(() => process.exit(2), 30000);
