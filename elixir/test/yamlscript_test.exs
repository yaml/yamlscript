defmodule YamlscriptTest do
  use ExUnit.Case
  doctest Yamlscript

  test "greets the world" do
    assert Yamlscript.hello() == :world
  end
end
