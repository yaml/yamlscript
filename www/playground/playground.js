import {
  defaultKeymap,
  history as historyExtension,
  historyKeymap,
} from "@codemirror/commands";
import {
  defaultHighlightStyle,
  HighlightStyle,
  indentUnit,
  StreamLanguage,
  syntaxHighlighting,
} from "@codemirror/language";
import {json} from "@codemirror/lang-json";
import {yaml} from "@codemirror/lang-yaml";
import {clojure} from "@codemirror/legacy-modes/mode/clojure";
import {properties} from "@codemirror/legacy-modes/mode/properties";
import {Compartment, EditorState} from "@codemirror/state";
import {
  Decoration,
  EditorView,
  keymap,
  lineNumbers,
} from "@codemirror/view";
import {tags} from "@lezer/highlight";

import {
  formatEvaluationOutput,
  renderEvaluationStreams,
} from "./output-format.js";
import {
  buildPlaygroundURL,
  parsePlaygroundState,
} from "./url-state.js";
import {
  normalizePreset,
  overlayProjectFiles,
  ENVIRONMENT_PATH,
  presetProject,
  PROJECT_LIMIT,
  projectSize,
} from "./project.js";

const DEFAULT_RUN_TIMEOUT = 5000;
const DEFAULT_EVAL_FORMAT = "yaml";
const AUTO_RUN_DELAY = 500;
const exampleCollator = new Intl.Collator("en", {numeric: true});

function insertIndentSpaces({state, dispatch}) {
  dispatch(state.update(
    state.replaceSelection(state.facet(indentUnit)),
    {scrollIntoView: true, userEvent: "input"},
  ));
  return true;
}

const clojureHighlight = HighlightStyle.define([
  {tag: tags.comment, color: "#84947f"},
  {tag: tags.keyword, color: "#c792ea"},
  {tag: [tags.atom, tags.bool, tags.null], color: "#ffcb6b"},
  {tag: [tags.string, tags.character], color: "#c3e88d"},
  {tag: tags.number, color: "#f78c6c"},
  {tag: tags.operator, color: "#89ddff"},
  {tag: tags.function(tags.variableName), color: "#82aaff"},
]);

const dataHighlight = HighlightStyle.define([
  {tag: tags.comment, color: "#84947f"},
  {tag: [tags.propertyName, tags.labelName], color: "#82aaff"},
  {tag: tags.string, color: "#c3e88d"},
  {tag: tags.number, color: "#f78c6c"},
  {tag: [tags.atom, tags.bool, tags.null], color: "#ffcb6b"},
  {tag: [tags.operator, tags.punctuation], color: "#89ddff"},
]);

function sourceLanguage(path) {
  if (path === ENVIRONMENT_PATH) {
    return StreamLanguage.define(properties);
  }
  if (/\.json$/i.test(path)) return json();
  if (/\.cljc?$/i.test(path)) return StreamLanguage.define(clojure);
  return yaml();
}

function sourceLanguageLabel(path) {
  if (path === ENVIRONMENT_PATH) return "Environment";
  if (/\.json$/i.test(path)) return "JSON";
  if (/\.cljc?$/i.test(path)) return "Clojure";
  if (/\.ya?ml$/i.test(path)) return "YAML";
  return "YAMLScript";
}

function createSourceState(source, path, onChange) {
  return EditorState.create({
    doc: source,
    extensions: [
      lineNumbers(),
      historyExtension(),
      indentUnit.of("  "),
      sourceLanguage(path),
      syntaxHighlighting(defaultHighlightStyle),
      EditorView.lineWrapping,
      keymap.of([
        {key: "Tab", run: insertIndentSpaces},
        ...defaultKeymap,
        ...historyKeymap,
      ]),
      EditorView.updateListener.of((update) => {
        if (update.docChanged) onChange(update.state);
      }),
      EditorView.theme({
        "&": {height: "100%", fontSize: "14px"},
        ".cm-content": {
          fontFamily: "var(--md-code-font-family)",
          minHeight: "18rem",
        },
        ".cm-scroller": {overflow: "auto"},
        "&.cm-focused": {
          outline: "2px solid var(--md-accent-fg-color)",
        },
      }),
    ],
  });
}

function createEditor(parent, state) {
  return new EditorView({parent, state});
}

function createCompiledViewer(parent) {
  return new EditorView({
    parent,
    state: EditorState.create({
      doc: "",
      extensions: [
        EditorState.readOnly.of(true),
        EditorView.editable.of(false),
        EditorView.lineWrapping,
        StreamLanguage.define(clojure),
        syntaxHighlighting(clojureHighlight),
      ],
    }),
  });
}

function createEvaluationViewer(parent) {
  const language = new Compartment();
  const standardError = new Compartment();
  const editor = new EditorView({
    parent,
    state: EditorState.create({
      doc: "",
      extensions: [
        EditorState.readOnly.of(true),
        EditorView.editable.of(false),
        EditorView.lineWrapping,
        language.of(yaml()),
        standardError.of([]),
        syntaxHighlighting(dataHighlight),
      ],
    }),
  });
  return {editor, language, standardError};
}

