<!-- DO NOT EDIT — THIS FILE WAS GENERATED -->

YAMLScript
==========

Add Logic to Your YAML Files


## Quick Start

This library lets you load YAML files that may or may not contain
[YAMLScript](https://yamlscript.org) functional programming logic.
You can use it as a drop-in replacement for your current YAML loader.

Here's an example `config.yaml` that makes use of YAMLScript functions.

```yaml
# config.yaml with YAMLScript:
!ys-0:

# Define variables
db-host =: ENV.DB_HOST || 'localhost'
db-port =: ENV.DB_PORT || 5432
deploy =: ENV.DEPLOYMENT || 'dev'
:when deploy !~ /^(dev|stage|prod)/:
  die: |
    Invalid deployment value '$deploy'.
    Must be one of: dev | stage | prod

# Normal YAML data
description: Dynamic application configuration

# Dynamic data values
database:
  host:: db-host
  port:: db-port:num
  name:: "app_$deploy"

# Import external data
features:: load('common.yaml').features

# Use logic and conditions
cache:
  # Variable scoped to this mapping
  enabled =: deploy == 'production'

  directory: .cache
  enabled:: enabled
  limit: 100
  # Conditional key/value pairs
  :when enabled::
    limit:: 1000
    ttl:: 60 * 60  # 3600
```


## What is YAMLScript?

YAMLScript is a functional programming language that can be embedded in YAML.
Its syntax is 100% YAML so files that embed it are still valid YAML files.

The YAMLScript project provides YAML loader libraries for many programming
languages.
They can be used to load any YAML config files properly, whether or not they
contain functional programming logic.

It's perfect for:

* **Configuration files** that need logic, variables, and dynamic values
* **Data transformation** with built-in functions for JSON, YAML, and text
  processing
* **Templating** with powerful string interpolation and data manipulation
* **Scripting** as a complete functional programming language


## Key Features

* **Drop-in YAML replacement** – Works with your existing YAML files
* **Variables & functions** – Define and reuse values throughout your files
* **External data loading** – Import JSON, YAML, or data from URLs
* **Conditional logic** – Use if/then/else and pattern matching
* **Data transformation** – Built-ins for transforming & manipulating data
* **String interpolation** – Embed expressions/variables directly in strings
* **No JVM required** – Runs as a native library despite compiling to Clojure


## How It Works

YAMLScript extends YAML with a simple, elegant syntax:

```yaml
# file.yaml
!ys-0:               # Enable YAMLScript

name =: 'World'       # Variable assignment
nums =:: [1, 2, 3]    # Any YAML value

# Literal YAML with ':'
a key: a value

# Evaluated expressions with '::'
message:: "Hello, $name!"
sum:: nums.reduce(+)
timestamp:: now():str
```

You can load this file from a program as described below, or you can use the
`ys` YAMLScript binary to load the file from the command line:

```bash
$ ys -Y file.yaml
a key: a value
message: Hello, World!
sum: 6
timestamp: '2025-09-14T22:35:42.832470203Z'
```

Under the hood, YAMLScript compiles YAML to Clojure and evaluates it, giving
you access to a rich functional programming environment.

## Delphi (Free Pascal) Usage

Use `yamlscript` as a drop-in replacement for your current YAML
loader:

File `main.pas`:

```pascal
program main;

{$mode objfpc}{$H+}

uses
  Classes, SysUtils, fpjson, yamlscript;

var
  ys: TYAMLScript;
  input: TStringList;
  data: TJSONData;
begin
  ys := TYAMLScript.Create;
  input := TStringList.Create;
  try
    input.LoadFromFile('config.yaml');
    data := ys.Load(input.Text);
    try
      WriteLn(data.FormatJSON);
    finally
      data.Free;
    end;
  finally
    input.Free;
    ys.Free;
  end;
end.
```


## Installation

Get the source units from the
[yamlscript-delphi](https://github.com/yaml/yamlscript-delphi) repo
and install the `libys.so` shared library:

```bash
git clone https://github.com/yaml/yamlscript-delphi
curl -sSL https://yamlscript.org/install | LIB=1 bash
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```

Compile with the unit and library paths:

```bash
fpc -Fuyamlscript-delphi/src -Fl$HOME/.local/lib main.pas
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* Free Pascal Compiler (FPC) 3.0 or higher
* Linux, macOS or Windows

The binding is written in the Delphi dialect of Object Pascal and
currently builds with Free Pascal.
Native Delphi (Embarcadero) support is planned; contributions are
welcome.

## See Also

* [YAMLScript Web Site](https://yamlscript.org)
* [Learn YAMLScript](https://exercism.org/tracks/yamlscript)
* [YAMLScript Blog](https://yamlscript.org/blog)
* [YAMLScript Source Code](https://github.com/yaml/yamlscript)
* [YAMLScript Programs](https://rosettacode.org/wiki/Category:YAMLScript)
* [YAML](https://yaml.org)
* [Clojure](https://clojure.org)


## Authors

* [Ingy döt Net](https://github.com/ingydotnet)

## License & Copyright

Copyright 2022-2026 Ingy döt Net <ingy@ingy.net>

This project is licensed under the terms of the `MIT` license.
See [LICENSE](https://github.com/yaml/yamlscript/blob/main/License) for more
details.
