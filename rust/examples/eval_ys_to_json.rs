fn main() {
    let input = std::io::read_to_string(std::io::stdin()).unwrap();
    let ys = yamlscript::YAMLScript::new().unwrap();
    let output = ys.load::<serde_json::Value>(&input).unwrap();
    println!("{output}");
}