function replaceDocument(editor, value) {
  editor.dispatch({
    changes: {from: 0, to: editor.state.doc.length, insert: value},
  });
}

function replaceEvaluationDocument(viewer, value, stderrRanges = []) {
  replaceDocument(viewer.editor, value);
  const ranges = [];
  const lines = new Set();
  const document = viewer.editor.state.doc;
  for (const stderrRange of stderrRanges) {
    let position = stderrRange.from;
    while (position < stderrRange.to) {
      const line = document.lineAt(position);
      if (!lines.has(line.from)) {
        lines.add(line.from);
        ranges.push(
          Decoration.line({class: "cm-stderr"}).range(line.from),
        );
      }
      position = line.to + 1;
    }
  }
  viewer.editor.dispatch({
    effects: viewer.standardError.reconfigure(
      EditorView.decorations.of(Decoration.set(ranges)),
    ),
  });
}

function capturedStreams(result) {
  if (Array.isArray(result.streams)) return result.streams;
  return [
    {fd: 1, text: result.stdout || ""},
    {fd: 2, text: result.stderr || ""},
  ];
}

function parsePresets(root) {
  const element = root.querySelector("script[type='application/json']");
  return JSON.parse(element.textContent).map(normalizePreset);
}

function isPageReload() {
  const entries = globalThis.performance?.getEntriesByType?.("navigation") ||
    [];
  return entries[0]?.type === "reload" ||
    globalThis.performance?.navigation?.type === 1;
}

