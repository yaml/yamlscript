## Haskell Usage

Use `yamlscript` as a drop-in replacement for your current YAML loader:

```haskell
-- program.hs
import YAMLScript
import qualified Data.Text as T
import qualified Data.Text.IO as TIO

main :: IO ()
main = do
  -- Load from file
  input <- TIO.readFile "config.yaml"
  result <- loadYAMLScript input
  print result
```


## Installation

Install YAMLScript for Haskell and the `libys.so` shared library:

```bash
# Add to your .cabal file:
build-depends: yamlscript

# Or install directly
cabal install yamlscript

# Install shared library
curl -sSL https://yamlscript.org/install | bash
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* GHC 9.0 or higher