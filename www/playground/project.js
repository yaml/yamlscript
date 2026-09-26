const DEFAULT_ENTRY = "main.ys";
export const ENVIRONMENT_PATH = "Environment";
export const PROJECT_LIMIT = 100 * 1024;

function text(value) {
  if (Array.isArray(value)) return value.join("\n");
  return String(value ?? "");
}

export function validateProjectPath(path) {
  if (typeof path !== "string" || !path || path.startsWith("/")) {
    throw new Error(`Invalid project path: ${path}`);
  }
  const parts = path.split("/");
  if (parts.some((part) => !part || part === "." || part === "..")) {
    throw new Error(`Invalid project path: ${path}`);
  }
  if (path.includes("\\") || path.includes("\0")) {
    throw new Error(`Invalid project path: ${path}`);
  }
  return path;
}

export function normalizePreset(preset) {
  const item = {...preset};
  const hasEnvironment = Object.hasOwn(item, "environment");
  item.source = text(item.source);
  item.expected = text(item.expected);
  item.entry = validateProjectPath(item.entry || DEFAULT_ENTRY);
  item.files = Object.fromEntries(
    Object.entries(item.files || {}).map(([path, source]) => [
      validateProjectPath(path),
      text(source),
    ]),
  );
  if (hasEnvironment) {
    if (Object.hasOwn(item.files, ENVIRONMENT_PATH)) {
      throw new Error("Companion files contain Environment");
    }
    item.files[ENVIRONMENT_PATH] = text(item.environment);
  }
  delete item.environment;
  if (Object.hasOwn(item.files, item.entry)) {
    throw new Error(`Companion files contain entry path: ${item.entry}`);
  }
  if (projectSize(presetProject(item).files) > PROJECT_LIMIT) {
    throw new Error("Project must be under 100 KiB");
  }
  return item;
}

export function presetProject(preset) {
  return {
    entry: preset.entry,
    files: {
      [preset.entry]: preset.source,
      ...preset.files,
    },
  };
}

export function projectSize(files) {
  const encoder = new TextEncoder();
  return Object.values(files).reduce(
    (total, source) => total + encoder.encode(source).length,
    0,
  );
}

export function projectModules(files, entry) {
  const modules = new Set();
  for (const path of Object.keys(files)) {
    if (path === entry || !/\.(?:ys|clj|cljc)$/.test(path)) continue;
    const module = path.replace(/\.(?:ys|clj|cljc)$/, "")
      .replaceAll("/", ".");
    if (!/^[A-Za-z_][A-Za-z0-9_.-]*$/.test(module)) continue;
    modules.add(module);
    modules.add(module.replaceAll("_", "-"));
  }
  return [...modules];
}

export function changedProjectFiles(files, originalFiles) {
  return Object.fromEntries(
    Object.entries(files).filter(
      ([path, source]) => originalFiles[path] !== source,
    ),
  );
}

export function overlayProjectFiles(originalFiles, changes) {
  const files = {...originalFiles};
  for (const [path, source] of Object.entries(changes || {})) {
    if (!Object.hasOwn(files, path)) {
      throw new Error(`Unknown project file: ${path}`);
    }
    files[path] = text(source);
  }
  if (projectSize(files) > PROJECT_LIMIT) {
    throw new Error("Project must be under 100 KiB");
  }
  return files;
}
