# Copyright 2023-2026 Ingy dot Net
# This code is licensed under MIT license (See License for details)

defmodule YAMLScriptTest do
  use ExUnit.Case, async: true

  test "load ys code" do
    assert {:ok, %{"test" => 42}} =
             YAMLScript.load("!ys-0:\ntest:: inc(41)")
  end

  test "load plain yaml" do
    assert {:ok, %{"foo" => "bar"}} = YAMLScript.load("foo: bar")
  end

  test "load error returns cause" do
    assert {:error, cause} = YAMLScript.load(":")
    assert is_binary(cause)
  end

  test "load! raises" do
    assert_raise YAMLScript.Error, fn ->
      YAMLScript.load!(":")
    end
  end

  test "load multiple times" do
    for _ <- 1..4 do
      assert {:ok, %{"test" => 42}} =
               YAMLScript.load("!ys-0:\ntest:: inc(41)")
    end
  end
end
