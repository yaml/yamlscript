## Rust Usage

Use `yamlscript` as a drop-in replacement for your current YAML loader:

```rust
// program.rs
use yamlscript::YAMLScript;
use std::fs;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let ys = YAMLScript::new()?;

    // Load from file
    let content = fs::read_to_string("config.yaml")?;
    let data: serde_json::Value = ys.load(&content)?;

    println!("{:#?}", data);
    Ok(())
}
```


## Installation

Add YAMLScript to your `Cargo.toml` and install the shared library:

```toml
[dependencies]
yamlscript = "0.2"
serde_json = "1.0"
```

```bash
curl -sSL https://yamlscript.org/install | bash
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* Rust 1.70 or higher