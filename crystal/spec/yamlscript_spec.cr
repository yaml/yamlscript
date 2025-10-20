require "./spec_helper"

describe YAMLScript do
  it "loads a basic YAMLScript" do
    result = YAMLScript.load("!ys-0\ninc: 41")
    result.as_i.should eq(42)
  end

  it "loads plain YAML" do
    result = YAMLScript.load("foo: bar")
    result.as_h["foo"].as_s.should eq("bar")
  end

  it "raises an error on invalid YAMLScript" do
    expect_raises(Exception) do
      YAMLScript.load("!ys-0\ninc: (41")
    end
  end
end
