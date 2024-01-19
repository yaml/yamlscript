use std::fs::File;
use std::io::prelude::*;
use yamlscript::YAMLScript;

fn main() -> std::io::Result<()> {
    let mut file = File::open("file.ys")?;
    let mut input = String::new();
    file.read_to_string(&mut input)?;

    let docs = YAMLScript::load(&input).unwrap();

    for doc in &docs {
        println!("{:?}", doc);
    }

    Ok(())
}
