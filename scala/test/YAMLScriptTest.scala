//> using scala 3.7.3

package org.yamlscript

object YAMLScriptTest:
  def main(args: Array[String]): Unit =
    val data = YAMLScript.load("!ys-0:\ntest:: inc(41)")
    assert(data("test").num.toInt == 42)
    println("ok - load ys code")
