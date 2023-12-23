fn main() {
    let input = std::fs::read_to_string("hearsay.ys").unwrap();
    let output = yamlscript::load(&input).unwrap();
    println!("{output}");
}
