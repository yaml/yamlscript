## C# Usage

Use `YAMLScript.NET` as a drop-in replacement for your current YAML loader:

```csharp
// Program.cs
using YAMLScript;
using System;
using System.IO;

class Program
{
    static void Main()
    {
        using var ys = new YAMLScriptLoader();

        // Load from file
        string input = File.ReadAllText("config.yaml");
        var config = ys.Load(input);

        Console.WriteLine(config);
    }
}
```


## Installation

Install YAMLScript for .NET and the `libys.so` shared library:

```bash
# Add package reference
dotnet add package YAMLScript.NET

# Install shared library
curl -sSL https://yamlscript.org/install | bash
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* .NET 8.0 or higher