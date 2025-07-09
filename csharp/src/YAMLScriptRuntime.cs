using System;
using System.Text.Json;
using System.Runtime.InteropServices;
using YAMLScript.Native;

namespace YAMLScript;

public sealed class YAMLScriptRuntime : IDisposable
{
    private readonly IntPtr _isolateThread;
    private bool _disposed;

    public YAMLScriptRuntime()
    {
        _isolateThread = IntPtr.Zero;
        var rc = YAMLScriptNative.graal_create_isolate(
            IntPtr.Zero,
            IntPtr.Zero,
            ref _isolateThread);

        if (rc != 0 || _isolateThread == IntPtr.Zero)
        {
            throw new YAMLScriptException("Failed to create GraalVM isolate");
        }
    }

    public object? Evaluate(string yaml, string source = "")
    {
        if (_disposed)
        {
            throw new ObjectDisposedException(nameof(YAMLScriptRuntime));
        }

        var result = YAMLScriptNative.load_ys_to_json(_isolateThread, yaml);

        if (result == IntPtr.Zero)
        {
            return null;
        }

        var json = Marshal.PtrToStringAnsi(result);
        if (json == null)
        {
            return null;
        }

        try
        {
            var response = JsonSerializer.Deserialize<JsonElement>(json);

            // Check for error in response
            if (response.TryGetProperty("error", out var errorElement))
            {
                var error = errorElement.GetProperty("cause").GetString();
                throw new YAMLScriptException(error ?? "Unknown error");
            }

            // Get the data from response
            if (response.TryGetProperty("data", out var dataElement))
            {
                return JsonSerializer.Deserialize<object>(dataElement.GetRawText());
            }

            return null;
        }
        catch (JsonException ex)
        {
            throw new YAMLScriptException($"Failed to parse response: {ex.Message}");
        }
    }

    public void Dispose()
    {
        if (!_disposed && _isolateThread != IntPtr.Zero)
        {
            var rc = YAMLScriptNative.graal_tear_down_isolate(_isolateThread);
            if (rc != 0)
            {
                // Log error but don't throw since we're in Dispose
                Console.Error.WriteLine($"Failed to tear down isolate: {rc}");
            }
            _disposed = true;
        }
    }
}
