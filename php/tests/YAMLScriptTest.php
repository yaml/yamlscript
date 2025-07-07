<?php

namespace YAMLScript\Tests;

use PHPUnit\Framework\TestCase;
use YAMLScript\YAMLScript;
use RuntimeException;

class YAMLScriptTest extends TestCase
{
    private YAMLScript $ys;

    protected function setUp(): void
    {
        // Let YAMLScript constructor find libys using LD_LIBRARY_PATH
        // which is set by the Makefile
        $this->ys = new YAMLScript();
    }

    public function testBasicCompilation(): void
    {
        $input = "
say: Hello World
";
        $result = $this->ys->compile($input);
        $this->assertNotEmpty($result);
        $this->assertJson($result);
        $decoded = json_decode($result, true);
        $this->assertIsArray($decoded);
        $this->assertEquals('Hello World', $decoded['say']);
    }

    public function testEmptyInput(): void
    {
        $result = $this->ys->compile('');
        $this->assertNotEmpty($result);
        $this->assertJson($result);
        $decoded = json_decode($result, true);
        $this->assertIsArray($decoded);
        $this->assertEmpty($decoded);
    }

    public function testInvalidYAML(): void
    {
        $this->expectException(RuntimeException::class);
        $input = "
invalid:
  - yaml:
    syntax
";
        $this->ys->compile($input);
    }
}
