YS / YAMLScript
===============

Program in YAML — Code is Data


## About YS / YAMLScript

[YS](https://yamlscript.org) is a new YAML loader for 15 (and counting)
programming languages:
[C#](https://www.nuget.org/packages/YAMLScript/),
[Clojure](https://clojars.org/org.yamlscript/clj-yamlscript),
[Crystal](https://github.com/yaml/yamlscript/crystal)
[Go](https://github.com/yaml/yamlscript-go),
[Haskell](https://hackage.haskell.org/package/yamlscript),
[Java](https://clojars.org/org.yamlscript/yamlscript),
[Julia](https://juliahub.com/ui/Packages/General/YAMLScript),
[Lua](https://luarocks.org/modules/ingy/yamlscript),
[NodeJS](https://www.npmjs.com/package/@yaml/yamlscript),
[Perl](https://metacpan.org/pod/YAMLScript),
[PHP](https://packagist.org/packages/yaml/yamlscript),
[Python](https://pypi.org/project/yamlscript/),
[Raku](https://raku.land/zef:ingy/YAMLScript),
[Ruby](https://rubygems.org/gems/yamlscript) and
[Rust](https://crates.io/crates/yamlscript).

You should consider trying out YS to replace your current YAML loader, because:

* It's easy to use
* It works the same way in every programming language
  * Same features, same bugs, same bug fixes
* YS has optional functional programming features
  * File imports, string interpolation, standard library, etc
  * Everything a compiled programming language has

How can YS offer all this?

YS is also a functional programming language!

Like [PyYAML](https://pyyaml.org/) and many other YAML loaders, YS is
implemented to the [YAML 1.2 specification](https://yaml.org/spec/1.2.2/).
But instead of loading YAML into its intended data structure, a YS loader loads
YAML into a Lisp AST (abstract syntax tree) data structure.
The AST is then rendered into Lisp code and evaluated, resulting in
the intended data structure.

The specific Lisp is [Clojure](https://clojure.org/), which is very capable,
mature and well-documented programming language with a large ecosystem of
libraries and tools.

You may be aware that Clojure is a JVM hosted language (and also a JavaScript
hosted one via ClojureScript), but YS doesn't need either of those.
YS is compiled to a native binary executable and also a native shared library.
The shared library can be used by nearly any programming language, including the
ones listed above.

Not only can you use YS as a loader libraryfrom a programming language, but you
can also use it from the command line with the `ys` command.

YS is awesome for:

* Querying, manipulating and transforming YAML (and JSON) files
  * Outputs include YAML, JSON, CSV, TSV and EDN
* Refactoring large monolythic YAML files into smaller, more manageable files
* Using data in your YAML files from external sources
  * Files, web, databases, APIs, shell commands, etc
* Applying over 1000 built-in functions to your YAML data
* Using YS libraries for even more functionality
* Writing complete programs, applications, automation scripts

> Note: YS is an official language on the
> [Exercism](https://exercism.org/tracks) (free) language learning site!
> It's a great way to learn how to program in YS.


### Using YS

First you need to install `ys` and `libys`.
You can install both to `~/.local/bin/ys` and `~/.local/lib/libys.so`
respectively, with:

```bash
curl https://yamlscript.org/install | bash
```
See the [Installing YS](#installing-ys) section below for more details.

Now say you have a file called `file.ys` that looks like this:
```yaml
name: Fido
age: 6
dog years: 42
likes: [running, fetching, playing, treats]
```

You can load it with:
```bash
ys --json file.ys
```

And get this output:
```json
{
  "name": "Fido",
  "age": 6,
  "dog years": 42,
  "likes": ["running", "fetching", "playing", "treats"]
}
```

Wait, that's just regular YAML! Exactly! YS is 100% compatible with existing
YAML files.
But here's where it gets interesting...

Let's say you want to calculate the dog years dynamically:

```yaml
!YS-v0:
age =: 6

name: Fido
age:: age
dog years:: age * 7
likes: [running, fetching, playing, treats]
```

Now when you load it, the dog years are calculated automatically!
The `age =: 6` sets a variable, and the `::` tells YS that the value is a code
expression, not a data value.


### Why YS is Different

With YS, YAML files can either be "loaded" or "run".
When a YS program is loaded, it evaluates to a JSON-model data structure.
When a YS program is run, it is executed as a normal program.

If you have a valid YAML ([1.2 Core Schema](
https://yaml.org/spec/1.2.2/#103-core-schema)) file that doesn't use custom
tags, and loads to a value expressible in JSON, then it will load properly with
YS.
The YS `load` operation will evaluate that file exactly the same in any
programming language / environment.


### Real-World Examples

Want to merge multiple YAML files with environment variable overrides?

```yaml
!YS-v0:
config =:
  merge:
    load: 'base-config.yaml'
    load: 'prod-config.yaml'

database:
  host:: config.database.host
  port:: config.database.port || 5432
  ssl:: ENV.environment == 'production'
```

Need to transform data from an API?

```yaml
!YS-v0:
users =: http/get('https://api.example.com/users'):json/load

active-users::
  map user-data:
    filter users:
      fn(u):
        (u.status == 'active') &&
        (u.lastLogin > date('-30d'))

defn user-data(u)::
  name:: "$(u.firstName) $(u.lastName)"
  email:: u.email
  role:: u.role:uc
```

<!--
Want to generate Kubernetes manifests dynamically?

```yaml
!YS-v0:
apps =: ['web', 'api', 'worker']
replicas =: ENV.PRODUCTION ? 3 : 1

deployments::
  apps:map(fn [app] =>
    apiVersion: 'apps/v1'
    kind: 'Deployment'
    metadata:
      name: app
      labels:
        app: app
    spec:
      replicas: replicas
      selector:
        matchLabels:
          app: app
      template:
        spec:
          containers:
          - name: app
            image: "mycompany/$app:$ENV.VERSION"
            resources:
              limits:
                memory: ENV."${app:upper-case()}_MEMORY" || '512Mi'
                cpu: ENV."${app:upper-case()}_CPU" || '500m')
```
-->


### Getting Started with YS

There are two primary ways to use YS:

* Using the `ys` command line runner / loader / compiler / installer
* Using a YS library in your own programming language

The `ys` command line tool is the easiest way to get started with YS.
You can use it to load / evaluate / transform YAML files and print the result as
YAML, JSON, CSV, TSV, or EDN.
Or you can use it to run programs written in YS.

You can also use YS as a library in your own programming language.
For example, in Python you can use the `yamlscript` module like this:

```python
import yamlscript
ys = yamlscript.YAMLScript()
text = open("foo.yaml").read()
data = ys.load(text)
```


### Supported Operating Systems

YS is supported on these operating systems:

* Linux
* macOS
* Windows  (work in progress)

YS is supported on these architectures:

* Intel/AMD (`x86_64`)
* ARM (`aarch64`)

For now other systems cannot be supported because `ys` and `libys` are compiled
by GraalVM's `native-image` tool, which only supports the above systems.


### Quick Examples in Your Language

**Python:**
```python
import yamlscript
ys = yamlscript.YAMLScript()
config = ys.load(open('config.yaml').read())
```

**JavaScript/Node:**
```javascript
const YS = require('@yaml/yamlscript');
const ys = new YS();
const config = ys.load(fs.readFileSync('config.yaml', 'utf8'));
```

**Go:**
```go
import "github.com/yaml/yamlscript-go"
ys := yamlscript.New()
data, _ := ys.Load(yamlContent)
```

**Ruby:**
```ruby
require 'yamlscript'
ys = YAMLScript.new
config = ys.load(File.read('config.yaml'))
```


### Why Choose YS?

* **🔧 Reliability** - Same implementation across all languages means consistent behavior
* **📦 Zero Dependencies** - Single shared library with no runtime dependencies
* **🎯 100% YAML Compatible** - Works with all your existing YAML files today
* **🚀 Performance** - Native compiled code runs as fast or faster than Python/Perl
* **💪 Powerful When Needed** - Access to 1000+ built-in functions and full Clojure ecosystem
* **🌐 Universal** - One syntax to learn, use everywhere


## Installing YS

You can install the YS `ys` interpreter and/or its `libys.so` shared
library from pre-built binaries or building from source.
Both are very easy to do.


### Installing YS Pre-built Binary Releases

YS ships pre-built binaries for each release version [here](
https://github.com/yaml/yamlscript/releases).

To install a latest release for your machine platform, try:

```bash
curl https://yamlscript.org/install | bash
```

Make sure `~/.local/bin` is in your `PATH` environment variable.

You can use the following environment variables to control the installation:

* `PREFIX=...` - The directory to install to. Default: `~/.local`
* `VERSION=...` - The YS version to install. Default: `0.2.3`
* `BIN=1` - Only install the `PREFIX/bin/ys` command line tool.
* `LIB=1` - Only install the `PREFIX/lib/libys` shared library.
* `DEBUG=1` - Print the Bash commands that are being run.

Once you have installed the `ys` command you can upgrade to a bin binary
version with `ys --upgrade`.


### Installing YS from Source

This is very easy to build and install YS from its source code because the YS
build process has very few dependencies:

* `bash` (your interactive shell can be any shell)
* `curl`
* `git`
* `make`

To install the `ys` command line tool, and `libys` shared library, run these
commands:

```bash
git clone https://github.com/yaml/yamlscript
cd yamlscript
make build
make install
```

That's it!

See [Installing YS](https://yamlscript.org/doc/install/) for full details.


### Installing a YS Binding for a Programming Language

YS ships its language binding libraries and the `libys.so` shared library
separately.

Currently, each binding release version requires an exact version of the shared
library, or it will not work.

The best way to install a binding library is to use your programming language's
package manager to install the latest binding version, and the YS installer to
install the latest shared library version.

So for Python you would:

```bash
pip install yamlscript
curl https://yamlscript.org/install | bash
```

The Perl installation process can automatically install the shared library, so
you can just do:

```bash
cpanm YAMLScript
```


## The YS Repository

The [YS source code repository](https://github.com/yaml/yamlscript)
is a mono-repo containing:

* The YS compiler code
* The YS shared library code
* A YS binding module for each programming language
* The YS test suite
* The YS documentation
* The yamlscript.org website (with docs, blog, wiki, etc)


### `make` It So

The YS repository uses a `Makefile` system to build, test and install its
various offerings.
It installs (locally within this directory)all the dependencies you need to
build and test YS, including every programming language needed by the bindings.

There is a top level `Makefile` and each repo subdirectory has its own
`Makefile`.
When run at the top level, many `make` targets like `test`, `build`, `install`,
`clean`, `distclean`, etc will invoke that target in each relevant subdirectory.

Given that this repository has so few dependencies, you should be able to clone
it and run `make` targets (try `make test`) without any problems.


### Contributing to YS

To ensure that YS libraries work the same across all languages, this project
aims to have a binding implementation for each programming language.

If you would like to contribute a new YS binding for a programming language,
you are encouraged to
[submit a pull request](https://github.com/yaml/yamlscript/pulls) to this
repository.

See the YS [Contributing Guide](
https://github.com/yaml/yamlscript/tree/main/Contributing.md) for more details.


## YS Resources

* [YS Documentation](https://yamlscript.org/doc/)
* [YS Blog](https://yamlscript.org/blog/)
* [Learn YS at Exercism](http://exercism.org/tracks/yamlscript)
* [Example YS Programs on RosettaCode.org](
  https://rosettacode.org/wiki/Category:YAMLScript)


## Authors

* [Ingy döt Net](https://github.com/ingydotnet) - Creator / Lead
* [Ven de Thiel](https://github.com/vendethiel) - Language design mentor
* [Delon R.Newman](https://github.com/delonnewman) - Clojure, Java and Ruby
* [Josephine Pfeiffer](https://github.com/pfeifferj) - Crystal binding
* [Andrew Pam](https://github.com/xanni) - Go binding
* [Kenta Murata](https://github.com/mrkn) - Julia binding
* [José Joaquín Atria](https://github.com/jjatria) - Perl binding
* [tony-o](https://github.com/tony-o) - Raku binding
* [Ethiraric](https://github.com/Ethiraric) - Rust binding


## Copyright and License

Copyright 2022-2025 by Ingy döt Net

This is free software, licensed under:

The MIT (X11) License
