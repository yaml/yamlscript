## Elixir Usage

Use `yamlscript` as a drop-in replacement for your current YAML
loader:

File `main.exs`:

```elixir
{:ok, data} = YAMLScript.load(File.read!("config.yaml"))
IO.inspect(data)

# Or the raising variant:
data = YAMLScript.load!(File.read!("config.yaml"))
```


## Installation

Add `yamlscript` to your `mix.exs` deps and install the `libys.so`
shared library:

```elixir
def deps do
  [
    {:yamlscript, "~> 0.2.30"}
  ]
end
```

```bash
curl -sSL https://yamlscript.org/install | LIB=1 bash
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* Elixir 1.18 or higher (Erlang/OTP 27+)
* A C compiler (the package builds a small NIF shim)
* Linux or macOS
