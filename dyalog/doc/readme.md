## Dyalog APL Usage

```apl
⎕FIX⊃⎕NGET 'src/YAMLScript.apln' 1
data←YAMLScript.Load '!ys-0:',(⎕UCS 10),'answer:: 6 * 7'
⎕←data.answer
```

The binding requires Dyalog APL and the `libys.so` shared library.

For development in this repository, both are provided by the Docker-based
Makefile:

```bash
make -C dyalog test
```
