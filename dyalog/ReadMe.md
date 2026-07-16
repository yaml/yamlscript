# YAMLScript for Dyalog APL

YAMLScript loader binding for Dyalog APL.

This binding is currently built and tested entirely inside the
`docker.io/dyalog/dyalog` container image.


## Usage

```apl
⎕FIX⊃⎕NGET 'src/YAMLScript.apln' 1
data←YAMLScript.Load '!ys-0:',(⎕UCS 10),'answer:: 6 * 7'
⎕←data.answer
```

Run the tests:

```bash
make -C dyalog test
```


## Packaging

Dyalog packages are commonly distributed with Tatin.
The included `apl-package.json` is a starting point for publishing this binding
from a future `yaml/yamlscript-dyalog` repository.
