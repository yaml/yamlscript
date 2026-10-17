#!/usr/bin/env python3
"""Compare two committed Glojure runtimes using fixed YAMLScript sources."""

import argparse
from contextlib import contextmanager
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shutil
import statistics
import subprocess
import sys
import tempfile
import time


SUITE_VERSION = 1
BUILD_VERSION = 1
BUILD_FLAGS = ["-trimpath", "-tags=glj_aot_runtime,glj_no_goimports",
               "-ldflags=-s -w"]


class SpeedError(Exception):
    pass


@contextmanager
def cache_lock(cache):
    cache.mkdir(parents=True, exist_ok=True)
    lock = cache / ".lock"
    try:
        lock.mkdir()
    except FileExistsError:
        raise SpeedError(f"Cache is in use: {lock}; after an interrupted run, "
                         "remove this directory once that process has stopped")
    try:
        yield
    finally:
        lock.rmdir()


def run(argv, *, cwd=None, env=None):
    result = subprocess.run(argv, cwd=cwd, env=env, capture_output=True,
                            text=True)
    if result.returncode:
        raise SpeedError(f"Command failed: {argv!r}\n{result.stderr.strip()}")
    return result.stdout.strip()


def sha256(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def commit(repo, revision):
    return run(["git", "-C", str(repo), "rev-parse", "--verify",
                "--end-of-options", f"{revision}^{{commit}}"])


def snapshot(repo, revision, cache):
    """Always isolate committed sources, including HEAD of a dirty checkout."""
    identity = commit(repo, revision)
    target = cache / identity
    if target.exists():
        if commit(target, "HEAD") != identity or run([
            "git", "-C", str(target), "status", "--porcelain",
            "--untracked-files=no",
        ]):
            raise SpeedError(f"Cached source was modified: {target}")
        return target, identity
    cache.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="snapshot-", dir=cache) as temp:
        source = Path(temp) / "source"
        run(["git", "clone", "--quiet", "--shared", "--no-checkout",
             str(repo), str(source)])
        run(["git", "-C", str(source), "-c", "advice.detachedHead=false",
             "checkout", "--quiet", "--detach", identity])
        source.rename(target)
    return target, identity


def share_dependencies(source, target, names):
    for name in names:
        existing, link = source / name, target / name
        if existing.exists() and not link.exists():
            link.parent.mkdir(parents=True, exist_ok=True)
            link.symlink_to(existing.resolve(), target_is_directory=True)


def related_source(ys, name, explicit):
    if explicit:
        candidate = Path(explicit).resolve()
    else:
        workspace = ys.parent.parent if ys.parent.name == "repos" else ys
        candidate = workspace / "repos" / name
    if not (candidate / ".git").exists():
        raise SpeedError(f"Missing {name} checkout: {candidate}")
    return candidate


def go_environment(gloat, env):
    rule = ("speed-diff-go-env: $(GO)\n"
            "\t@$(GO) env -json GOVERSION GOOS GOARCH GOROOT "
            "GOPATH GOMODCACHE GOCACHE GOEXPERIMENT GOAMD64 GOARM64 GOFLAGS\n")
    output = run(["make", "--quiet", "--no-print-directory", "-C", str(gloat),
                  "--eval", rule, "speed-diff-go-env"], env=env)
    # Makes may print dependency installation progress before the JSON.
    start = output.find("{\n")
    if start < 0:
        raise SpeedError("Could not read the Gloat Go environment")
    return json.loads(output[start:])


