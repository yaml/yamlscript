// Copyright 2023-2026 Ingy dot Net
// This code is licensed under MIT license (See License for details)

import XCTest

@testable import YAMLScript

final class YAMLScriptTests: XCTestCase {
    func testLoadYsCode() throws {
        let ys = try YAMLScript()
        let data = try ys.load("!ys-0:\ntest:: inc(41)")
        let map = try XCTUnwrap(data as? [String: Any])
        XCTAssertEqual(map["test"] as? Int, 42)
    }

    func testLoadPlainYaml() throws {
        let ys = try YAMLScript()
        let data = try ys.load("foo: bar")
        let map = try XCTUnwrap(data as? [String: Any])
        XCTAssertEqual(map["foo"] as? String, "bar")
    }

    func testLoadError() throws {
        let ys = try YAMLScript()
        XCTAssertThrowsError(try ys.load(":"))
        XCTAssertNotNil(ys.error)
    }

    func testLoadMultipleTimes() throws {
        let ys = try YAMLScript()
        for _ in 0..<2 {
            let data = try ys.load("!ys-0:\ntest:: inc(41)")
            let map = try XCTUnwrap(data as? [String: Any])
            XCTAssertEqual(map["test"] as? Int, 42)
        }
    }
}
