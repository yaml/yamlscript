module TestYAMLScript

using Test
import YAMLScript as YS

@testset "load" begin
    function load(script)
        ys = YS.Runtime()
        YS.load(ys, script)
    end

    @testset "a: 1" begin
        data = load("a: 1")
        @test data["a"] == 1
    end

    @testset "A simple script" begin
        script = """
!YS v0:
say: "Hello"
key: ! inc(42)
baz: ! range(1 6)
"""
        obj = load(script)
        @test haskey(obj, "say")
        @test haskey(obj, "key")
        @test haskey(obj, "baz")

        @test obj["say"] == "Hello"
        @test obj["key"] == 43
        @test obj["baz"] == [1, 2, 3, 4, 5]
    end

#     @testset "An error case" begin
#         script = """
# !YS v0:
# : : : : : :
# """
#         @test_throws Exception load(script)
#     end

    @testset "Load multiple times" begin
        script = """
!YS v0:
say: "Hello"
key: ! inc(42)
baz: ! range(1 6)
"""
        ys = YS.Runtime()
        obj = YS.load(ys, script)
        @test obj["say"] == "Hello"
        @test obj["key"] == 43
        @test obj["baz"] == [1, 2, 3, 4, 5]
        obj = YS.load(ys, script)
        @test obj["say"] == "Hello"
        @test obj["key"] == 43
        @test obj["baz"] == [1, 2, 3, 4, 5]
    end
end

end  # module TestYAMLScript