function initializePlayground(root) {
  const presets = parsePresets(root);
  const variant = root.dataset.variant || "home";
  const version = root.dataset.version;
  const modeTabs = [...root.querySelectorAll("[data-category]")];
  const resultTabs = [...root.querySelectorAll("[data-result-tab]")];
  const homeFormatTabs = [...root.querySelectorAll("[data-home-format]")];
  const exampleSelect = root.querySelector("[data-example]");
  const exampleField = exampleSelect.closest(".ys-playground__examples");
  const showcase = root.querySelector("[data-showcase]");
  const examplePrevious = root.querySelector("[data-example-previous]");
  const exampleNext = root.querySelector("[data-example-next]");
  const exampleTitle = root.querySelector("[data-example-title]");
  const exampleDescription = root.querySelector(
    "[data-example-description]",
  );
  const sourceLabel = root.querySelector("[data-source-label]");
  const sourceLink = root.querySelector("[data-source-link]");
  const fileTabs = root.querySelector("[data-file-tabs]");
  const sourcePanel = root.querySelector("[data-panel='source']");
  const argsRow = root.querySelector("[data-args-row]");
  const argsInput = root.querySelector("[data-args]");
  const evaluationOutput = root.querySelector("[data-output='evaluation']");
  const compiledOutput = root.querySelector("[data-output='compiled']");
  const status = root.querySelector("[data-status]");
  const runButton = root.querySelector("[data-run]");
  const autoRun = root.querySelector("[data-auto-run]");
  const timeoutSelect = root.querySelector("[data-timeout]");
  const helpOpen = root.querySelector("[data-help-open]");
  const helpDialog = root.querySelector("[data-help-dialog]");
  const configOpen = root.querySelector("[data-config-open]");
  const configDialog = root.querySelector("[data-config-dialog]");
  const factoryReset = root.querySelector("[data-factory-reset]");
  const shareButton = root.querySelector("[data-share]");
  const collapseButton = root.querySelector("[data-collapse]");
  const expandButton = root.querySelector("[data-expand]");
  const openPlayground = root.querySelector("[data-open-playground]");
  const collapsible = root.querySelector("[data-collapsible]");
  const runtimeUrl = root.dataset.runtimeUrl;
  const supportUrl = root.dataset.supportUrl;
  const workerUrl = root.dataset.workerUrl;
  const cycleMs = Number(root.dataset.cycleMs || 0);

  let selected = presets[0].id;
  const selectedByCategory = {};
  let activeResult = "evaluation";
  let evalFormat = DEFAULT_EVAL_FORMAT;
  let suppressChanges = false;
  let worker = null;
  let readyPromise = null;
  let resolveReady = null;
  let rejectReady = null;
  let requestId = 0;
  let activeRequest = 0;
  let timer = null;
  let autoTimer = null;
  let cycleTimer = null;
  let cycleAnimationTimer = null;
  let runButtonTimer = null;
  let shareTimer = null;
  let urlTimer = null;
  let busy = false;
  let hasRun = false;
  let latestResult = null;
  let loadingURLState = true;
  let observer = null;
  let cycleObserver = null;
  let cycleInView = false;
  let cycleStopped = false;
  let activeProject = presetProject(presets[0]);
  let originalProject = activeProject;
  let activeFile = activeProject.entry;
  let fileStates = new Map();

  const sourceEditor = createEditor(
    sourcePanel,
    createSourceState("", activeFile, handleSourceChange),
  );
  const evaluationViewer = createEvaluationViewer(evaluationOutput);
  const compiledViewer = createCompiledViewer(compiledOutput);

  function handleSourceChange(state) {
    fileStates.set(activeFile, state);
    if (!suppressChanges) {
      stopShowcaseCycle();
      resetRunButton();
      scheduleAutoRun();
      schedulePlaygroundURL();
    }
  }

  function preset() {
    return presets.find((item) => item.id === selected);
  }

  function currentProjectFiles() {
    fileStates.set(activeFile, sourceEditor.state);
    return Object.fromEntries(
      [...fileStates].map(([path, state]) => [path, state.doc.toString()]),
    );
  }

  function renderFileTabs() {
    const paths = Object.keys(activeProject.files);
    const multipleFiles = paths.length > 1;
    sourceLabel.hidden = multipleFiles;
    fileTabs.hidden = !multipleFiles;
    fileTabs.replaceChildren(...paths.map((path) => {
      const button = document.createElement("button");
      const selectedFile = path === activeFile;
      button.type = "button";
      button.role = "tab";
      button.textContent = path;
      button.title = path;
      button.dataset.file = path;
      button.setAttribute(
        "aria-selected",
        selectedFile ? "true" : "false",
      );
      button.tabIndex = selectedFile ? 0 : -1;
      button.addEventListener("click", () => selectProjectFile(path));
      button.addEventListener("keydown", handleFileTabKeydown);
      return button;
    }));
  }

  function handleFileTabKeydown(event) {
    if (!["ArrowLeft", "ArrowRight"].includes(event.key)) return;
    event.preventDefault();
    const tabs = [...fileTabs.querySelectorAll("button")];
    const index = tabs.indexOf(event.currentTarget);
    const offset = event.key === "ArrowRight" ? 1 : -1;
    const next = tabs[(index + offset + tabs.length) % tabs.length];
    next.focus();
    selectProjectFile(next.dataset.file);
  }

  function selectProjectFile(path) {
    if (path === activeFile || !fileStates.has(path)) return;
    fileStates.set(activeFile, sourceEditor.state);
    activeFile = path;
    sourceEditor.setState(fileStates.get(path));
    sourceLabel.textContent = sourceLanguageLabel(path);
    renderFileTabs();
    schedulePlaygroundURL();
  }

  function setProject(project, selectedFile = project.entry) {
    activeProject = {
      entry: project.entry,
      files: {...project.files},
    };
    activeFile = Object.hasOwn(project.files, selectedFile)
      ? selectedFile
      : project.entry;
    fileStates = new Map(
      Object.entries(project.files).map(([path, source]) => [
        path,
        createSourceState(source, path, handleSourceChange),
      ]),
    );
    sourceEditor.setState(fileStates.get(activeFile));
    sourceLabel.textContent = sourceLanguageLabel(activeFile);
    renderFileTabs();
  }

  function updatePlaygroundURL(replaceHistory = true) {
    if (loadingURLState) return;
    const item = preset();
    const files = currentProjectFiles();
    const args = argsInput.value;
    const state = {
      example: item.id,
      files,
      active: activeFile,
      entry: activeProject.entry,
      args,
      originalFiles: originalProject.files,
      originalArgs: item.args || "",
    };
    const currentURL = buildPlaygroundURL(
      location.href,
      location.pathname,
      state,
    );
    if (replaceHistory) {
      window.history.replaceState(null, "", currentURL);
    }
    if (openPlayground) {
      const targetURL = new URL(
        buildPlaygroundURL(currentURL, "/play/", state),
      );
      if (!targetURL.hash) targetURL.searchParams.set("e", item.id);
      openPlayground.href = targetURL.toString();
    }
  }

  function schedulePlaygroundURL() {
    clearTimeout(urlTimer);
    urlTimer = setTimeout(updatePlaygroundURL, 250);
  }

  function setShareStatus(label) {
    clearTimeout(shareTimer);
    shareButton.textContent = label;
    if (label !== "Share") {
      shareTimer = setTimeout(() => {
        shareButton.textContent = "Share";
      }, 2000);
    }
  }

  async function copyText(text) {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
      return;
    }
    const input = document.createElement("textarea");
    input.value = text;
    input.setAttribute("readonly", "");
    input.style.position = "fixed";
    input.style.opacity = "0";
    document.body.append(input);
    input.select();
    const copied = document.execCommand("copy");
    input.remove();
    if (!copied) throw new Error("Copy failed");
  }

  async function sharePlayground() {
    clearTimeout(urlTimer);
    updatePlaygroundURL();
    try {
      await copyText(window.location.href);
      setShareStatus("Copied");
    } catch (_) {
      setShareStatus("Error");
    }
  }

  function setCollapsed(collapsed) {
    root.dataset.collapsed = String(collapsed);
    collapsible.hidden = collapsed;
    if (collapseButton) {
      collapseButton.hidden = collapsed;
      collapseButton.setAttribute("aria-expanded", String(!collapsed));
    }
    if (expandButton) {
      expandButton.hidden = !collapsed;
      expandButton.setAttribute("aria-expanded", String(!collapsed));
    }
    try {
      localStorage.setItem(collapsedKey(), String(collapsed));
    } catch (_) {
      // Collapse still works when storage is unavailable.
    }
    if (collapsed) {
      clearTimeout(cycleTimer);
      cycleTimer = null;
    } else {
      scheduleShowcaseCycle();
    }
  }

  function collapsedKey() {
    return `ys-playground:${version}:collapsed`;
  }

  function loadCollapsed() {
    try {
      return localStorage.getItem(collapsedKey()) === "true";
    } catch (_) {
      return false;
    }
  }

  function autoRunKey() {
    return `ys-playground:${version}:auto-run`;
  }

  function loadAutoRun() {
    try {
      const value = localStorage.getItem(autoRunKey());
      return value === null ? true : value === "true";
    } catch (_) {
      return true;
    }
  }

  function saveAutoRun() {
    try {
      localStorage.setItem(autoRunKey(), String(autoRun.checked));
    } catch (_) {
      // Auto-run still works when storage is unavailable.
    }
  }

  function evalFormatKey() {
    return `ys-playground:${version}:eval-format`;
  }

  function selectedEvalFormat() {
    return evalFormat;
  }

  function loadEvalFormat() {
    try {
      const value = localStorage.getItem(evalFormatKey());
      return ["yaml", "json"].includes(value)
        ? value
        : DEFAULT_EVAL_FORMAT;
    } catch (_) {
      return DEFAULT_EVAL_FORMAT;
    }
  }

  function saveEvalFormat() {
    try {
      localStorage.setItem(evalFormatKey(), selectedEvalFormat());
    } catch (_) {
      // The selected evaluation format still works without storage.
    }
  }

  function timeoutKey() {
    return `ys-playground:${version}:timeout`;
  }

  function loadTimeout() {
    try {
      const value = localStorage.getItem(timeoutKey());
      const valid = [...timeoutSelect.options]
        .some((option) => option.value === value);
      return valid ? value : String(DEFAULT_RUN_TIMEOUT);
    } catch (_) {
      return String(DEFAULT_RUN_TIMEOUT);
    }
  }

  function saveTimeout() {
    try {
      localStorage.setItem(timeoutKey(), timeoutSelect.value);
    } catch (_) {
      // The selected timeout still works when storage is unavailable.
    }
  }

  function selectedExampleKey() {
    return `ys-playground:${version}:selected:${variant}`;
  }

  function loadSelectedExample() {
    try {
      return localStorage.getItem(selectedExampleKey());
    } catch (_) {
      return null;
    }
  }

  function saveSelectedExample(id) {
    try {
      localStorage.setItem(selectedExampleKey(), id);
    } catch (_) {
      // Selecting an example still works when storage is unavailable.
    }
  }

  function deleteCookies() {
    const expires = "Thu, 01 Jan 1970 00:00:00 GMT";
    for (const cookie of document.cookie.split(";")) {
      const name = cookie.split("=", 1)[0].trim();
      if (!name) continue;
      document.cookie = `${name}=; expires=${expires}; path=/`;
      document.cookie =
        `${name}=; expires=${expires}; path=/; domain=${location.hostname}`;
    }
  }

  function resetPlayground() {
    deleteCookies();
    try {
      for (const key of Object.keys(localStorage)) {
        if (key.startsWith("ys-playground:")) localStorage.removeItem(key);
      }
    } catch (_) {
      // Cookie deletion and the visible reset still apply.
    }
    clearTimeout(autoTimer);
    autoRun.checked = true;
    evalFormat = DEFAULT_EVAL_FORMAT;
    timeoutSelect.value = String(DEFAULT_RUN_TIMEOUT);
    configDialog.close();
    window.location.reload();
  }

  function setStatus(message, state = "ready") {
    status.textContent = message;
    root.dataset.state = state;
    if (!busy && runButton) {
      const label = state === "success"
        ? "√"
        : state === "error"
          ? "X"
          : state === "timeout" ? "Time" : "▶";
      setRunButton(label, state);
      clearTimeout(runButtonTimer);
      runButtonTimer = null;
      if (["success", "error", "timeout"].includes(state)) {
        runButtonTimer = setTimeout(() => {
          runButtonTimer = null;
          if (!busy && runButton.dataset.state === state) {
            setRunButton("▶", "ready");
          }
        }, 1500);
      }
    }
  }

  function setRunButton(label, state) {
    if (!runButton) return;
    runButton.textContent = label;
    runButton.dataset.state = state;
    runButton.setAttribute("aria-label", {
      "√": "Evaluation finished; evaluate again",
      X: "Evaluation failed; evaluate again",
      Time: "Evaluation timed out; evaluate again",
      "▶": "Evaluate YAMLScript",
      "...": "Evaluating YAMLScript",
    }[label]);
  }

  function setBusy(value) {
    clearTimeout(runButtonTimer);
    runButtonTimer = null;
    busy = value;
    if (runButton) runButton.disabled = value;
    setRunButton(
      value ? "..." : "▶",
      value ? "running" : "ready",
    );
  }

  function resetRunButton() {
    clearTimeout(runButtonTimer);
    runButtonTimer = null;
    if (busy) {
      stopWorker("Input changed");
    } else {
      setRunButton("▶", "ready");
    }
  }

  function renderResultTab(name) {
    activeResult = name;
    renderFullResultTabs();
    evaluationOutput.hidden = name !== "evaluation";
    compiledOutput.hidden = name !== "compiled";
  }

  function renderFullResultTabs() {
    if (!resultTabs.length) return;
    const isCode = preset().category === "code";
    const format = selectedEvalFormat();
    resultTabs.forEach((tab) => {
      const view = tab.dataset.resultFormat;
      const active = activeResult === "compiled"
        ? view === "glj"
        : isCode ? view === format : view === "yaml";
      tab.textContent = !isCode && view === "yaml"
        ? "OUT"
        : view.toUpperCase();
      const tooltip = !isCode && view === "yaml"
        ? "Show program output"
        : view === "glj"
          ? "Show compiled Glojure"
          : `Show ${view.toUpperCase()} output`;
      tab.dataset.tooltip = tooltip;
      tab.setAttribute("aria-label", tooltip);
      tab.hidden = !isCode && view === "json";
      tab.setAttribute("aria-selected", active ? "true" : "false");
      tab.tabIndex = active ? 0 : -1;
    });
  }

  function setEvaluationLanguage(format) {
    const extension = format === "json"
      ? json()
      : format === "yaml" ? yaml() : [];
    evaluationViewer.editor.dispatch({
      effects: evaluationViewer.language.reconfigure(extension),
    });
  }

  function renderHomeFormatTabs() {
    if (!homeFormatTabs.length) return;
    const isCode = preset().category === "code";
    const format = selectedEvalFormat();
    homeFormatTabs.forEach((tab) => {
      const yaml = tab.dataset.homeFormat === "yaml";
      const active = isCode ? tab.dataset.homeFormat === format : yaml;
      tab.textContent = isCode
        ? tab.dataset.homeFormat.toUpperCase()
        : "OUT";
      tab.hidden = !isCode && !yaml;
      tab.setAttribute("aria-selected", active ? "true" : "false");
      tab.tabIndex = active ? 0 : -1;
    });
  }

  function renderEvaluationOutput() {
    if (!latestResult) return;
    const output = latestResult.outputs?.[selectedEvalFormat()] ??
      latestResult.output;
    const streams = capturedStreams(latestResult);
    const hasStdout = streams.some(({fd, text}) => fd === 1 && text);
    const format = preset().category === "code" && !hasStdout
      ? selectedEvalFormat()
      : null;
    const formattedOutput = formatEvaluationOutput(output, format);
    const displayedStreams = hasStdout
      ? streams
      : [{fd: 1, text: formattedOutput}, ...streams];
    setEvaluationLanguage(format);
    const rendered = renderEvaluationStreams(displayedStreams);
    replaceEvaluationDocument(
      evaluationViewer,
      rendered.text,
      rendered.stderrRanges,
    );
  }

  function showcasePresets() {
    const category = preset().category;
    return presets.filter((item) =>
      item.category === category && item.listed !== false
    );
  }

  function clearShowcaseAnimation() {
    clearTimeout(cycleAnimationTimer);
    cycleAnimationTimer = null;
    exampleNext?.classList.remove("is-auto-advancing");
  }

  function animateShowcaseAdvance() {
    const reduceMotion = globalThis.matchMedia?.(
      "(prefers-reduced-motion: reduce)",
    ).matches;
    if (!exampleNext || reduceMotion) {
      moveShowcase(1, true);
      return;
    }
    clearShowcaseAnimation();
    exampleNext.classList.add("is-auto-advancing");
    cycleAnimationTimer = setTimeout(() => {
      cycleAnimationTimer = null;
      exampleNext.classList.remove("is-auto-advancing");
      moveShowcase(1, true);
    }, 160);
  }

  function stopShowcaseCycle() {
    cycleStopped = true;
    clearTimeout(cycleTimer);
    cycleTimer = null;
    clearShowcaseAnimation();
  }

  function showcaseEligible() {
    return cycleMs > 0 &&
      !cycleStopped &&
      cycleInView &&
      document.visibilityState !== "hidden" &&
      root.dataset.collapsed !== "true" &&
      !busy;
  }

  function scheduleShowcaseCycle() {
    clearTimeout(cycleTimer);
    cycleTimer = null;
    if (showcaseEligible()) {
      cycleTimer = setTimeout(() => {
        cycleTimer = null;
        animateShowcaseAdvance();
      }, cycleMs);
    }
  }

  function handleVisibilityChange() {
    if (document.visibilityState === "hidden") {
      clearTimeout(cycleTimer);
      cycleTimer = null;
      clearShowcaseAnimation();
    } else {
      scheduleShowcaseCycle();
    }
  }

  function moveShowcase(offset, automatic = false) {
    const examples = showcasePresets();
    if (!examples.length) return;
    const index = examples.findIndex((item) => item.id === selected);
    const next = examples[
      ((index < 0 ? 0 : index) + offset + examples.length) %
        examples.length
    ];
    if (!automatic) stopShowcaseCycle();
    renderPreset(next.id);
  }

  function renderShowcase(item) {
    const visible = Boolean(
      showcase && variant === "home",
    );
    if (showcase) showcase.hidden = !visible;
    exampleField.hidden = visible;
    if (exampleTitle) exampleTitle.textContent = item.label;
    if (exampleDescription) {
      exampleDescription.textContent = item.description;
    }
  }

  function renderPreset(id, run = true, replaceHistory = true) {
    selected = id;
    const item = preset();
    selectedByCategory[item.category] = id;
    saveSelectedExample(id);
    latestResult = null;
    suppressChanges = true;
    originalProject = presetProject(item);
    setProject(originalProject);
    argsInput.value = item.args || "";
    suppressChanges = false;
    if (!busy) {
      resetRunButton();
    }
    argsRow.hidden = variant === "home" || item.mode !== "run";
    renderShowcase(item);
    renderHomeFormatTabs();
    renderFullResultTabs();
    const format = item.category === "code" ? selectedEvalFormat() : null;
    const initialOutput = item.category === "program" || format === "yaml"
      ? item.expected
      : "";
    setEvaluationLanguage(format);
    replaceDocument(evaluationViewer.editor, initialOutput);
    replaceDocument(compiledViewer, "Run to see the compiled Clojure.");
    sourceLink.removeAttribute("href");
    sourceLink.hidden = !item.sourceUrl;
    if (item.sourceUrl) {
      sourceLink.href = item.sourceUrl;
    }
    exampleSelect.value = id;
    updatePlaygroundURL(replaceHistory);
    if (run && readyPromise) {
      runDemo();
    }
  }

  function renderCategory(category, requested, run = true) {
    const preferred = requested || selectedByCategory[category];
    const examples = presets.filter((item) =>
      item.category === category &&
      (item.listed !== false || item.id === preferred)
    );
    if (category === "program") {
      examples.sort((a, b) =>
        Number(Boolean(b.featured)) - Number(Boolean(a.featured)) ||
        exampleCollator.compare(a.label, b.label)
      );
    }
    exampleSelect.replaceChildren(...examples.map((item) => {
      const option = document.createElement("option");
      option.value = item.id;
      option.textContent = item.label;
      return option;
    }));
    modeTabs.forEach((tab) => {
      const active = tab.dataset.category === category;
      tab.setAttribute("aria-selected", active ? "true" : "false");
      tab.tabIndex = active ? 0 : -1;
    });
    const id = examples.some((item) => item.id === preferred)
      ? preferred
      : examples[0].id;
    renderPreset(id, run);
  }

  function displayResult(result) {
    if (!result.ok) {
      latestResult = null;
      const streams = [...capturedStreams(result)];
      const tail = streams.at(-1)?.text || "";
      const separator = tail && !tail.endsWith("\n") ? "\n" : "";
      streams.push({
        fd: 2,
        text: `${separator}Error: ${result.error}`,
      });
      const rendered = renderEvaluationStreams(streams);
      setEvaluationLanguage(null);
      replaceEvaluationDocument(
        evaluationViewer,
        rendered.text,
        rendered.stderrRanges,
      );
      replaceDocument(compiledViewer, rendered.text);
      const errorType = result.phase === "runtime"
        ? "Runtime error"
        : "Compilation error";
      setStatus(`${errorType} after ${result.elapsedMs} ms`, "error");
      scheduleShowcaseCycle();
      return;
    }
    latestResult = result;
    renderEvaluationOutput();
    replaceDocument(compiledViewer, result.compiled);
    const suffix = result.truncated ? ", output truncated" : "";
    setStatus(`Finished in ${result.elapsedMs} ms${suffix}`, "success");
    scheduleShowcaseCycle();
  }

  function stopWorker(message = "Stopped", state = "ready") {
    clearTimeout(timer);
    timer = null;
    rejectReady?.(new Error(message));
    resolveReady = null;
    rejectReady = null;
    if (worker) {
      worker.terminate();
    }
    worker = null;
    readyPromise = null;
    setBusy(false);
    setStatus(message, state);
  }

  function startWorker() {
    if (readyPromise) {
      return readyPromise;
    }
    setStatus("Loading YS WebAssembly...", "loading");
    worker = new Worker(workerUrl);
    readyPromise = new Promise((resolve, reject) => {
      resolveReady = resolve;
      rejectReady = reject;
    });
    worker.onmessage = ({data}) => {
      if (data.type === "ready") {
        setStatus(`YS ${version} is ready`);
        resolveReady();
        resolveReady = null;
        rejectReady = null;
      } else if (data.type === "fatal") {
        rejectReady(new Error(data.error));
        stopWorker(`Unable to start YS: ${data.error}`, "error");
      } else if (data.type === "result" && data.id === activeRequest) {
        clearTimeout(timer);
        timer = null;
        setBusy(false);
        displayResult(data);
      }
    };
    worker.onerror = (event) => {
      rejectReady(new Error(event.message));
      stopWorker(`Worker error: ${event.message}`, "error");
    };
    worker.postMessage({type: "init", runtimeUrl, supportUrl});
    return readyPromise;
  }

  async function runDemo() {
    hasRun = true;
    clearTimeout(cycleTimer);
    cycleTimer = null;
    if (busy) {
      stopWorker("Restarting YS...");
    }
    const item = preset();
    const files = currentProjectFiles();
    if (projectSize(files) > PROJECT_LIMIT) {
      setStatus("Project must be under 100 KiB", "error");
      return;
    }
    setBusy(true);
    setStatus("Running...", "running");
    try {
      await startWorker();
      const id = ++requestId;
      activeRequest = id;
      worker.postMessage({
        type: "run",
        request: {
          id,
          mode: item.mode,
          format: item.category === "code"
            ? selectedEvalFormat()
            : item.format,
          formats: item.category === "code"
            ? ["yaml", "json"]
            : undefined,
          project: {
            entry: activeProject.entry,
            files,
          },
          args: argsInput.value.trim().split(/\s+/).filter(Boolean),
          ignoreTerminalEffects: item.ignoreTerminalEffects || false,
        },
      });
      const runTimeout = Number(timeoutSelect.value);
      timer = setTimeout(() => {
        const seconds = runTimeout / 1000;
        const unit = seconds === 1 ? "second" : "seconds";
        stopWorker(`Stopped after ${seconds} ${unit}`, "timeout");
        setEvaluationLanguage(null);
        replaceDocument(
          evaluationViewer.editor,
          "Error: The program took too long to finish.",
        );
        scheduleShowcaseCycle();
      }, runTimeout);
    } catch (error) {
      if (worker) {
        stopWorker(`Unable to run YS: ${error.message}`, "error");
      }
    }
  }

  function scheduleAutoRun() {
    clearTimeout(autoTimer);
    if (variant === "home" || autoRun.checked) {
      autoTimer = setTimeout(runDemo, AUTO_RUN_DELAY);
    }
  }

  function activateCategory(category) {
    if (variant === "home") cycleStopped = false;
    renderCategory(category);
  }

  modeTabs.forEach((tab, index) => {
    tab.addEventListener("click", () => {
      activateCategory(tab.dataset.category);
    });
    tab.addEventListener("keydown", (event) => {
      if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") {
        return;
      }
      event.preventDefault();
      const offset = event.key === "ArrowRight" ? 1 : -1;
      const length = modeTabs.length;
      const next = modeTabs[(index + offset + length) % length];
      next.focus();
      activateCategory(next.dataset.category);
    });
  });

  resultTabs.forEach((tab, index) => {
    tab.addEventListener("click", () => {
      selectFullResultTab(tab);
    });
    tab.addEventListener("keydown", (event) => {
      if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") {
        return;
      }
      event.preventDefault();
      const offset = event.key === "ArrowRight" ? 1 : -1;
      const visibleTabs = resultTabs.filter((item) => !item.hidden);
      const visibleIndex = visibleTabs.indexOf(tab);
      const length = visibleTabs.length;
      const next = visibleTabs[(visibleIndex + offset + length) % length];
      next.focus();
      selectFullResultTab(next);
    });
  });

  function selectFullResultTab(tab) {
    const view = tab.dataset.resultFormat;
    if (view === "glj") {
      renderResultTab("compiled");
      return;
    }
    if (preset().category === "code") {
      evalFormat = view;
      saveEvalFormat();
      renderEvaluationOutput();
    }
    renderResultTab("evaluation");
  }

  homeFormatTabs.forEach((tab, index) => {
    tab.addEventListener("click", () => {
      if (preset().category !== "code") return;
      evalFormat = tab.dataset.homeFormat;
      saveEvalFormat();
      renderHomeFormatTabs();
      renderEvaluationOutput();
    });
    tab.addEventListener("keydown", (event) => {
      if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") return;
      event.preventDefault();
      const offset = event.key === "ArrowRight" ? 1 : -1;
      const length = homeFormatTabs.length;
      const next = homeFormatTabs[(index + offset + length) % length];
      next.focus();
      next.click();
    });
  });

  exampleSelect.addEventListener("change", () => {
    renderPreset(exampleSelect.value);
  });
  examplePrevious?.addEventListener("click", () => moveShowcase(-1));
  exampleNext?.addEventListener("click", () => moveShowcase(1));

  runButton?.addEventListener("click", runDemo);
  argsInput.addEventListener("input", () => {
    resetRunButton();
    scheduleAutoRun();
    schedulePlaygroundURL();
  });
  autoRun.addEventListener("change", () => {
    saveAutoRun();
    if (autoRun.checked) {
      scheduleAutoRun();
    } else {
      clearTimeout(autoTimer);
    }
  });
  timeoutSelect.addEventListener("change", saveTimeout);
  helpOpen.addEventListener("click", () => helpDialog.showModal());
  helpDialog.addEventListener("click", (event) => {
    if (event.target === helpDialog) helpDialog.close();
  });
  configOpen?.addEventListener("click", () => configDialog.showModal());
  configDialog.addEventListener("click", (event) => {
    if (event.target === configDialog) configDialog.close();
  });
  factoryReset.addEventListener("click", resetPlayground);
  shareButton?.addEventListener("click", () => void sharePlayground());
  collapseButton?.addEventListener("click", () => setCollapsed(true));
  expandButton?.addEventListener("click", () => setCollapsed(false));
  openPlayground?.addEventListener("click", updatePlaygroundURL);
  root.addEventListener("keydown", (event) => {
    if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
      event.preventDefault();
      runDemo();
    }
  });
  root.addEventListener("pointerdown", stopShowcaseCycle);
  root.addEventListener("focusin", stopShowcaseCycle);

  const parameters = new URL(location.href).searchParams;
  const requested =
    parameters.get("e") ||
    parameters.get("example") ||
    parameters.get("demo");
  const parsedState = parsePlaygroundState(location.hash);
  const savedExample = loadSelectedExample();
  cycleStopped = Boolean(
    requested ||
    parsedState.state.source !== undefined ||
    parsedState.state.files !== undefined ||
    parsedState.state.active !== undefined ||
    parsedState.state.args !== undefined,
  );
  const pageReload = isPageReload();
  evalFormat = loadEvalFormat();
  timeoutSelect.value = loadTimeout();
  const requestedPreset = presets.find((item) => item.id === requested) ||
    presets.find((item) => item.id === savedExample);
  const firstCategory = requestedPreset?.category || presets[0].category;
  const categoryPresets = presets.filter(
    (item) => item.category === firstCategory &&
      (item.listed !== false || item.id === requestedPreset?.id),
  );
  if (firstCategory === "program") {
    categoryPresets.sort(
      (a, b) => exampleCollator.compare(a.label, b.label),
    );
  }
  const current = categoryPresets.find(
    (item) => item.id === requestedPreset?.id,
  ) || categoryPresets[0];
  const first = current.id;
  autoRun.checked = loadAutoRun();
  if (variant === "full") {
    root.dataset.collapsed = "false";
    collapsible.hidden = false;
  } else {
    setCollapsed(loadCollapsed());
  }
  renderCategory(firstCategory, first, false);
  if (parsedState.ok && !pageReload) {
    suppressChanges = true;
    let files = originalProject.files;
    if (parsedState.state.files !== undefined) {
      try {
        files = overlayProjectFiles(files, parsedState.state.files);
      } catch (error) {
        setStatus(`Invalid shared project: ${error.message}`, "error");
      }
    } else if (parsedState.state.source !== undefined) {
      files = {
        ...files,
        [originalProject.entry]: parsedState.state.source,
      };
    }
    setProject(
      {entry: originalProject.entry, files},
      parsedState.state.active,
    );
    if (parsedState.state.args !== undefined) {
      argsInput.value = parsedState.state.args;
    }
    suppressChanges = false;
  }
  loadingURLState = false;
  updatePlaygroundURL();
  renderResultTab(activeResult);

  if (!("WebAssembly" in globalThis) || !("Worker" in globalThis)) {
    if (runButton) runButton.disabled = true;
    setStatus("This browser does not support the YS WebAssembly demo", "error");
    return () => {};
  }

  document.addEventListener("visibilitychange", handleVisibilityChange);
  observer = new IntersectionObserver((entries) => {
    if (entries.some((entry) => entry.isIntersecting)) {
      observer.disconnect();
      const warm = async () => {
        await startWorker();
        if (!hasRun) {
          runDemo();
        }
      };
      if ("requestIdleCallback" in globalThis) {
        requestIdleCallback(() => warm().catch(() => {}), {timeout: 1000});
      } else {
        setTimeout(() => warm().catch(() => {}), 0);
      }
    }
  });
  observer.observe(root);

  if (cycleMs > 0) {
    cycleObserver = new IntersectionObserver((entries) => {
      cycleInView = entries.some((entry) => entry.isIntersecting);
      if (cycleInView) {
        scheduleShowcaseCycle();
      } else {
        clearTimeout(cycleTimer);
        cycleTimer = null;
        clearShowcaseAnimation();
      }
    });
    cycleObserver.observe(root);
  }

  return () => {
    clearTimeout(timer);
    clearTimeout(autoTimer);
    clearTimeout(cycleTimer);
    clearTimeout(cycleAnimationTimer);
    clearTimeout(runButtonTimer);
    clearTimeout(shareTimer);
    clearTimeout(urlTimer);
    observer?.disconnect();
    cycleObserver?.disconnect();
    document.removeEventListener("visibilitychange", handleVisibilityChange);
    worker?.terminate();
    sourceEditor.destroy();
    evaluationViewer.editor.destroy();
    compiledViewer.destroy();
  };
}

let cleanupPlayground = () => {};

document$.subscribe(() => {
  cleanupPlayground();
  cleanupPlayground = () => {};
  const root = document.querySelector("[data-ys-playground]");
  if (root && !root.dataset.initialized) {
    root.dataset.initialized = "true";
    cleanupPlayground = initializePlayground(root);
  }
});
