<!-- DO NOT EDIT — THIS FILE WAS GENERATED -->

YS / YAMLScript
===============

Add Logic to Your YAML Files


## Synopsis

Load `file.yaml` with YS:

```yaml
!YS-v0:

# Get data from external sources:
names-url =:
  'github:dominictarr/random-name/first-names.json'

name-list =: names-url:curl:json/load

# Data object with literal keys and generated values:
name:: name-list:shuffle:first
aka:: name-list:rand-nth
age:: &num 2 * 3 * 7
color:: &hue
  rand-nth: qw(red green blue yellow)
title:: "$(*num) shades of $(*hue)."
```

and get:
```json
{
  "name": "Dolores",
  "aka": "Anita",
  "age": 42,
  "color": "green",
  "title": "42 shades of green."
}
```


## Description

[YS](https://yamlscript.org) is a functional programming language with a clean
YAML syntax.

YS can be used for enhancing ordinary [YAML](https://yaml.org) files with
functional operations, such as:

* Import (parts of) other YAML files to any node
* String interpolation including function calls
* Data transforms including ones defined by you

This YS library should be a drop-in replacement for your current YAML loader!

Most existing YAML files are already valid YS files.
This means that YS works as a normal YAML loader, but can also evaluate
functional expressions if asked to.

Under the hood, YS code compiles to the Clojure programming language.
This makes YS a complete functional programming language right out of the box.

Even though YS compiles to Clojure, and Clojure compiles to Java, there is no
dependency on Java or the JVM.
YS is compiled to a native shared library (`libys.so`) that can be used
by any programming language that can load shared libraries.

To see the Clojure code that YS compiles to, you can use the YS
CLI binary `ys` to run:

```text
$ ys --compile file.ys
(let
 [names-url "https://raw.githubusercontent.com/dominictarr/random-name/master/first-names.json"
  name-list (json/load (curl names-url))]
 (%
  "name" (first (shuffle name-list))
  "aka" (rand-nth name-list)
  "age" (_& 'num (mul+ 2 3 7))
  "color" (_& 'hue (rand-nth (qw red green blue yellow)))
  "title" (str (_** 'num) " shades of " (_** 'hue) ".")))
```

## Haskell Usage

File `prog.hs`:

```haskell
import YAMLScript
import qualified Data.Text as T

main :: IO ()
main = do
  let input = T.pack "inc: 41"
  result <- loadYAMLScript input
  print result
```

File `file.ys`:

```yaml
!YS-v0:

name =: "World"

foo: [1, 2, ! inc(41)]
bar:: load("other.yaml")
baz:: "Hello, $name!"
```

File `other.yaml`:

```yaml
oh: Hello
```

Run:

```text
$ runhaskell prog.hs
Number 42.0
```

## Installation

You can install this package using Cabal:

```bash
cabal install yamlscript
```

but you will need to have a system install of `libys.so`.

One simple way to do that is with:

```bash
curl https://yamlscript.org/install | bash
```

> Note: The above command will install the latest version of the YAMLScript
command line utility, `ys`, and the shared library, `libys.so`, into
`~/local/bin` and `~/.local/lib` respectively.

See <https://yamlscript.org/doc/install/> for more info.

## API Reference

### `loadYAMLScript :: MonadIO m => Text -> m Aeson.Value`

Load and evaluate YAMLScript code from a string.
Returns the result as a JSON Value.

### `loadYAMLScriptFile :: MonadIO m => FilePath -> m Aeson.Value`

Load and evaluate YAMLScript code from a file.
Returns the result as a JSON Value.

### `loadYAMLScriptPure :: Text -> Aeson.Value`

Convenience function for pure contexts.
Note: This uses unsafePerformIO and should be used carefully.

### `YAMLScriptError`

Error type for YAMLScript operations:
- `YAMLScriptParseError String` - Parsing errors
- `YAMLScriptRuntimeError String` - Runtime evaluation errors
- `YAMLScriptFFIError String` - Foreign function interface errors

## See Also

* [YS Web Site](https://yamlscript.org)
* [YS Blog](https://yamlscript.org/blog)
* [YS Source Code](https://github.com/yaml/yamlscript)
* [YS Samples](https://github.com/yaml/yamlscript/tree/main/sample)
* [YS Programs](https://rosettacode.org/wiki/Category:YAMLScript)
* [YAML](https://yaml.org)
* [Clojure](https://clojure.org)


## Authors

Ingy döt Net <ingy@ingy.net>

## License & Copyright

Copyright 2022-2025 Ingy döt Net <ingy@ingy.net>

This project is licensed under the terms of the `MIT` license.
See [LICENSE](https://github.com/yaml/yamlscript/blob/main/License) for
more details.
