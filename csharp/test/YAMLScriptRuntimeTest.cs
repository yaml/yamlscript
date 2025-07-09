using Xunit;
using YAMLScript;

namespace YAMLScript.Tests;

public class YAMLScriptRuntimeTest
{
    [Fact]
    public void TestRuntimeInitialization()
    {
        var runtime = new YAMLScriptRuntime();
        Assert.NotNull(runtime);
    }

    [Fact]
    public void TestBasicEvaluation()
    {
        using var runtime = new YAMLScriptRuntime();

        // Test a simple YAMLScript expression
        var result = runtime.Evaluate("answer: 42");

        Assert.NotNull(result);
        // The result should be a JsonElement representing the YAML object
        Assert.True(result is System.Text.Json.JsonElement);
    }

    [Fact]
    public void TestDispose()
    {
        var runtime = new YAMLScriptRuntime();
        runtime.Dispose();

        // Should not throw when disposed
        Assert.Throws<ObjectDisposedException>(() => runtime.Evaluate("test: value"));
    }
}
