YS / YAMLScript
===============

Program in YAML — Code is Data


## About YS / YAMLScript

[YS](https://yamlscript.org) is a new YAML loader for 15 (and counting)
programming languages:
* [C#](https://www.nuget.org/packages/YAMLScript/)
* [Clojure](https://clojars.org/org.yamlscript/clj-yamlscript)
* [Crystal](https://github.com/yaml/yamlscript/crystal)
* [Go](https://github.com/yaml/yamlscript-go)
* [Haskell](https://hackage.haskell.org/package/yamlscript)
* [Java](https://clojars.org/org.yamlscript/yamlscript)
* [Julia](https://juliahub.com/ui/Packages/General/YAMLScript)
* [Lua](https://luarocks.org/modules/ingy/yamlscript)
* [NodeJS](https://www.npmjs.com/package/@yaml/yamlscript)
* [Perl](https://metacpan.org/pod/YAMLScript)
* [PHP](https://packagist.org/packages/yaml/yamlscript)
* [Python](https://pypi.org/project/yamlscript/)
* [Raku](https://raku.land/zef:ingy/YAMLScript)
* [Ruby](https://rubygems.org/gems/yamlscript)
* [Rust](https://crates.io/crates/yamlscript)

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
You can install both in `~/.local/bin` and `~/.local/lib` respectively, with:
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
ys --load file.ys
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
The `::` tells YS that the value is a code expression, not a data value.


### Why YS is Different

YS programs can either be "run" or "loaded".
When a YS program is run, it is executed as a normal program.
When a YS program is loaded, it evaluates to a JSON-model data structure.

If you have a valid YAML ([1.2 Core Schema](
https://yaml.org/spec/1.2.2/#103-core-schema)) file that doesn't use custom
tags, and loads to a value expressible in JSON, then it is a valid YS program.
The YS `load` operation will evaluate that file exactly the same in any
programming language / environment.


### Real-World Examples

Want to merge multiple YAML files with environment variable overrides?

```yaml
!YS-v0:
config =: merge(
  load('base-config.yaml')
  load('prod-config.yaml')
  ENV)

database:
  host: $config.database.host
  port: $config.database.port || 5432
  ssl:: config.environment == 'production'
```

Need to transform data from an API?

```yaml
!YS-v0:
users =: json(http-get('https://api.example.com/users'))

active-users::
  users:filter(fn [u] =>
    u.status == 'active' &&
    u.lastLogin > date('30 days ago')):
  map(fn [u] =>
    name: "$u.firstName $u.lastName"
    email: u.email
    role: u.role:upper-case())
```

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

### Getting Started with YS

There are two primary ways to use YS:

* Using the `ys` command line runner / loader / compiler / installer
* Using a YS library in your own programming language

The `ys` command line tool is the easiest way to get started with YS.
It has these main modes of operation:

* `ys <file>` - Run a YS program
* `ys --load <file>` - Load a YS program
* `ys --compile <file>` - Compile a YS program to Clojure
* `ys --binary <file>` - Compile YS to a native binary executable
* `ys --eval '<expr>'` - Evaluate a YS expression string
* `ys --install` - Install the latest libys shared library
* `ys --upgrade` - Upgrade ys and libys
* `ys --help` - Show the `ys` command help

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
# Works with your existing YAML files!

# Or use YS features:
result = ys.eval('range(1, 11):map(inc):filter(even?)')
# [2, 4, 6, 8, 10]
```

**JavaScript/Node:**
```javascript
const YS = require('@yaml/yamlscript');
const ys = new YS();

const config = ys.load(fs.readFileSync('config.yaml', 'utf8'));
// Seamless drop-in replacement!
```

**Go:**
```go
import "github.com/yaml/yamlscript-go"

ys := yamlscript.New()
data, _ := ys.Load(yamlContent)
// Same API across all languages!
```

**Ruby:**
```ruby
require 'yamlscript'

ys = YAMLScript.new
config = ys.load(File.read('config.yaml'))
# Just works!
```

### Supported Programming Language Bindings

YS has the same API, features, and behavior across all supported languages:

* [C#](https://www.nuget.org/packages/YAMLScript/) • [Clojure](https://clojars.org/org.yamlscript/clj-yamlscript) • [Crystal](https://github.com/yaml/yamlscript/crystal) • [Go](https://github.com/yaml/yamlscript-go) • [Haskell](https://hackage.haskell.org/package/yamlscript) • [Java](https://clojars.org/org.yamlscript/yamlscript) • [Julia](https://juliahub.com/ui/Packages/General/YAMLScript) • [Lua](https://luarocks.org/modules/ingy/yamlscript) • [NodeJS](https://www.npmjs.com/package/@yaml/yamlscript) • [Perl](https://metacpan.org/pod/YAMLScript) • [PHP](https://packagist.org/packages/yaml/yamlscript) • [Python](https://pypi.org/project/yamlscript/) • [Raku](https://raku.land/zef:ingy/YAMLScript) • [Ruby](https://rubygems.org/gems/yamlscript) • [Rust](https://crates.io/crates/yamlscript)

### Why Choose YS?

**🚀 Performance** - Native compiled code runs as fast or faster than Python/Perl
**🔧 Reliability** - Same implementation across all languages means consistent behavior
**📦 Zero Dependencies** - Single shared library with no runtime dependencies
**🎯 100% YAML Compatible** - Works with all your existing YAML files today
**💪 Powerful When Needed** - Access to 1000+ built-in functions and full Clojure ecosystem
**🌐 Universal** - One syntax to learn, use everywhere

### Under the Hood

YS compiles YAML to [Clojure](https://clojure.org/) and runs it using the [Small Clojure Interpreter (SCI)](https://github.com/babashka/sci) - a native runtime with no JVM dependency. This gives you:

- The elegance and power of a functional Lisp
- The familiar syntax of YAML
- The performance of compiled native code
- The same exact behavior in every programming language

You don't need to know Clojure to use YS - start with simple YAML and add features as you need them.


## Try the YS `ys` Command

You can try out the latest version of the `ys` command without actually
"installing" it.

If you run this command in Bash or Zsh:

```
. <(curl https://yamlscript.org/try-ys)
```

it will install the `ys` command in a temporary directory (under `/tmp/`) and
then add the directory to your current `PATH` shell variable.

This will allow you to try the `ys` command in your current shell only.
No other present or future shell session will be affected.

Try it out!


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

The `make install` command will install `ys` and `libys` to `~/.local/bin` and
`~/.local/lib` respectively, by default.
If run as root they will default to `/usr/local/bin` and `/usr/local/lib`.

To install to a different location, run `make install PREFIX=/some/path`.

> Notes:
> * `make install` triggers a `make build` if needed, but...
> * You need to run `make build` not as root
> * The build can take several minutes (`native-image` is slow)
> * If you install to a custom location, you will need to add that location to
>   your `PATH` and `LD_LIBRARY_PATH` environment variables


### Installing a YS Binding for a Programming Language

YS ships its language binding libraries and the `libys.so` shared library
separately.

Currently, each binding release version requires an exact version of the shared
library, or it will not work.
That's because the YS language is still evolving quickly.

The best way to install a binding library is to use your programming language's
package manager to install the latest binding version, and the YS installer to
install the latest shared library version.

So for Python you would:

```bash
pip install yamlscript
ys --install
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
