## Julia Usage

Use `YAMLScript.jl` as a drop-in replacement for your current YAML loader:

```julia
# program.jl
using YAMLScript

# Create YAMLScript runtime
ys = YAMLScript.Runtime()

# Load from file
input = read("config.yaml", String)
config = YAMLScript.load(ys, input)

println(config)
```


## Installation

Install YAMLScript for Julia and the `libys.so` shared library:

```bash
# Install package
julia -e 'using Pkg; Pkg.add("YAMLScript")'

# Install shared library
curl -sSL https://yamlscript.org/install | bash
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* Julia 1.8 or higher