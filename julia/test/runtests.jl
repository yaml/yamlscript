using Test
using YAMLScript

@testset "YAMLScript.jl" begin

  binPath = joinpath(@__DIR__, "../bin/ys-yamlscript.jl")
  ysPath = joinpath(@__DIR__, "../test/hello.ys")
  greeting = readchomp(`$binPath $ysPath Julia`)
  @test greeting == "Hello, Julia!"

end