def build_runtime(source, revision, ys, ys_revision, gloat, gloat_revision,
                  oracle, go_env, cache, env, rebuild):
    identity = {
        "build_version": BUILD_VERSION,
        "glojure": revision, "yamlscript": ys_revision,
        "gloat": gloat_revision, "go": go_env["GOVERSION"],
        "os": go_env["GOOS"], "arch": go_env["GOARCH"],
        "go_features": {key: go_env.get(key, "") for key in
                        ("GOEXPERIMENT", "GOAMD64", "GOARM64")},
        "flags": BUILD_FLAGS, "bootstrap_sha256": sha256(oracle),
        "go_flags": go_env.get("GOFLAGS", env.get("GOFLAGS", "")),
    }
    key = hashlib.sha256(json.dumps(identity, sort_keys=True).encode()).hexdigest()
    directory = cache / "builds" / key
    directory.mkdir(parents=True, exist_ok=True)
    meta = directory / "build.json"
    version = re.search(r"(?m)^version:\s*(\S+)", (ys / "Meta").read_text())[1]
    suffix = ".exe" if go_env["GOOS"] == "windows" else ""
    binary = directory / f"ys-{version}{suffix}"
    if not rebuild and meta.exists() and binary.exists():
        try:
            saved = json.loads(meta.read_text())
            if saved["identity"] == identity and saved["sha256"] == sha256(binary):
                return binary, saved
        except (ValueError, KeyError):
            pass
    build_env = dict(env, GLOJURE_DIR=str(source), GLOAT_YS=str(oracle),
                     CGO_ENABLED="0")
    build_env.update({k: go_env[k] for k in ("GOPATH", "GOMODCACHE", "GOCACHE")})
    log = directory / "build.log"
    print(f"Building Glojure {revision[:12]} (log: {log})", flush=True)
    commands = [
        [str(gloat / "bin/gloat"), "--glj-build"],
        ["make", "-C", str(ys / "ys"), "-W", "gloat/src/yamlscript/cli.clj",
         "build", "YAMLSCRIPT_ENGINE=glojure", "GLOAT-ENGINE=glj",
         f"GLOAT-DIR={gloat}",
         f"CLI-BIN-GLOJURE={binary}"],
    ]
    with log.open("w") as stream:
        for command in commands:
            stream.write(json.dumps(command) + "\n")
            stream.flush()
            result = subprocess.run(command, cwd=ys, env=build_env,
                                    stdout=stream, stderr=subprocess.STDOUT)
            if result.returncode:
                raise SpeedError(f"Build failed for {revision[:12]}; see {log}")
    go = Path(go_env["GOROOT"]) / "bin" / ("go.exe" if suffix else "go")
    build_info = run([str(go), "version", "-m", str(binary)], env=build_env)
    if str(source) not in build_info:
        raise SpeedError(f"Built CLI does not reference {source}")
    saved = {"identity": identity, "path": str(binary),
             "sha256": sha256(binary), "build_info": build_info}
    temporary_meta = directory / "build.json.tmp"
    temporary_meta.write_text(json.dumps(saved, indent=2) + "\n")
    temporary_meta.replace(meta)
    return binary, saved


def suite(ys, directory):
    directory.mkdir(parents=True, exist_ok=True)

    def fixture(name, text):
        target = directory / name
        target.write_text(text)
        return str(target)

    small = fixture("small.yaml", "name: example\nvalues: [1, 2, 3]\n")
    wide = fixture("wide.yaml", "".join(f"key{i}: {i}\n" for i in range(1000)))
    deep_value = "leaf"
    for i in range(10):
        deep_value = {f"level{i}": [deep_value]}
    deep = fixture("deep.yaml", json.dumps(deep_value))
    stream = fixture("stream.yaml", "---\na: 1\n---\nb: [2, 3]\n---\nc: true\n")
    collections = fixture("collections.clj", """(clojure.core/let
 [xs (clojure.core/map clojure.core/inc (clojure.core/range 10000))]
 [(clojure.core/count xs)
  (clojure.core/reduce clojure.core/+ xs)
  (clojure.core/count (clojure.core/vec xs))])
""")
    strings = fixture("strings.clj", """(clojure.core/require 'clojure.string)
(clojure.core/count
 (clojure.string/split
  (clojure.string/join "," (clojure.core/repeat 10000 "alpha")) #","))
""")
    output = fixture("json-output.clj", """(clojure.core/vec
 (clojure.core/map
  (clojure.core/fn [n] [n (clojure.core/str "item-" n) (clojure.core/* n n)])
  (clojure.core/range 1000)))
""")
    release = str(ys / ".github/workflows/release.yaml")
    weird = str(ys / "sample/rosetta-code/weird-numbers.ys")
    return [
        {"name": "startup", "args": ["--version"]},
        {"name": "yaml-small", "args": ["-l", small]},
        {"name": "yaml-release", "args": ["-l", release]},
        {"name": "yaml-wide", "args": ["-l", wide]},
        {"name": "yaml-deep", "args": ["-l", deep]},
        {"name": "yaml-stream", "args": ["-s", "-l", stream]},
        {"name": "compile-data", "args": ["-c", release], "format": "clojure"},
        {"name": "compile-code", "args": ["-c", weird], "format": "clojure"},
        {"name": "execute-numeric", "args": [weird]},
        {"name": "execute-collections", "args": ["-C", "-l", collections]},
        {"name": "execute-strings", "args": ["-C", "-l", strings]},
        {"name": "json-output", "args": ["-C", "-l", output]},
    ]


