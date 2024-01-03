use yamlscript::Error;

#[test]
fn load_sample_ys() {
    let ys = yamlscript::Yamlscript::new().unwrap();
    let ret = ys
        .load::<serde_json::Value>(
            r#"!yamlscript/v0/data
               say: "Hello"
               key: ! inc(42)
               baz: ! range(1 6)"#,
        )
        .unwrap();

    let obj = ret.as_object().unwrap();
    assert!(obj.contains_key("say"));
    assert!(obj.contains_key("key"));
    assert!(obj.contains_key("baz"));

    assert_eq!(ret.get("say").unwrap().as_str().unwrap(), "Hello");
    assert_eq!(ret.get("key").unwrap().as_u64().unwrap(), 43);

    let arr = ret.get("baz").unwrap().as_array().unwrap();
    assert_eq!(arr.len(), 5);
    assert_eq!(arr[0].as_u64().unwrap(), 1);
    assert_eq!(arr[1].as_u64().unwrap(), 2);
    assert_eq!(arr[2].as_u64().unwrap(), 3);
    assert_eq!(arr[3].as_u64().unwrap(), 4);
    assert_eq!(arr[4].as_u64().unwrap(), 5);
}

#[derive(serde::Deserialize, Debug)]
struct Response {
    say: String,
    key: u64,
    baz: Vec<u64>,
}

#[test]
fn load_sample_ys_serde() {
    let ys = yamlscript::Yamlscript::new().unwrap();
    let ret = ys
        .load::<Response>(
            r#"!yamlscript/v0/data
               say: "Hello"
               key: ! inc(42)
               baz: ! range(1 6)"#,
        )
        .unwrap();
    assert_eq!(ret.say, "Hello");
    assert_eq!(ret.key, 43);
    assert_eq!(ret.baz, &[1, 2, 3, 4, 5]);
}

#[test]
fn load_sample_error() {
    let ys = yamlscript::Yamlscript::new().unwrap();
    let result = ys.load::<Response>(
        r#"!yamlscript/v0/data
           : : : : : :
        "#,
    );
    dbg!(&result);
    assert!(matches!(result, Err(Error::Yamlscript(_))));
}
