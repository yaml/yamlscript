fn main() {
    let input = std::io::read_to_string(std::io::stdin()).unwrap();
    let output = yamlscript::load::<serde_json::Value>(&input).unwrap();
    println!("{output}");
}
