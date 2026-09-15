---
title: Installing YS
talk: 0
---

YS has 3 main things you might want to install:

1. The `ys` compiler, loader and runner command.
2. The `libys.so` shared library needed by a YS loader library for your
   particular programming language.
3. A [YS loader library](bindings.md#currently-available-libraries) for your
   particular programming language, like Python, Rust, NodeJS, etc.


## Install with Homebrew

You can install `ys` and `libys` on macOS or Linux with Homebrew.

```bash
brew trust yaml/yamlscript
brew tap yaml/yamlscript

brew install ys                       # Install ys first time
brew upgrade yaml/yamlscript/ys       # Install new ys version later

brew install yaml/yamlscript/libys    # Install libys first time
brew upgrade yaml/yamlscript/libys    # Install new libys version later
```

Install `libys` (`libys.so` on Linux, `libys.dylib` on macOS) when you need the
shared library for a YS loader library in another programming language.


### Install with in-1

You can install `ys` and `libys` persistently with
[in-1](https://in-1.cc/install/).

Examples for Bash and Zsh:

```bash
$ source <(curl -sL https://in-1.cc) --local ys libys
$ source <(curl -sL https://in-1.cc) --local ys
$ source <(curl -sL https://in-1.cc) --local libys
$ source <(curl -sL https://in-1.cc) --local ys libys \
    YAMLSCRIPT-VERSION=0.3.1 LIBYS-VERSION=0.3.1
$ source <(curl -sL https://in-1.cc) --local ys libys \
    PREFIX=/tmp/yamlscript
```

For Fish:

```fish
$ curl -sL https://in-1.cc | source - --local ys libys
```

`YAMLSCRIPT-VERSION` and `LIBYS-VERSION` default to the latest versions.
`PREFIX` defaults to `$HOME/.local` for `--local` installations.
See the in-1 installation documentation for temporary installations,
environment setup, upgrades, and removal.


<!--
### Temporary Test Install

If you just want to try out the `ys` command but not install it permanently, you
can run this (in Bash and Zsh only):
```bash
source <(curl https://yamlscript.org/try-ys)
```

This will install the `ys` binary under `/tmp/` and add the directory to you
current shell's `PATH`.
It will only be available for the duration of the shell session that you run it
in.
-->


### Install and Upgrade with `ys`

Once `ys` is installed, it can install or upgrade release files directly:

```bash
ys --install                         # Install libys
ys --upgrade                         # Upgrade ys and libys
ys --install-m2                      # Install runtime jars for ys -T bb
VERSION=0.3.1 ys --upgrade           # Select a release
PREFIX=/tmp/yamlscript ys --upgrade  # Select an installation prefix
```

These commands support Linux and macOS on x64 and ARM64.
They use system tools such as `curl` and `tar`, without running an installer
script or an archive's Makefile.
Upgrades install the default Glojure build, including upgrades from GraalVM.

`VERSION` defaults to the latest published release.
`PREFIX` defaults to the installation containing the running executable.
Relative prefixes are resolved from the current directory.
`BIN=1` selects only `ys`; otherwise `LIB=1` selects only `libys`.
`TARBALL=/path/to/release.tar.xz` installs a local release archive instead of
downloading one, selecting `ys` or `libys` from the archive name.
`QUIET=1` suppresses installation messages.

Installing `ys` also installs its bundled Maven resources into `~/.m2`, unless
`M2=0` is set or the installer is running as root.
Run `ys --install-m2` as your normal user to install missing runtime JARs and
POMs for the running `ys` version.
This command preserves existing files and extracts the runtime JAR when
`unzip` is available.


### Download and Install

All the binary pre-built release files are
[here](https://github.com/yaml/yamlscript/releases).

* Download the appropriate release file.
* Expand the file with `$ tar xf <release-file>`.
* Use `cd` to enter the release directory.
* Run `make install` or `make install PREFIX=...`.
  * Or just copy the binary file to the place where you want it.


### Build a Release from Source

You can also easily build and install `ys` and `libys` from source:

* Download the "Source code" release file.
* Expand the file with `$ tar xf <release-file>`.
* Use `cd` to enter the release directory.
* Run `make install` or `make install PREFIX=...`.

This will take a few minutes but it requires no dependencies besides `bash`,
`git`, `make` and `curl`.

!!! note

    On Linux it also requires the `libz-dev` package.


### Install a YS Loader Library

YS loader libraries are intended to be a  drop in replacement for your
current YAML loader.

YS loader libraries are currently available for these programming
languages:

* [Ada](https://alire.ada.dev/crates/yamlscript.html)
* [C#](https://www.nuget.org/packages/YAMLScript/)
* [Clojure](https://clojars.org/org.yamlscript/clj-yamlscript)
* [Crystal](https://shardbox.org/shards/yamlscript)
* [D](https://code.dlang.org/packages/yamlscript)
* [Dart](https://pub.dev/packages/yamlscript)
* [Delphi (Free Pascal)](https://github.com/yaml/yamlscript-delphi)
* [Dyalog APL](https://tatin.dev/v1/packages/versions/yaml-yamlscript-0)
* [Elixir](https://hex.pm/packages/yamlscript)
* [Erlang](https://hex.pm/packages/yamlscript_erlang)
* [F#](https://www.nuget.org/packages/YAMLScript.FSharp)
* [Fortran](https://github.com/yaml/yamlscript-fortran)
* [Go](https://github.com/yaml/yamlscript-go)
* [Haskell](https://hackage.haskell.org/package/yamlscript)
* [Java](https://central.sonatype.com/artifact/org.yamlscript/yamlscript)
* [Julia](https://juliahub.com/ui/Packages/General/YAMLScript)
* [Kotlin](
  https://central.sonatype.com/artifact/org.yamlscript/kotlin-yamlscript)
* [Lua](https://luarocks.org/modules/ingy/yamlscript)
* [MoonBit](https://mooncakes.io/docs/ingydotnet/yamlscript)
* [Nim](https://github.com/yaml/yamlscript-nim)
* [NodeJS](https://www.npmjs.com/package/@yaml/yamlscript)
* [Perl](https://metacpan.org/pod/YAMLScript)
* [PHP](https://packagist.org/packages/yaml/yamlscript)
* [PowerShell](https://www.powershellgallery.com/packages/YAMLScript/)
* [Python](https://pypi.org/project/yamlscript/)
* [R](https://github.com/yaml/yamlscript-r)
* [Raku](https://raku.land/zef:ingy/YAMLScript)
* [Ruby](https://rubygems.org/gems/yamlscript)
* [Rust](https://crates.io/crates/yamlscript)
* [Scala](https://central.sonatype.com/artifact/org.yamlscript/scala-yamlscript)
* [Swift](https://github.com/yaml/yamlscript-swift)
* [Zig](https://github.com/yaml/yamlscript-zig)

Install the library you want using the normal library installer for your
language.
Then install the matching version of the `libys` shared library as
described above.

!!! note

    Currently when you install a YS loader library for your
    particular programming language you must also install the `libys`
    shared library of the **exact same version**.
    Fortunately this is simple.
