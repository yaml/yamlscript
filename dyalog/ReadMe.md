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

This binding is published to [Tatin](https://tatin.dev/) as
[yaml-yamlscript](https://tatin.dev/v1/packages/versions/yaml-yamlscript-0).
