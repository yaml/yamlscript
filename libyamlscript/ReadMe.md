yamlscript/libyamlscript
========================

Build the YAMLScript compiler into a shared library


## Synopsis

```
$ make build
```


## Description

This directory builds the YAMLScript compiler (written in Clojure) into a shared
library for binding to other porgramming languages.

The compilation is acheived by using the GraalVM native-image tool to compile
the compiled Clojure code into a shared library.

The `yamlscript.core/compile` function takes a YAMLScript input string and
compiles it to a Clojure code string.


## Prerequisites

You just need Clojure and GNU `make` installed.
GraalVM is automatically downloaded and installed by the build system.
See Build Instructions below.

The build system supports using both:

* [Oracle GraalVM Free Terms](https://www.graalvm.org/downloads/)
* [GraalVM Community Edition](
  https://github.com/graalvm/graalvm-ce-builds/releases/)


## Build Instructions

Just run one of the following commands:

```
$ make build                            # Oracle GraalVM jdk-21.0.0

$ make build GRAALVM_VER=17             # Oracle GraalVM jdk-17.0.8

$ make build GRAALVM_CE=1               # GraalVM CE jdk-21.0.0

$ make build GRAALVM_CE=1 GRAAL_VER=17  # GraalVM CE jdk-17.0.8
```

The Makefile system will download and the approriate GraalVM version according
to your OS type and your intended GraalVM version.
It then sets the appropriate environment variables for the GraalVM build system
to work, so there is no need to install GraalVM yourself.
If you already have GraalVM installed, that installation will not be used and
will not be affected by the build process.

The downloaded GraalVM tarball will be placed under `/tmp/` so you only need to
download it once.
Subsequent builds will use the cached GraalVM file.

You should run `make clean` between builds to delete the old build artifacts.
This will not delete the downloaded GraalVM file.
