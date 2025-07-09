namespace YAMLScript;

public class YAMLScriptException : Exception
{
    public YAMLScriptException(string message)
        : base(message)
    {
    }

    public YAMLScriptException(string message, Exception innerException)
        : base(message, innerException)
    {
    }
}
