import assert from "node:assert/strict";
import fs from "node:fs";
import test from "node:test";

const normalizeNewlines = (text) => text.replace(/\r\n?/g, "\n");
const readText = (path) => normalizeNewlines(fs.readFileSync(path, "utf8"));

const claudeSetup = readText("src/ai/claude.md");
const codexSetup = readText("src/ai/codex.md");
const homepage = readText("src/index.md");
const playPage = readText("src/play.md");
const makefile = readText("Makefile");
const rootMakefile = readText("../Makefile");
const meta = readText("../Meta");
const mdys = readText("src/mdys.ys");
const mkdocs = readText("mkdocs.ys");
const playgroundJs = readText("playground/playground.js");
const sitecustomize = readText("python/sitecustomize.py");
const themeCss = readText("src/css/theme.css");
const worker = readText("playground/playground-worker.js");

const rosettaIds = {
  "100-doors": "hundred-doors",
  "99-bottles-of-beer": "ninety-nine-bottles",
};

const disabledPrograms = new Set([
  "average-loop-length",
  "xiaolin-wus-line-algorithm",
]);

test("text fixtures normalize native line endings", () => {
  assert.equal(normalizeNewlines("one\r\ntwo\rthree\n"), "one\ntwo\nthree\n");
});

function playground(page) {
  return page
    .split("data-ys-playground", 2)[1]
    .split("</script>", 1)[0];
}

function presets(page) {
  const section = playground(page);
  const json = section
    .split('<script type="application/json">', 2)[1]
    .split("</script>", 1)[0];
  return JSON.parse(json);
}