# Ignore source formatting, preserving strings, character literals, and tokens.
CLOJURE_TOKEN = re.compile(
    r'"(?:\\.|[^"\\])*"|\\(?:[^\s()\[\]{}";,]+|.)|;[^\n]*|'
    r'[\s,]+|[()\[\]{}]|[^\s,()\[\]{}";]+', re.DOTALL)


def normalized_output(data, case):
    if case.get("format") != "clojure":
        return data
    text = data.decode()
    return tuple(token for token in CLOJURE_TOKEN.findall(text)
                 if not all(char.isspace() or char == "," for char in token)
                 and not token.startswith(";"))


def measure(binaries, cases, runs, warmups, env, timeout=60, rows=None):
    if rows is None:
        rows = []
    for case in cases:
        values = {name: [] for name in binaries}
        expected = None
        hashes = {}
        names = list(binaries)
        for iteration in range(warmups + runs):
            start = iteration % len(names)
            for name in names[start:] + names[:start]:
                command = [str(binaries[name]), *case["args"]]
                begin = time.perf_counter_ns()
                try:
                    result = subprocess.run(command, env=env,
                                            capture_output=True, timeout=timeout)
                except subprocess.TimeoutExpired:
                    raise SpeedError(f"{case['name']} timed out for {name} "
                                     f"after {timeout} seconds")
                elapsed = (time.perf_counter_ns() - begin) / 1e9
                if result.returncode:
                    raise SpeedError(f"{case['name']} failed for {name}: "
                                     + result.stderr.decode(errors="replace"))
                output = normalized_output(result.stdout, case)
                if expected is None:
                    expected = output
                elif output != expected:
                    raise SpeedError(f"Output differs for {case['name']}: {name}")
                hashes[name] = hashlib.sha256(result.stdout).hexdigest()
                if iteration >= warmups:
                    values[name].append(elapsed)
        medians = {name: statistics.median(v) for name, v in values.items()}
        row = {**case, "seconds": values, "medians": medians,
               "stdout_sha256": hashes,
               "speedup": medians["old"] / medians["new"],
               "old_over_graal": medians["old"] / medians["graalvm"],
               "new_over_graal": medians["new"] / medians["graalvm"]}
        rows.append(row)
        print(f"{case['name']:<21} {medians['old'] * 1000:9.2f} "
              f"{medians['new'] * 1000:9.2f} {medians['graalvm'] * 1000:9.2f} "
              f"{row['speedup']:8.3f}x {row['old_over_graal']:8.2f}x "
              f"{row['new_over_graal']:8.2f}x", flush=True)
    return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--old", default="HEAD^")
    parser.add_argument("--new", default="HEAD")
    parser.add_argument("--glojure-dir", default=os.getenv("GLOJURE_DIR"))
    parser.add_argument("--gloat-dir")
    parser.add_argument("--graalvm-bin", type=Path)
    parser.add_argument("--cache", type=Path)
    parser.add_argument("--cases", default="all")
    parser.add_argument("--runs", type=int, default=11)
    parser.add_argument("--warmups", type=int, default=2)
    parser.add_argument("--timeout", type=float, default=60)
    parser.add_argument("--rebuild", action="store_true")
    args = parser.parse_args()
    if args.runs < 1 or args.warmups < 0:
        parser.error("runs must be positive and warmups nonnegative")
    if not 0 < args.timeout < float("inf"):
        parser.error("timeout must be positive and finite")
    ys_live = Path(__file__).resolve().parent.parent
    cache = (args.cache or ys_live / ".cache/speed-diff").resolve()
    if args.cases == "list":
        for case in suite(ys_live, cache / "fixtures/list"):
            print(case["name"])
        return
    with cache_lock(cache):
        compare(args, ys_live, cache)


