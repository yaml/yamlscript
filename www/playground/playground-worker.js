import {ENVIRONMENT_PATH, projectModules} from "./project.js";
import {installVirtualFileSystem} from "./virtual-fs.js";

const OUTPUT_LIMIT = 64 * 1024;
const MODULES = [
  "std",
  "clj",
  "csv",
  "fs",
  "json",
  "math",
  "set",
  "str",
  "walk",
  "yaml",
].join(",");

let stdout = "";
let stderr = "";
let streams = [];
let truncated = false;
let virtualFs = null;

function appendStream(fd, text) {
  if (!text) return;
  const previous = streams.at(-1);
  if (previous?.fd === fd) {
    previous.text += text;
  } else {
    streams.push({fd, text});
  }
}

function appendOutput(fd, buffer) {
  const decoder = fd === 1 ? appendOutput.stdout : appendOutput.stderr;
  const text = decoder.decode(buffer, {stream: true});
  const current = fd === 1 ? stdout : stderr;
  const room = OUTPUT_LIMIT - current.length;
  let accepted = text;
  if (room <= 0) {
    truncated = true;
    accepted = "";
  } else if (text.length > room) {
    truncated = true;
    accepted = text.slice(0, room);
  }
  if (fd === 1) {
    stdout += accepted;
  } else {
    stderr += accepted;
  }
  appendStream(fd, accepted);
  return buffer.length;
}

appendOutput.stdout = new TextDecoder();
appendOutput.stderr = new TextDecoder();

function resetOutput() {
  stdout = "";
  stderr = "";
  streams = [];
  truncated = false;
}

function errorMessage(error) {
  return error instanceof Error ? error.message : String(error);
}

async function initialize({runtimeUrl, supportUrl}) {
  importScripts(supportUrl);
  virtualFs = installVirtualFileSystem(globalThis.fs);
  globalThis.process.cwd = () => "/playground";
  globalThis.process.chdir = () => {
    const error = new Error("read-only file system");
    error.code = "EROFS";
    throw error;
  };
  const originalWrite = globalThis.fs.writeSync.bind(globalThis.fs);
  globalThis.fs.writeSync = (fd, buffer) =>
    fd === 1 || fd === 2
      ? appendOutput(fd, buffer)
      : originalWrite(fd, buffer);

  const response = await fetch(runtimeUrl);
  if (!response.ok) {
    throw new Error(`Unable to load YS runtime: ${response.status}`);
  }

  const go = new Go();
  go.argv = ["ys-playground"];
  go.env = {YS_MODULES: MODULES};
  const bytes = await response.arrayBuffer();
  const {instance} = await WebAssembly.instantiate(bytes, go.importObject);
  go.run(instance).catch((error) => {
    self.postMessage({type: "fatal", error: errorMessage(error)});
  });
  self.postMessage({type: "ready"});
}

function execute(request) {
  resetOutput();
  const started = performance.now();
  try {
    const {entry, files} = request.project;
    const runtimeFiles = {...files};
    const environment = runtimeFiles[ENVIRONMENT_PATH] || "";
    delete runtimeFiles[ENVIRONMENT_PATH];
    virtualFs.mount(runtimeFiles);
    const runtimeRequest = {
      ...request,
      file: `/playground/${entry}`,
      environment,
      projectModules: projectModules(runtimeFiles, entry),
      source: runtimeFiles[entry],
    };
    delete runtimeRequest.project;
    const response = globalThis.gloat.exports.run(
      JSON.stringify(runtimeRequest),
    );
    if (!response.ok) {
      throw new Error(response.error);
    }
    const result = JSON.parse(response.value);
    self.postMessage({
      type: "result",
      id: request.id,
      ok: true,
      stdout,
      stderr,
      streams,
      output: result.output,
      outputs: result.outputs,
      compiled: result.compiled,
      truncated,
      elapsedMs: Math.round(performance.now() - started),
    });
  } catch (error) {
    let phase = "runtime";
    try {
      const response = globalThis.gloat.exports["compile-source"](
        request.source,
      );
      if (!response.ok) phase = "compile";
    } catch (_) {
      phase = "compile";
    }
    self.postMessage({
      type: "result",
      id: request.id,
      ok: false,
      phase,
      stdout,
      stderr,
      streams,
      truncated,
      elapsedMs: Math.round(performance.now() - started),
      error: errorMessage(error),
    });
  }
}

self.onmessage = ({data}) => {
  if (data.type === "init") {
    initialize(data).catch((error) => {
      self.postMessage({type: "fatal", error: errorMessage(error)});
    });
  } else if (data.type === "run") {
    execute(data.request);
  }
};
