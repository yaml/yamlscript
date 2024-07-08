## Julia Usage

File `prog.jl`:

```julia
import YAMLScript as YS
ys = YS.Runtime()
yaml = read("file.ys", String)
data = YS.load(ys, yaml)
println(data)
```

File `file.ys`:

```yaml
!yamlscript/v0

name =: "World"

=>::
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
$ julia prog.jl
Dict{String, Any}("bar" => Dict{String, Any}("oh" => "Hello"), "baz" => "Hello, World!", "foo" => Any[1, 2, 42])
```


## Installation

You can install this package like any other Julia package:

```bash
$ julia --project=. -e 'using Pkg; Pkg.add("YAMLScript")'
```

but you will need to have a system install of `libyamlscript.so`.

One simple way to do that is with:

```bash
$ curl https://yamlscript.org/install | bash
```

> Note: The above command will install the latest version of the YAMLScript
command line utility, `ys`, and the shared library, `libyamlscript.so`, into
`~/local/bin` and `~/.local/lib` respectively.
You may need to add this install directory to `LD_LIBRARY_PATH`.

See https://github.com/yaml/yamlscript?#installing-yamlscript for more info.
