## Rust Usage

Create a new Rust project:

```text
$ cargo new --bin prog
$ cd prog
```

Add the file `src/main.rs`:

```rust
use std::fs::File;
use std::io::prelude::*;
use yamlscript::YAMLScript;

fn main() -> std::io::Result<()> {
    let mut file = File::open("file.ys")?;
    let mut input = String::new();
    file.read_to_string(&mut input)?;
    let ys = YAMLScript::new().unwrap();
    let data = ys.load::<serde_json::Value>(&input).unwrap();
    println!("{data:?}");
    Ok(())
}
```

Add file `file.ys`:

```yaml
!YS-v0:

name =: "World"

foo: [1, 2, ! inc(41)]
bar:: load("other.yaml")
baz:: "Hello, $name!"
```

Add file `other.yaml`:

```yaml
oh: Hello
```

Run:

```text
$ curl https://yamlscript.org/install | bash
$ cargo add yamlscript
$ cargo add serde_json
$ cargo run
    Finished dev [unoptimized + debuginfo] target(s) in 0.02s
     Running `target/debug/prog`
Object {"bar": Object {"oh": String("Hello")}, "baz": String("Hello, World!"), "foo": Array [Number(1), Number(2), Number(42)]}
```


## Installation

You can install this module like any other Rust module:

```bash
$ cargo add yamlscript
```

but you will need to have a system install of `libyamlscript.so`.

One simple way to do that is with:

```bash
$ curl https://yamlscript.org/install | bash
```

> Note: The above command will install the latest version of the YAMLScript
command line utility, `ys`, and the shared library, `libyamlscript.so`, into
`~/.local/bin` and `~/.local/lib` respectively.

See https://github.com/yaml/yamlscript/wiki/Installing-YAMLScript for more info.
