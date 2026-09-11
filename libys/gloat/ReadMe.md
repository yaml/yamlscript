# Glojure libys compatibility layer

`src/libys.clj` implements YAMLScript evaluation and its JSON result envelope.
`native/abi.c` and `native/abi.go` implement the C API declared in `include/`.
Gloat copies these files into its generated main package using
`GLOAT_EXTRA_GO_MAIN_DIR`.

The `graal_*` names preserve compatibility with existing language bindings.
Handles are opaque C allocations tracked in a synchronized Go registry.
Attachments belong to native threads; create, attach, detach, and teardown
operate on the calling thread's attachments.
An isolate represents lifecycle bookkeeping around a shared runtime.
It does not provide independent namespaces or other GraalVM isolate semantics.
Teardown frees all handles associated with that isolate.
Callers must not reuse handles after detach or teardown.

Evaluation accepts a live attachment and runs on one persistent Go worker.
This serializes access to the process-wide Glojure runtime and preserves its
runtime bindings between calls.
Teardown waits for an active evaluation to finish.
Returned JSON strings remain C allocations, with the same ownership convention
as the previous Gloat exports.
The YAML parser's binary events protocol is unrelated to this layer.

Run `make -C libys test-abi` to build and test the library using its C headers.
Run `make -C libys test-installed-abi` to test unpacked release artifacts without
rebuilding them.
The C test covers lifecycle operations, invalid arguments, nullable outputs,
repeated creation, and concurrent evaluation from native threads.
Run `make -C libys test-handles` to check allocation failure and registry cleanup
with Go's race detector.
Before the pinned Glojure release exists, set `GLOJURE_DIR` to the absolute path
of the local `repos/glojure` checkout and override `GLOAT-DIR` with the local
`repos/gloat` path.
