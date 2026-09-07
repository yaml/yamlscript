# yamlscript/libys

Build the YAMLScript compiler and runtime as a shared library.

## Building

The default build uses Gloat and Glojure:

```bash
make build
```

The repository-managed Makes system downloads all build dependencies into
`./.cache/`.
No JVM or GraalVM installation is needed for the default build.

The GraalVM implementation remains available as an alternate build:

```bash
make build-graalvm
```

## C API

The Glojure library preserves the existing public ABI:

```c
char *load_ys_to_json(
  graal_isolatethread_t *thread, const char *yamlscript);
```

Existing bindings may continue to call the `graal_*` lifecycle functions.
They are compatibility no-ops in the Glojure library because its Go runtime is
process-wide.

The default output is `lib/libys-glojure.so` on Linux, with the usual
`libys.so` compatibility links.
Platform-specific extensions are used on macOS and Windows.

The `yamlscript.compiler/compile` function takes YAMLScript input and produces
Clojure code.
The library evaluates that code with Glojure and returns the established JSON
data or error envelope.
