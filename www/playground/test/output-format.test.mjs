import assert from "node:assert/strict";
import test from "node:test";

import {
  formatEvaluationOutput,
  limitOutputLines,
  OUTPUT_LINE_LIMIT,
  renderEvaluationStreams,
} from "../output-format.js";

test("JSON output uses the tightened two-space layout", () => {
  const input = JSON.stringify({
    server: {name: "api", host: "localhost", port: 8080},
    database: {name: "app", driver: "postgres", port: 5432},
  });
  assert.equal(
    formatEvaluationOutput(input, "json"),
    [
      '{ "server": {',
      '    "name": "api",',
      '    "host": "localhost",',
      '    "port": 8080 },',
      '  "database": {',
      '    "name": "app",',
      '    "driver": "postgres",',
      '    "port": 5432 }}',
    ].join("\n"),
  );
});

test("tightening preserves nested data and structural characters", () => {
  const value = {
    text: "literal { braces } and [ brackets ]",
    groups: [{items: [1, 2]}, {items: []}],
  };
  const formatted = formatEvaluationOutput(JSON.stringify(value), "json");
  assert.deepEqual(JSON.parse(formatted), value);
  assert.match(formatted, /literal \{ braces \} and \[ brackets \]/);
});

test("array object openings collapse onto adjacent structural lines", () => {
  const value = {
    containers: [{
      name: "orders",
      env: [
        {name: "LOG_LEVEL", value: "info"},
        {name: "REGION", value: "us-east-1"},
      ],
    }],
  };
  assert.equal(
    formatEvaluationOutput(JSON.stringify(value), "json"),
    [
      '{ "containers": [{',
      '    "name": "orders",',
      '    "env": [{',
      '      "name": "LOG_LEVEL",',
      '      "value": "info" }, {',
      '      "name": "REGION",',
      '      "value": "us-east-1" }]}]}',
    ].join("\n"),
  );
});

test("arrays of objects do not add an indentation level", () => {
  const value = {
    services: [{
      name: "orders-api",
      image: "ghcr.io/acme/orders:1.4",
      labels: {app: "orders", team: "platform"},
    }, {
      name: "orders-worker",
      image: "ghcr.io/acme/orders:1.4",
      labels: {app: "orders", team: "platform"},
    }],
  };
  const formatted = formatEvaluationOutput(JSON.stringify(value), "json");
  assert.equal(
    formatted,
    [
      '{ "services": [{',
      '    "name": "orders-api",',
      '    "image": "ghcr.io/acme/orders:1.4",',
      '    "labels": {',
      '      "app": "orders",',
      '      "team": "platform" }}, {',
      '    "name": "orders-worker",',
      '    "image": "ghcr.io/acme/orders:1.4",',
      '    "labels": {',
      '      "app": "orders",',
      '      "team": "platform" }}]}',
    ].join("\n"),
  );
  assert.deepEqual(JSON.parse(formatted), value);
});

test("scalar arrays keep their indentation", () => {
  const value = {ports: [80, 443]};
  assert.equal(
    formatEvaluationOutput(JSON.stringify(value), "json"),
    [
      '{ "ports": [',
      '    80,',
      '    443 ]}',
    ].join("\n"),
  );
});

test("JSON scalar output remains valid", () => {
  assert.equal(formatEvaluationOutput("42", "json"), "42");
  assert.equal(formatEvaluationOutput('"value"', "json"), '"value"');
  assert.equal(formatEvaluationOutput("null", "json"), "null");
});

test("empty and malformed JSON output is preserved", () => {
  assert.equal(formatEvaluationOutput("", "json"), "");
  assert.equal(formatEvaluationOutput("{broken", "json"), "{broken");
});

test("non-JSON output is preserved", () => {
  const yaml = "service:\n  name: orders\n";
  assert.equal(formatEvaluationOutput(yaml, "yaml"), yaml);
  assert.equal(formatEvaluationOutput("hello\n", "text"), "hello\n");
});

test("output through 1,000 lines is preserved", () => {
  const output = Array.from(
    {length: OUTPUT_LINE_LIMIT},
    (_, index) => `line ${index + 1}`,
  ).join("\n") + "\n";
  assert.equal(limitOutputLines(output), output);
});

test("long output ends with a truncation line", () => {
  const output = Array.from(
    {length: OUTPUT_LINE_LIMIT + 1},
    (_, index) => `line ${index + 1}`,
  ).join("\n");
  const limited = limitOutputLines(output).split("\n");
  assert.equal(limited.length, OUTPUT_LINE_LIMIT + 1);
  assert.equal(limited[0], "line 1");
  assert.equal(limited[OUTPUT_LINE_LIMIT - 1], "line 1000");
  assert.equal(
    limited[OUTPUT_LINE_LIMIT],
    "[Output truncated after 1000 lines]",
  );
});

test("evaluation streams retain the standard error range", () => {
  assert.deepEqual(
    renderEvaluationStreams([
      {fd: 1, text: "answer: 42\n"},
      {fd: 2, text: ">>>123<<<\noops\n"},
    ]),
    {
      text: "answer: 42\n>>>123<<<\noops\n",
      stderrRanges: [{from: 11, to: 26}],
    },
  );
});

test("evaluation streams preserve interleaved writes", () => {
  assert.deepEqual(
    renderEvaluationStreams([
      {fd: 1, text: "123\n"},
      {fd: 2, text: "456\n"},
      {fd: 1, text: "768\n"},
    ]),
    {
      text: "123\n456\n768\n",
      stderrRanges: [{from: 4, to: 8}],
    },
  );
});

test("truncated evaluation streams mark visible standard error", () => {
  assert.deepEqual(
    renderEvaluationStreams([
      {fd: 1, text: "answer\n"},
      {fd: 2, text: "oops\nmore\n"},
    ], 2),
    {
      text: "answer\noops\n[Output truncated after 2 lines]",
      stderrRanges: [{from: 7, to: 11}],
    },
  );
  assert.deepEqual(
    renderEvaluationStreams([
      {fd: 1, text: "one\ntwo\n"},
      {fd: 2, text: "oops\n"},
    ], 1).stderrRanges,
    [],
  );
});
