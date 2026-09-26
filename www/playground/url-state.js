import {gzipSync, gunzipSync, strFromU8, strToU8} from "fflate";

import {changedProjectFiles} from "./project.js";

const stateVersion = "2";

function bytesToBase64Url(bytes) {
  let binary = "";
  for (let offset = 0; offset < bytes.length; offset += 0x8000) {
    binary += String.fromCharCode(...bytes.subarray(offset, offset + 0x8000));
  }
  return btoa(binary)
    .replaceAll("+", "-")
    .replaceAll("/", "_")
    .replace(/=+$/, "");
}

function base64UrlToBytes(value) {
  if (!/^[A-Za-z0-9_-]*$/.test(value)) {
    throw new Error("invalid Base64URL content");
  }
  const base64 = value.replaceAll("-", "+").replaceAll("_", "/");
  const padded = base64 + "=".repeat((4 - base64.length % 4) % 4);
  const binary = atob(padded);
  return Uint8Array.from(binary, (character) => character.charCodeAt(0));
}

export function encodeContent(content) {
  return bytesToBase64Url(gzipSync(strToU8(content), {level: 9}));
}

export function decodeContent(encoded) {
  return strFromU8(gunzipSync(base64UrlToBytes(encoded)));
}

export function serializePlaygroundState({files, active, entry, args} = {}) {
  const project = {};
  if (files && Object.keys(files).length) project.files = files;
  if (active && active !== entry) project.active = active;
  const hasProject = Object.keys(project).length > 0;
  if (!hasProject && args === undefined) return "";
  const parameters = new URLSearchParams();
  parameters.set("v", stateVersion);
  if (hasProject) {
    parameters.set("z", encodeContent(JSON.stringify(project)));
  }
  if (args !== undefined) parameters.set("a", args);
  return `#${parameters}`;
}

export function buildPlaygroundURL(
  href,
  pathname,
  {
    example,
    files,
    active,
    entry,
    args,
    originalFiles,
    originalArgs = "",
  },
) {
  const url = new URL(href);
  const changedFiles = changedProjectFiles(files, originalFiles);
  const state = serializePlaygroundState({
    files: changedFiles,
    active,
    entry,
    args: args === originalArgs ? undefined : args,
  });
  url.pathname = pathname;
  url.searchParams.delete("e");
  url.searchParams.delete("example");
  url.searchParams.delete("demo");
  if (state) url.searchParams.set("e", example);
  url.hash = state;
  return url.toString();
}

export function parsePlaygroundState(hash) {
  if (!hash || hash === "#") return {ok: true, state: {}};
  try {
    const parameters = new URLSearchParams(hash.replace(/^#/, ""));
    const version = parameters.get("v");
    if (!["1", stateVersion].includes(version)) {
      throw new Error("unsupported playground URL version");
    }
    const state = {};
    const encoded = parameters.get("z");
    const args = parameters.get("a");
    if (encoded !== null && version === "1") {
      state.source = decodeContent(encoded);
    } else if (encoded !== null) {
      const project = JSON.parse(decodeContent(encoded));
      if (project.files !== undefined) state.files = project.files;
      if (project.active !== undefined) state.active = project.active;
    }
    if (args !== null) state.args = args;
    return {ok: true, state};
  } catch (error) {
    return {ok: false, error: error.message, state: {}};
  }
}
