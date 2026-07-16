///|
test "load YAMLScript" {
  let data = @yamlscript.load("!ys-0:\ntest:: inc(41)")
  match data {
    Object(object) =>
      match object.get("test") {
        Some(Number(value, ..)) => @test.assert_eq(value, 42.0)
        _ => @test.fail("unexpected test value")
      }
    _ => @test.fail("unexpected data value")
  }
}

///|
test "load plain YAML" {
  let data = @yamlscript.load("foo: bar")
  match data {
    Object(object) =>
      match object.get("foo") {
        Some(String(value)) => @test.assert_eq(value, "bar")
        _ => @test.fail("unexpected foo value")
      }
    _ => @test.fail("unexpected data value")
  }
}

///|
test "load error raises" {
  @test.assert_raise(() => @yamlscript.load(":"))
}
