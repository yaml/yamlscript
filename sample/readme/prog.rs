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
