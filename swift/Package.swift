// swift-tools-version:5.9

import PackageDescription

let package = Package(
    name: "YAMLScript",
    products: [
        .library(name: "YAMLScript", targets: ["YAMLScript"])
    ],
    targets: [
        .target(name: "YAMLScript"),
        .testTarget(
            name: "YAMLScriptTests",
            dependencies: ["YAMLScript"]
        ),
    ]
)