def compare(args, ys_live, cache):
    glj_live = related_source(ys_live, "glojure", args.glojure_dir)
    gloat_live = related_source(ys_live, "gloat", args.gloat_dir)
    # Resolve both revisions before installing dependencies or compiling.
    old_revision = commit(glj_live, args.old)
    new_revision = commit(glj_live, args.new)
    version = re.search(r"(?m)^version:\s*(\S+)",
                        run(["git", "-C", str(ys_live), "show", "HEAD:Meta"]))[1]
    oracle = (args.graalvm_bin or ys_live / f"ys/bin/ys-{version}-graalvm").resolve()
    if not oracle.is_file() or not os.access(oracle, os.X_OK):
        raise SpeedError(f"GraalVM executable not found: {oracle}; "
                         "set graalvm-bin=/path/to/ys-graalvm")
    oracle_version = run([str(oracle), "--version"])
    if not re.search(rf"(?<![\d.]){re.escape(version)}(?![\d.])", oracle_version):
        raise SpeedError(f"GraalVM version {oracle_version!r} does not match {version}")
    print(f"Glojure: old={old_revision[:12]} new={new_revision[:12]}")
    print(f"GraalVM: existing executable {oracle} ({oracle_version})")
    print("Source snapshots exclude all working-tree changes.", flush=True)
    ys, ys_revision = snapshot(ys_live, "HEAD", cache / "sources/yamlscript")
    gloat, gloat_revision = snapshot(gloat_live, "HEAD", cache / "sources/gloat")
    share_dependencies(ys_live, ys, [".cache/makes", ".cache/.local",
                                   ".cache/glojure-source", ".cache/glojure-parser"])
    share_dependencies(gloat_live, gloat, [".cache/makes", ".cache/local",
                                         ".cache/ys-v0-glj"])
    env = dict(os.environ)
    for key in ("GOROOT", "GOOS", "GOARCH", "GLOAT_PLATFORM", "GLOAT_ENGINE",
                "GLOJURE_DIR", "GLOAT_ROOT", "GLOAT_VERSION", "MAKEFLAGS",
                "MFLAGS", "MAKELEVEL", "YAMLSCRIPT_ROOT"):
        env.pop(key, None)
    for key in ("GOAMD64", "GOARM64", "GO386", "GOARM", "GOMIPS", "GOMIPS64",
                "GOPPC64", "GORISCV64", "GOWASM", "GOEXPERIMENT", "GO_REPLACE"):
        env.pop(key, None)
    env.update(CGO_ENABLED="0", GOWORK="off")
    go_env = go_environment(gloat, env)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
    output = cache / "runs" / stamp
    output.mkdir(parents=True)
    cases = suite(ys, output / "inputs")
    if args.cases != "all":
        selected = args.cases.split(",")
        unknown = set(selected) - {case["name"] for case in cases}
        if unknown:
            raise SpeedError(f"Unknown cases: {', '.join(sorted(unknown))}")
        cases = [case for case in cases if case["name"] in selected]
    copied_oracle = output / oracle.name
    shutil.copy2(oracle, copied_oracle)
    binaries, builds = {"graalvm": copied_oracle}, {}
    for name, revision in (("old", old_revision), ("new", new_revision)):
        source, _ = snapshot(glj_live, revision, cache / "sources/glojure")
        binary, info = build_runtime(source, revision, ys, ys_revision, gloat,
                                     gloat_revision, copied_oracle, go_env,
                                     cache, env, args.rebuild)
        binaries[name], builds[name] = binary, info
    binaries = {name: binaries[name] for name in ("old", "new", "graalvm")}
    print("\nOperation               Old ms    New ms  Graal ms  Speedup "
          "Old/Graal New/Graal", flush=True)
    report = {"suite_version": SUITE_VERSION, "old": old_revision,
              "new": new_revision, "yamlscript": ys_revision,
              "gloat": gloat_revision, "go": go_env, "builds": builds,
              "graalvm": {"path": str(oracle), "version": oracle_version,
                          "sha256": sha256(copied_oracle),
                          "provenance": "existing executable; not rebuilt"},
              "platform": platform.platform(), "runs": args.runs,
              "warmups": args.warmups, "timeout": args.timeout,
              "runtime_environment": {key: env[key] for key in
                  ("GOGC", "GOMEMLIMIT", "GOMAXPROCS", "GODEBUG", "LANG", "TZ")
                  if key in env}}
    report["inputs"] = {str(path): sha256(path) for case in cases
                        for arg in case["args"] if (path := Path(arg)).is_file()}
    report["results"] = []
    try:
        measure(binaries, cases, args.runs, args.warmups, env,
                args.timeout, report["results"])
    except (SpeedError, OSError) as error:
        report["error"] = str(error)
        raise
    except KeyboardInterrupt:
        report["error"] = "Interrupted"
        raise
    finally:
        target = output / "results.json"
        target.write_text(json.dumps(report, indent=2) + "\n")
        print(f"\nRaw results: {target}", flush=True)
    print("Speedup = old/new (higher is faster); runtime/Graal ratios: lower is better.")


if __name__ == "__main__":
    try:
        main()
    except (SpeedError, OSError) as error:
        print(f"speed-diff: {error}", file=sys.stderr)
        sys.exit(1)
    except KeyboardInterrupt:
        print("speed-diff: interrupted", file=sys.stderr)
        sys.exit(130)
