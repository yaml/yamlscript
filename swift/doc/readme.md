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
source <(curl -sL https://in-1.cc) --local libys
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```

In your `Package.swift`:

```swift
dependencies: [
    .package(
        url: "https://github.com/yaml/yamlscript-swift",
        from: "0.3.0"),
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
