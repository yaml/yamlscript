# Comparing Glojure revisions

Run `make speed-diff` from either YAMLScript or its containing YAMLStar
workspace to compare Glojure `HEAD^` with `HEAD` using a fixed YAMLScript
revision and a GraalVM executable.

```bash
make speed-diff
make speed-diff old=cc865fd new=HEAD
make speed-diff cases=yaml-release,execute-numeric
make speed-diff cases=list
```

The defaults are two warmups and eleven measured invocations per binary and
operation.
Execution order rotates between the old runtime, new runtime, and GraalVM.
Every row shows elapsed milliseconds, old/new speedup, and each Glojure
runtime's elapsed time divided by GraalVM's time.
There is no combined score that could hide a regression.

The suite covers startup, small and large YAML loads, wide and deep
collections, document streams, compilation of data and code, numeric
execution, collection processing, strings, and JSON output.
Output must match between all three binaries.
Compilation comparisons ignore formatting outside strings and character
literals; all other comparisons require identical bytes.

## Sources and builds

The revisions are resolved in the Glojure checkout, not the YAMLScript or
YAMLStar checkout.
Dirty and untracked files are excluded even when comparing `HEAD`.
Both builds use isolated committed Glojure sources, the same YAMLScript
`HEAD`, the same Gloat `HEAD`, and Gloat's Makes-provided Go.
Go build flags are inherited and included in the build cache identity.
The command never checks out revisions in your working trees or overwrites
your normal CLI executables.

By default, related sources are taken from `repos/glojure` and `repos/gloat`.
When YAMLScript itself is under a workspace's `repos/` directory, those
workspace checkouts are used.
Override them with `glojure-dir=/path/to/glojure` and
`gloat-dir=/path/to/gloat`.
`GLOJURE_DIR` is also supported.

The default GraalVM reference is `ys/bin/ys-VERSION-graalvm` in YAMLScript.
Use `graalvm-bin=/path/to/ys-graalvm` to select another existing executable.
Its version must match YAMLScript's version.
Its hash and version are recorded; its source provenance is explicitly
reported as an existing executable rather than a rebuilt reference.

## Cache and results

Sources, dependency caches, build logs, and binaries are stored under
`.cache/speed-diff` in YAMLScript.
Builds are reused only when revisions, bootstrap executable, toolchain,
platform, and build options match, and the cached executable hash is intact.
Use `rebuild=1` to compile again or `speed-cache=/path/to/cache` for a separate
cache.
Modified cached source checkouts cause an error instead of being reset.
Comparisons sharing a cache cannot run concurrently.
After an interrupted process, remove its `.cache/speed-diff/.lock` directory
once the process has stopped.

Each run writes `results.json` under `.cache/speed-diff/runs/` with all timings,
medians, ratios, source revisions, input hashes, executable hashes, and build
information.
Set `runs=N` and `warmups=N` for a shorter diagnostic run.
Each invocation has a 60 second timeout, adjustable with `case-timeout=N`.
Failures preserve completed rows and input hashes in the report.
Use the defaults and repeat the complete run before accepting a performance
claim.
Keep builds and other tests stopped while measuring.

`make speed-diff-test` runs the comparison tool's focused regression tests.
