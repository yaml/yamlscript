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
