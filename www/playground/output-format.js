function tightenArrayIndentation(pretty) {
  const input = pretty.split("\n");
  const arrays = [];
  return input.map((line, index) => {
    const trimmed = line.trim();
    if (/^\](,)?$/.test(trimmed)) arrays.pop();
    const indent = line.length - line.trimStart().length;
    const reduction = arrays.filter(({compact}) => compact).length * 2;
    const output = `${" ".repeat(Math.max(0, indent - reduction))}${trimmed}`;
    if (trimmed === "[" || trimmed.endsWith(": [")) {
      arrays.push({compact: input[index + 1]?.trim() === "{"});
    }
    return output;
  });
}

function tightenJson(pretty) {
  const lines = [];
  for (const line of tightenArrayIndentation(pretty)) {
    const trimmed = line.trim();
    const opening = trimmed === "{" || trimmed === "[";
    const closing = line.match(/^\s*([}\]])(,?)$/);
    if (opening && lines.length) {
      const separator = trimmed === "{" && lines.at(-1).endsWith("[")
        ? ""
        : " ";
      lines[lines.length - 1] += `${separator}${trimmed}`;
    } else if (closing && lines.length) {
      const last = lines.length - 1;
      const separator = /[}\]]$/.test(lines[last]) ? "" : " ";
      lines[last] += `${separator}${closing[1]}${closing[2]}`;
    } else {
      lines.push(line);
    }
  }
  if (lines.length > 1 && (lines[0] === "{" || lines[0] === "[")) {
    lines[1] = `${lines[0]} ${lines[1].trimStart()}`;
    lines.shift();
  }
  return lines.join("\n");
}

export const OUTPUT_LINE_LIMIT = 1000;

export function limitOutputLines(output, limit = OUTPUT_LINE_LIMIT) {
  if (!output) return output;
  const lines = output.split(/\r?\n/);
  if (lines[lines.length - 1] === "" && output.endsWith("\n")) {
    lines.pop();
  }
  if (lines.length <= limit) return output;
  return [
    ...lines.slice(0, limit),
    `[Output truncated after ${limit} lines]`,
  ].join("\n");
}

export function renderEvaluationStreams(
  streams,
  limit = OUTPUT_LINE_LIMIT,
) {
  let combined = "";
  const ranges = [];
  for (const {fd, text = ""} of streams || []) {
    const from = combined.length;
    combined += text;
    if (fd !== 2 || !text) continue;
    const previous = ranges.at(-1);
    if (previous?.to === from) {
      previous.to = combined.length;
    } else {
      ranges.push({from, to: combined.length});
    }
  }
  const text = limitOutputLines(combined, limit);
  let contentEnd = text.length;
  if (text !== combined) {
    const marker = `\n[Output truncated after ${limit} lines]`;
    const markerStart = text.lastIndexOf(marker);
    if (markerStart >= 0) contentEnd = markerStart;
  }
  const stderrRanges = ranges
    .map(({from, to}) => ({
      from: Math.min(from, contentEnd),
      to: Math.min(to, contentEnd),
    }))
    .filter(({from, to}) => from < to);
  return {text, stderrRanges};
}

export function formatEvaluationOutput(output, format) {
  if (format !== "json" || !output) return output;
  try {
    return tightenJson(JSON.stringify(JSON.parse(output), null, 2));
  } catch (_) {
    return output;
  }
}
