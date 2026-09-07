---
title: Standalone YS Programs
talk: 0
---

The `ys` command is distributed as a standalone executable built with Gloat
and Glojure.
It does not require Java or a JVM.

The former `ys --binary` option for compiling an individual YAMLScript program
with GraalVM has been retired.
Distribute a `.ys` source file together with the appropriate `ys` release
executable, or compile the program to portable Clojure with `ys --compile`.

The release workflow publishes executables for Linux, macOS, FreeBSD, Windows,
and WASI Preview 1.
