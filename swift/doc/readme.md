## Swift Usage

Use `YAMLScript` as a drop-in replacement for your current YAML loader:

File `main.swift`:

```swift
import Foundation
import YAMLScript

let ys = try YAMLScript()
let input = try String(
    contentsOfFile: "config.yaml", encoding: .utf8)
let data = try ys.load(input)
print(data ?? "null")
```


## Installation

Add the `yamlscript-swift` package to your project and install the
`libys.so` shared library:

```bash
curl -sSL https://yamlscript.org/install | LIB=1 bash
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```

In your `Package.swift`:

```swift
dependencies: [
    .package(
        url: "https://github.com/yaml/yamlscript-swift",
        from: "0.2.25"),
],
targets: [
    .executableTarget(
        name: "your-app",
        dependencies: [
            .product(name: "YAMLScript", package: "yamlscript-swift")
        ]),
]
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* Swift 5.9 or higher
* Linux or macOS