test("homepage carries curated code and program examples", () => {
  const demos = presets(homepage);
  const sampleFiles = fs.readdirSync("../sample/rosetta-code")
    .filter((file) => file.endsWith(".ys"))
    .sort();
  const programs = demos.filter(({category}) => category === "program");
  const code = demos.filter(({category}) => category === "code");
  assert.equal(new Set(demos.map(({id}) => id)).size, demos.length);
  assert.equal(code.length, 12);
  assert.deepEqual(
    code.map(({label}) => label),
    [
      "DRY Application Configuration",
      "Environment Overrides",
      "Generated Service Catalog",
      "Smart Configuration Function",
      "Generated Kubernetes Deployment",
      "HelmYS Service Account",
      "GitHub Actions Matrix",
      "Import a YAMLScript Module",
      "Load a YAMLScript File",
      "Import a Clojure Module",
      "Load a YAML Data File",
      "Load a JSON Data File",
    ],
  );
  for (const demo of code) {
    const source = demo.source.join("\n");
    assert.match(source, /(?:^|\n)!ys-0:\n/);
    assert.match(source, /(?:^|\n)# /);
    assert.ok(demo.description.length > 0);
    assert.equal(demo.listed, true);
  }
  const projects = new Map(
    code.filter(({files}) => Object.keys(files).length)
      .map((demo) => [demo.id, demo]),
  );
  assert.deepEqual([...projects.keys()], [
    "environment-overrides",
    "helmys-service-account",
    "import-yamlscript-module",
    "load-yamlscript-file",
    "import-clojure-module",
    "load-yaml-file",
    "load-json-file",
  ]);
  assert.deepEqual(
    [...projects.values()].map(({entry}) => entry),
    Array(7).fill("main.yaml"),
  );
  assert.deepEqual(
    [...projects.values()].map(({files}) => Object.keys(files)),
    [
      ["vars.yaml"],
      ["helm-init.ys"],
      ["service.ys"],
      ["settings.ys"],
      ["image.clj"],
      ["service.yaml"],
      ["release.json"],
    ],
  );
  const deployment = code.find(
    ({id}) => id === "generated-kubernetes-deployment",
  );
  assert.equal(
    deployment.environment,
    [
      "SHELL=/bin/bash",
      "USER=alice",
      "HOME=/home/alice",
      "LOG_LEVEL=debug",
      "",
    ].join("\n"),
  );
  const clojureModule = projects.get("import-clojure-module");
  assert.match(
    clojureModule.source.join("\n"),
    /image\/image-name\('orders' '1\.4'\)/,
  );
  assert.match(clojureModule.files["image.clj"], /\(defn image-name/);
  for (const demo of code) {
    assert.equal(demo.mode, "load");
    assert.equal(demo.format, "yaml");
    assert.equal("args" in demo, false);
  }
  assert.deepEqual(
    programs.map(({label}) => label),
    [
      "YAML to Pretty JSON",
      "99 Bottles of Beer",
      "Conway's Game of Life",
      "Dragon Curve",
      "Factorial",
      "Fibonacci Sequence",
      "FizzBuzz",
      "Floyd's Triangle",
      "ROT-13",
      "Sieve of Eratosthenes",
    ],
  );
  for (const demo of demos) {
    assert.ok(demo.source.length > 0);
    assert.ok(demo.expected.length > 0);
    assert.ok(["code", "program"].includes(demo.category));
  }
  const exampleSources = demos.flatMap((demo) => [
    demo.source.join("\n"),
    ...Object.values(demo.files || {}),
  ]).join("\n");
  assert.doesNotMatch(exampleSources, /ys::(?:json|yaml)/);
  for (const demo of programs) {
    assert.ok(demo.description.length > 0);
    assert.equal(demo.format, "text");
    assert.equal("args" in demo, false);
  }
  const featured = programs[0];
  assert.equal(featured.id, "yaml-to-pretty-json");
  assert.equal(featured.featured, true);
  assert.equal(featured.entry, "main.ys");
  assert.deepEqual(Object.keys(featured.files), [
    "file.yaml",
    "pretty-json.ys",
  ]);
  const featuredSource = featured.source.join("\n");
  assert.match(featuredSource, /^use: json yaml pretty-json$/m);
  assert.match(featured.files["pretty-json.ys"], /^use: json$/m);
  assert.match(featuredSource, /read\('file\.yaml'\):yaml\/load/);
  assert.match(featuredSource, /data:json\/dump/);
  assert.match(featuredSource, /text:pretty-json\/format/);
  assert.equal("sourceUrl" in featured, false);
  for (const demo of programs.slice(1)) {
    assert.match(demo.sourceUrl, /^https:\/\/rosettacode\.org\/wiki\//);
    assert.match(demo.sourceUrl, /#YAMLScript$/);
  }
  assert.ok(programs.length < sampleFiles.length);
});

test("homepage has the requested two-pane interface", () => {
  const demo = playground(homepage);
  assert.match(demo, />\s*Dynamic YAML\s*</);
  assert.match(demo, />\s*Program in YS\s*</);
  assert.match(demo, /data-example/);
  assert.match(demo, /data-cycle-ms="5000"/);
  assert.match(demo, /data-showcase hidden/);
  assert.match(
    demo,
    /data-example-previous aria-label="Previous example">&lt;<\/button>/,
  );
  assert.match(
    demo,
    /data-example-next aria-label="Next example">&gt;<\/button>/,
  );
  assert.match(demo, /data-example-title/);
  assert.match(demo, /data-example-description/);
  assert.match(
    playgroundJs,
    /argsRow\.hidden = variant === "home" \|\| item\.mode !== "run"/,
  );
  assert.match(demo, /data-auto-run checked>\s*Auto evaluate changes/);
  assert.doesNotMatch(demo, /data-reload-mode/);
  assert.doesNotMatch(demo, /Reload to:/);
  assert.match(demo, /data-help-open[\s\S]*>Help<\/button>/);
  assert.match(demo, /<dialog[^>]+data-help-dialog/);
  const tools = demo
    .split('class="ys-playground__tools">', 2)[1]
    .split("</div>", 1)[0];
  assert.match(tools, /data-help-open/);
  assert.doesNotMatch(tools, /data-share/);
  assert.doesNotMatch(tools, /data-config-open/);
  assert.match(
    demo,
    /For Dynamic YAML, select YAML or JSON output\./,
  );
  assert.match(
    demo,
    /Examples advance every five seconds until you interact\./,
  );
  assert.match(demo, /<dialog[^>]+data-config-dialog>/);
  assert.match(demo, /<select data-timeout>/);
  assert.match(demo, /<\/select>\s*Evaluation timeout/);
  assert.doesNotMatch(demo, /data-eval-format/);
  assert.doesNotMatch(demo, /Eval to:/);
  assert.match(demo, /value="5000" selected>5 seconds/);
  assert.match(demo, /data-factory-reset>Factory Reset<\/button>/);
  const resultTabs = demo
    .split('class="ys-playground__result-tabs"', 2)[1]
    .split("</div>", 1)[0];
  assert.match(resultTabs, /data-home-format="yaml"[\s\S]*YAML/);
  assert.match(resultTabs, /data-home-format="json"[\s\S]*JSON/);
  assert.doesNotMatch(resultTabs, /data-home-format="glj"/);
  assert.doesNotMatch(resultTabs, /Compilation/);
  assert.doesNotMatch(resultTabs, /data-result-tab/);
  const homeFormatEvents = playgroundJs
    .split("homeFormatTabs.forEach((tab, index)", 2)[1]
    .split('exampleSelect.addEventListener("change"', 1)[0];
  assert.match(homeFormatEvents, /renderEvaluationOutput\(\)/);
  assert.doesNotMatch(homeFormatEvents, /renderResultTab/);
  assert.doesNotMatch(homeFormatEvents, /scheduleAutoRun\(\)/);
  const introActions = demo
    .split("class=\"ys-playground__actions\">", 2)[1]
    .split("</div>", 1)[0];
  assert.doesNotMatch(introActions, /data-share/);
  assert.match(
    demo,
    /data-open-playground[\s\S]*ys-playground__full-prefix[\s\S]*Full/,
  );
  assert.match(themeCss, /ys-playground__full-prefix \{\s*display: none;/);
  assert.match(demo, /data-collapse[\s\S]*Collapse/);
  assert.match(demo, /data-expand[\s\S]*Expand/);
  assert.match(demo, /data-variant="home"/);
  assert.match(demo, /data-collapsed="true"/);
  assert.match(demo, /data-collapsible hidden/);
  assert.doesNotMatch(demo, /WebAssembly\s*<\/div>/);
  assert.doesNotMatch(demo, /data-reset/);
  assert.doesNotMatch(demo, /ys-playground__shortcut/);
  assert.match(
    demo,
    /data-example-next[\s\S]*class="ys-playground__run" data-run/,
  );
  assert.doesNotMatch(playgroundJs, /loadDraft|saveDraft|storageKey/);
  assert.match(playgroundJs, /exampleCollator\.compare\(a\.label, b\.label\)/);
  assert.match(playgroundJs, /variant === "home" \|\| autoRun\.checked/);
  assert.match(themeCss, /content: 'Click to evaluate the YS input now'/);
  assert.match(themeCss, /content: 'View program source'/);
  assert.match(themeCss, /content: 'Copy this demo URL to the clipboard'/);
  assert.match(themeCss, /content: 'Expand the demo panel'/);
  assert.match(themeCss, /content: 'Collapse the demo panel'/);
  assert.match(themeCss, /content: 'Open demo settings'/);
  assert.match(themeCss, /content: 'Open demo help'/);
  assert.match(themeCss, /\.ys-playground__showcase \{/);
  assert.match(themeCss, /content: attr\(aria-label\)/);
  assert.match(
    themeCss,
    /\.ys-playground__result-tabs button::after[\s\S]*bottom:/,
  );
  assert.match(
    themeCss,
    /\.ys-playground__result-tabs button:hover::after/,
  );
  assert.match(themeCss, /\.ys-playground__summary strong \{/);
  assert.match(playgroundJs, /const pageReload = isPageReload\(\)/);
  assert.match(playgroundJs, /renderPreset\(next\.id\);/);
  assert.match(playgroundJs, /const current = categoryPresets\.find\(/);
  assert.match(playgroundJs, /const first = current\.id/);
  assert.doesNotMatch(playgroundJs, /savedReloadMode|reloadModeKey/);
  assert.match(playgroundJs, /if \(parsedState\.ok && !pageReload\)/);
  assert.match(playgroundJs, /evalFormat = loadEvalFormat\(\)/);
  assert.match(playgroundJs, /selectedEvalFormat\(\)/);
  assert.match(playgroundJs, /renderEvaluationStreams\(/);
  assert.match(playgroundJs, /Decoration\.line\(\{class: "cm-stderr"\}\)/);
  assert.match(themeCss, /\.ys-playground__evaluation \.cm-stderr/);
  assert.match(playgroundJs, /item\.category === "code"/);
  assert.match(playgroundJs, /value === null \? true : value === "true"/);
  assert.match(playgroundJs, /timeoutSelect\.value = loadTimeout\(\)/);
  assert.match(
    playgroundJs,
    /ys-playground:\$\{version\}:selected:\$\{variant\}/,
  );
  assert.match(playgroundJs, /const savedExample = loadSelectedExample\(\)/);
  assert.match(playgroundJs, /saveSelectedExample\(id\)/);
  assert.doesNotMatch(
    playgroundJs,
    /cycleStopped = Boolean\(\s*requested \|\|\s*savedExample/,
  );
  assert.match(
    playgroundJs,
    /item\.id === requested\) \|\|\s+presets\.find\(.*savedExample/s,
  );
  assert.match(
    playgroundJs,
    /if \(!targetURL\.hash\) targetURL\.searchParams\.set\("e", item\.id\)/,
  );
  assert.match(playgroundJs, /configDialog\.showModal\(\)/);
  assert.match(playgroundJs, /helpDialog\.showModal\(\)/);
  assert.match(playgroundJs, /indentUnit\.of\("  "\)/);
  assert.match(
    playgroundJs,
    /state\.replaceSelection\(state\.facet\(indentUnit\)\)/,
  );
  assert.match(
    playgroundJs,
    /\{key: "Tab", run: insertIndentSpaces\}/,
  );
  assert.doesNotMatch(playgroundJs, /indentWithTab/);
  assert.match(playgroundJs, /document\.cookie =/);
  assert.match(playgroundJs, /key\.startsWith\("ys-playground:"\)/);
  assert.match(playgroundJs, /window\.location\.reload\(\)/);
  assert.match(playgroundJs, /syntaxHighlighting\(defaultHighlightStyle\)/);
  assert.match(
    playgroundJs,
    /item\.category === firstCategory/,
  );
  assert.match(playgroundJs, /setCollapsed\(true\)/);
  assert.match(playgroundJs, /setCollapsed\(false\)/);
  assert.match(playgroundJs, /setCollapsed\(loadCollapsed\(\)\)/);
  assert.match(playgroundJs, /schedulePlaygroundURL\(\)/);
  assert.match(playgroundJs, /buildPlaygroundURL\(/);
  assert.match(playgroundJs, /example: item\.id/);
  assert.match(playgroundJs, /parameters\.get\("e"\)/);
  assert.match(playgroundJs, /const cycleMs = Number\(/);
  assert.match(playgroundJs, /const category = preset\(\)\.category/);
  assert.match(playgroundJs, /item\.category === category/);
  assert.match(playgroundJs, /const selectedByCategory = \{\}/);
  assert.match(playgroundJs, /selectedByCategory\[item\.category\] = id/);
  assert.match(
    playgroundJs,
    /const preferred = requested \|\| selectedByCategory\[category\]/,
  );
  assert.match(playgroundJs, /showcase && variant === "home"/);
  assert.match(
    playgroundJs,
    /if \(variant === "home"\) cycleStopped = false;/,
  );
  assert.match(
    playgroundJs,
    /activateCategory\(tab\.dataset\.category\)/,
  );
  assert.match(playgroundJs, /moveShowcase\(1, true\)/);
  assert.match(playgroundJs, /animateShowcaseAdvance\(\)/);
  assert.match(playgroundJs, /classList\.add\("is-auto-advancing"\)/);
  assert.match(playgroundJs, /prefers-reduced-motion: reduce/);
  assert.match(themeCss, /\.ys-playground__nav\.is-auto-advancing/);
  assert.match(themeCss, /\.ys-playground__nav:hover/);
  assert.match(playgroundJs, /root\.addEventListener\("focusin"/);
  assert.match(playgroundJs, /root\.addEventListener\("pointerdown"/);
  assert.match(playgroundJs, /document\.visibilityState !== "hidden"/);
  assert.match(playgroundJs, /root\.dataset\.collapsed !== "true"/);
  assert.match(playgroundJs, /exampleField\.hidden = visible/);
  assert.match(playgroundJs, /navigator\.clipboard\?\.writeText/);
  assert.match(playgroundJs, /setShareStatus\("Copied"\)/);
  assert.match(playgroundJs, /setRunButton\("▶", "ready"\)/);
  assert.match(playgroundJs, /state === "success"\s*\? "√"/);
  assert.match(playgroundJs, /state === "error"\s*\? "X"/);
  assert.match(playgroundJs, /value \? "\.\.\." : "▶"/);
  assert.match(playgroundJs, /runButton\.disabled = value/);
  assert.doesNotMatch(playgroundJs, /"Stop"/);
  assert.match(playgroundJs, /state === "timeout" \? "Time"/);
  assert.match(playgroundJs, /}, 1500\)/);
  assert.match(playgroundJs, /result\.phase === "runtime"/);
  assert.match(playgroundJs, /\? "Runtime error"/);
  assert.match(worker, /exports\["compile-source"\]/);
  assert.match(worker, /let phase = "runtime"/);
  assert.match(worker, /phase = "compile"/);
  const errorResultHandling = playgroundJs
    .split("function displayResult(result)", 2)[1]
    .split("latestResult = result", 1)[0];
  assert.doesNotMatch(errorResultHandling, /renderResultTab/);
  assert.match(
    errorResultHandling,
    /compiledViewer, rendered\.text/,
  );
  const timeoutHandling = playgroundJs
    .split("timer = setTimeout", 2)[1]
    .split("}, runTimeout)", 1)[0];
  assert.doesNotMatch(timeoutHandling, /renderResultTab/);
  assert.match(demo, /data-output="evaluation"/);
  assert.match(demo, /data-output="compiled"/);
  assert.match(demo, /class="ys-playground__evaluation"/);
  assert.match(playgroundJs, /createEvaluationViewer\(evaluationOutput\)/);
  assert.match(playgroundJs, /format === "json"\s*\? json\(\)/);
  assert.match(playgroundJs, /format === "yaml" \? yaml\(\) : \[\]/);
  assert.match(playgroundJs, /syntaxHighlighting\(dataHighlight\)/);
  assert.match(
    playgroundJs,
    /item\.category === "program" \|\| format === "yaml"/,
  );
  assert.match(
    demo,
    /<h3>Evaluation<\/h3>\s*<span[^>]+data-status/,
  );
  assert.match(
    demo,
    /data-source-link\s+target="_blank" rel="noopener noreferrer" hidden>/,
  );
  assert.match(
    playgroundJs,
    /sourceLabel\.textContent = sourceLanguageLabel\(activeFile\)/,
  );
  assert.match(demo, /data-file-tabs/);
  assert.match(playgroundJs, /button\.dataset\.file = path/);
  assert.match(playgroundJs, /sourceLink\.hidden = !item\.sourceUrl/);
  assert.match(
    playgroundJs,
    /tab\.textContent = isCode[\s\S]*: "OUT"/,
  );
  assert.doesNotMatch(demo, /View on Rosetta Code/);
  assert.doesNotMatch(demo, /Transform YAML/);
  assert.doesNotMatch(demo, /Generate config/);
  assert.doesNotMatch(demo, /Write a program/);
  assert.doesNotMatch(demo, /data-panel="input"/);
});

test("dedicated playground keeps focused and handoff examples", () => {
  const demo = playground(playPage);
  const homePresets = presets(homepage);
  const fullPresets = presets(playPage);
  assert.equal(
    new Set(fullPresets.map(({id}) => id)).size,
    fullPresets.length,
  );
  for (const item of fullPresets) {
    assert.ok(item.description?.trim(), item.id);
  }
  const focused = fullPresets.filter((item) =>
    item.category === "code" && item.listed !== false
  );
  const handoffs = fullPresets.filter((item) => item.listed === false);
  assert.equal(focused.length, 22);
  assert.deepEqual(
    focused.map(({label}) => label),
    [
      "Using Variables",
      "String Interpolation",
      "Arithmetic Expressions",
      "Nested Data Lookups",
      "Conditional Values",
      "Optional Fields",
      "Generated Ranges",
      "Computed List Items",
      "List Splicing",
      "Code-Value Collections",
      "Chained Transformations",
      "Regular Expression Replacement",
      "Mapping Lists",
      "Filtering Lists",
      "Reducing Lists",
      "List Comprehensions",
      "Merging Defaults",
      "Destructuring Assignment",
      "Dotted Updates",
      "Helper Functions",
      "Kubernetes Deployment",
      "GitHub Actions Matrix",
    ],
  );
  assert.deepEqual(
    handoffs.map(({id}) => id),
    homePresets
      .filter(({category}) => category === "code")
      .map(({id}) => id),
  );
  const programs = fullPresets.filter(({category}) => category === "program");
  const sampleFiles = fs.readdirSync("../sample/rosetta-code")
    .filter((file) => file.endsWith(".ys"))
    .sort();
  const enabledFiles = sampleFiles.filter((file) =>
    !disabledPrograms.has(file.replace(/\.ys$/, ""))
  );
  assert.equal(programs.length, enabledFiles.length + 1);
  assert.equal(programs[0].id, "yaml-to-pretty-json");
  assert.match(mdys, /examples \.=: remove\(\\\(_\.disable\)\)/);
  assert.match(mdys, /examples :if home \.=: filter\(\\\(_\.home\)\)/);
  assert.match(mdys, /listed:: home \|\| not\(example\.home\)/);
  assert.match(mdys, /infos \.=: remove\(\\\(_\.disable\)\)/);
  assert.doesNotMatch(
    programs.map(({source}) => source.join("\n")).join("\n"),
    /\babs\s*\(/,
  );
  for (const file of enabledFiles) {
    const name = file.replace(/\.ys$/, "");
    const id = rosettaIds[name] || name;
    const program = programs.find((item) => item.id === id);
    const source = readText(`../sample/rosetta-code/${file}`).trimEnd();
    assert.ok(program, file);
    assert.equal(program.source.join("\n"), source, file);
  }
  assert.match(playPage, /hide:\n- navigation\n- toc/);
  assert.match(playPage, /<h1 class="empty"><\/h1>/);
  assert.match(demo, /data-variant="full"/);
  assert.match(demo, /data-collapsed="false"/);
  assert.match(demo, /data-collapsible>/);
  assert.match(demo, /data-example/);
  assert.match(
    demo,
    /data-example-previous[\s\S]*data-example-next[\s\S]*data-run/,
  );
  assert.match(demo, /data-help-open/);
  assert.match(demo, /data-share/);
  assert.match(demo, /data-config-open/);
  assert.match(
    themeCss,
    /data-variant='full'[^}]*ys-playground__intro > div:first-child/,
  );
  assert.match(
    themeCss,
    /data-variant='full'[^}]*ys-playground__actions \{[^}]*position: absolute/,
  );
  assert.match(
    themeCss,
    /data-variant='full'[^}]*\.md-main \{\s+overflow: hidden/,
  );
  assert.match(themeCss, /height: calc\(100% - 2rem\);\s+margin-top: 1rem/);
  assert.match(themeCss, /ys-playground__pane \{[^}]*min-height: 0/);
  const fullIntroActions = demo
    .split('class="ys-playground__intro">', 2)[1]
    .split("</div>\n\n  <div", 1)[0];
  assert.match(fullIntroActions, /data-help-open/);
  assert.match(fullIntroActions, /data-share/);
  assert.match(fullIntroActions, /data-config-open/);
  assert.match(
    demo,
    /data-example[^>]*><\/select>[\s\S]*data-example-description/,
  );
  assert.doesNotMatch(demo, /data-reload-mode/);
  assert.doesNotMatch(
    demo,
    /Reload page to go to next or random example\. See Settings\./,
  );
  assert.match(demo, /data-result-format="yaml"[\s\S]*YAML/);
  assert.match(demo, /data-result-format="json"[\s\S]*JSON/);
  assert.match(demo, /data-result-format="glj"[\s\S]*GLJ/);
  assert.match(demo, /data-tooltip="Show YAML output"/);
  assert.match(demo, /data-tooltip="Show JSON output"/);
  assert.match(demo, /data-tooltip="Show compiled Glojure"/);
  assert.doesNotMatch(demo, />\s*Compilation\s*<\/button>/);
  assert.match(demo, /For Dynamic YAML, select <strong>YAML<\/strong>/);
  assert.match(demo, /For programs, select <strong>OUT<\/strong>/);
  assert.match(
    playgroundJs,
    /tab\.hidden = !isCode && view === "json"/,
  );
  assert.match(playgroundJs, /!isCode && view === "yaml"\s*\? "OUT"/);
  assert.match(playgroundJs, /\? "Show program output"/);
  const resultTabEvents = playgroundJs
    .split("resultTabs.forEach((tab, index)", 2)[1]
    .split("homeFormatTabs.forEach((tab, index)", 1)[0];
  assert.match(resultTabEvents, /saveEvalFormat\(\)/);
  assert.match(resultTabEvents, /renderEvaluationOutput\(\)/);
  assert.doesNotMatch(resultTabEvents, /runDemo\(\)/);
  assert.doesNotMatch(resultTabEvents, /scheduleAutoRun\(\)/);
  assert.match(demo, /data-output="evaluation"/);
  assert.match(demo, /data-output="compiled"/);
  assert.doesNotMatch(demo, /data-open-playground/);
  assert.doesNotMatch(demo, /data-cycle-ms/);
  assert.doesNotMatch(demo, /data-showcase/);
  assert.doesNotMatch(demo, /\sdata-collapse(?:\s|>)/);
  assert.doesNotMatch(demo, /data-expand/);
  assert.match(
    themeCss,
    /data-variant='full'[^}]*height: 100%;[^}]*margin-bottom: 0;/,
  );
  assert.match(themeCss, /height: 100dvh;\s+overflow: hidden;/);
  assert.match(
    themeCss,
    /data-variant='full'[^}]*ys-playground__workbench[^}]*flex: 1;/,
  );
  assert.match(
    themeCss,
    /data-variant='full'\]\) \.md-footer \{\s+display: none;/,
  );
  assert.match(
    themeCss,
    /data-variant='full'\]\) \.md-tabs \{\s+display: none;/,
  );
  assert.match(themeCss, /h1\.empty \{\s+display: none;/);
  assert.match(
    themeCss,
    /data-variant='full'[\s\S]*ys-playground__run\[data-state='ready'\]/,
  );
  assert.match(themeCss, /ys-playground__run\[data-state='loading'\]/);
  assert.match(
    themeCss,
    /data-variant='full'\]\)\s+> \.md-content__button \{\s+display: none;/,
  );
});

test("homepage introduction and navigation are concise", () => {
  assert.match(
    homepage,
    /makes YAML\s+<span class="headline__fully">fully <\/span>dynamic/,
  );
  assert.doesNotMatch(homepage, /Learn More About YS/);
  assert.match(
    mkdocs,
    /nav:\n- About YS: about\.md\n- Using YS:[\s\S]*?\n- Playground: /,
  );
  assert.match(
    mkdocs,
    /- Playground: play\.md\n- Documentation:/,
  );
  assert.match(
    themeCss,
    /max-width: 540px[\s\S]*?\.headline small,[\s\S]*?\.headline__fully/,
  );
});

test("homepage leads with install, AI, and language bindings", () => {
  const playgroundAt = homepage.indexOf("data-ys-playground");
  const installAt = homepage.indexOf("Install YS instantly");
  const aiAt = homepage.indexOf("Write YAMLScript with AI");
  const bindingsAt = homepage.indexOf(
    "Use YS from 32 programming languages",
  );
  assert.ok(playgroundAt >= 0);
  assert.ok(playgroundAt < installAt);
  assert.ok(installAt < aiAt);
  assert.ok(aiAt < bindingsAt);
  assert.match(
    homepage,
    /Add code \(variables, functions, imports, transformations, interpolations/,
  );
  assert.doesNotMatch(
    themeCss,
    /\.ys-home-lede \{[^}]*max-width:/,
  );
  assert.match(
    homepage,
    new RegExp(
      "\\$ source &lt;\\(curl -sL in-1\\.cc\\) ys[\\s\\S]*" +
      "ys-home-terminal__success\">√</span> ys v0\\.3\\.1 installed" +
      "[\\s\\S]*" +
      "\\$ ys --version[\\s\\S]*" +
      "YS \\(YAMLScript\\) 0\\.3\\.1[\\s\\S]*" +
      "\\$ ys --help",
    ),
  );
  assert.match(homepage, /Compile YS everywhere/);
  assert.match(
    homepage,
    new RegExp(
      "Compile/cross-compile YS programs to standalone native binaries " +
      "and shared\\s+libraries for \\*\\*25\\+\\*\\* OS/Architecture " +
      "targets including WebAssembly\\.",
    ),
  );
  assert.match(
    homepage,
    /ys -e 'say: "Hello, world!"' -c -o hi\.wasm/,
  );
  assert.match(
    homepage,
    new RegExp(
      "ys-home-terminal__success\">√</span> Compiled to native binary: " +
      "'app' \\(1\\.4s\\)",
    ),
  );
  assert.match(
    homepage,
    /ys-home-terminal__success">√<\/span> Compiled to WASI Wasm:/,
  );
  assert.match(homepage, /'hi\.wasm' \(1\.8s\)/);
  assert.match(
    themeCss,
    /\.ys-home-install \.ys-home-terminal code span \{\s+color:/,
  );
  assert.match(
    themeCss,
    /\.ys-home-terminal__success \{\s+color: #2eaa46;/,
  );
  assert.match(homepage, /doc\/ys\.md#compiling-programs/);
  assert.match(
    themeCss,
    new RegExp(
      "\\.ys-home-install__grid \\{[\\s\\S]*?" +
      "grid-template-columns: repeat\\(2, minmax\\(0, 1fr\\)\\);",
    ),
  );
  assert.match(homepage, /### Claude Code/);
  assert.match(homepage, /### Codex/);
  assert.match(homepage, /\]\(ai\/claude\.md\)/);
  assert.match(homepage, /\]\(ai\/codex\.md\)/);
  assert.doesNotMatch(
    homepage,
    /github\.com\/yaml\/yamlscript\/tree\/v0\/ai\/(?:claude|codex)/,
  );
  assert.match(claudeSetup, /\/plugin marketplace add yaml\/yamlscript/);
  assert.match(claudeSetup, /\/plugin install ys-skill@yamlscript/);
  assert.match(codexSetup, /\$skill-installer Install the YAMLScript skill/);
  assert.match(codexSetup, /~\/\.agents\/skills/);
  const bindings = homepage.slice(bindingsAt);
  assert.equal(
    (bindings.match(/<div class="ys-home-binding"/g) || []).length,
    32,
  );
  assert.match(themeCss, /\.ys-home-bindings \{\s+display: flex;/);
  assert.match(themeCss, /flex-flow: row wrap/);
  const bindingAccents = themeCss.match(
    new RegExp(
      "\\.ys-home-binding:nth-child\\(\\d+\\) \\{\\s+" +
        "--ys-binding-accent: #[0-9a-f]{6};",
      "g",
    ),
  );
  assert.equal(bindingAccents?.length, 32);
  assert.match(
    themeCss,
    /--ys-binding-accent: #512bd4; \/\* C# and \.NET \*\//,
  );
  assert.match(
    themeCss,
    /--ys-binding-accent: #3776ab; \/\* Python \*\//,
  );
  assert.match(themeCss, /background: color-mix\(/);
  assert.match(themeCss, /border-top: 3px solid var\(--ys-binding-accent\)/);
  assert.match(
    themeCss,
    /\.ys-home-binding a \.external-link \{\s+display: none;/,
  );
  assert.match(bindings, /Ada/);
  assert.match(bindings, /Zig/);
  assert.doesNotMatch(homepage, /AI-Assisted YS/);
  assert.doesNotMatch(homepage, /Take Full Control of Your YAML/);
  assert.doesNotMatch(homepage, /Zero to YS in 10 seconds/);
});

test("playground has a solid background", () => {
  assert.match(themeCss, /\.ys-playground \{\s+background: #fff;/);
  assert.doesNotMatch(themeCss, /radial-gradient/);
  assert.match(
    themeCss,
    /\.ys-playground__intro \{\s+align-items: center;/,
  );
  assert.match(
    themeCss,
    /max-width: 540px[\s\S]*?\.ys-playground__intro em/,
  );
  assert.doesNotMatch(
    themeCss,
    /max-width: 540px[\s\S]*?\.ys-playground__intro \{[^}]*display: block/,
  );
  assert.match(
    themeCss,
    /max-width: 540px[\s\S]*?\.ys-playground__inputs \{[^}]*flex-basis: 100%/,
  );
  assert.match(
    themeCss,
    /max-width: 540px[\s\S]*?max-width: calc\(100vw - 2rem\)/,
  );
  assert.doesNotMatch(themeCss, /max-width: (?:499px|44\.984375em)/);
});

test("homepage runtime version matches project metadata", () => {
  const version = meta.match(/^version: (.+)$/m)[1];
  assert.match(homepage, new RegExp(`data-version="${version}"`));
});

test("worker capability list omits side-effect modules", () => {
  assert.match(worker, /^  "fs",$/m);
  for (const module of ["http", "io", "ipc", "pods", "process"]) {
    assert.doesNotMatch(worker, new RegExp(`^  "${module}",$`, "m"));
  }
});

test("playground assets do not load code from a CDN", () => {
  for (const file of [
    "playground/playground.js",
    "playground/playground-worker.js",
  ]) {
    const source = readText(file);
    assert.doesNotMatch(source, /https?:\/\//);
  }
});

test("website development server starts without noisy watchers", () => {
  assert.match(rootMakefile, /^serve: serve-www$/m);
  assert.match(makefile, /--debug-force-polling/);
  assert.match(makefile, /www\/playground\/dynamic-examples\.yaml/);
  assert.doesNotMatch(
    makefile,
    /www\/playground\/home-dynamic-examples\.yaml/,
  );
  assert.match(makefile, /www\/playground\/playground\.html/);
  assert.match(makefile, /www\/playground\/program-examples\.yaml/);
  assert.doesNotMatch(makefile, /featured-programs\.yaml/);
  assert.equal(
    fs.existsSync("playground/featured-programs.yaml"),
    false,
  );
  assert.match(mdys, /defn playground-programs/);
  assert.doesNotMatch(mdys, /playground-featured-programs/);
  assert.match(
    makefile,
    /watch_event_type =~ \^\(created\|deleted\|modified\|moved\)\$\$/,
  );
  assert.match(
    makefile,
    /PYTHONWARNINGS=ignore::DeprecationWarning/,
  );
  assert.match(makefile, /if \[\[ \$\$port == random \]\]/);
  assert.match(makefile, /\$\(PYTHON\) -c '\$\(RANDOM-PORT\)'/);
  assert.match(makefile, /--dev-addr=0\.0\.0\.0:\$\$port/);
  assert.match(sitecustomize, /mimetypes\.knownfiles = \[\]/);
});

test("root test includes website tests", () => {
  assert.match(rootMakefile, /TEST := \$\(DIRS:%=test-%\) test-www/);
});
