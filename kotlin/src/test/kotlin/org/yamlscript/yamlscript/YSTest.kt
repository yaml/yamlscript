// Copyright 2023-2026 Ingy dot Net
// This code is licensed under MIT license (See License for details)

package org.yamlscript.yamlscript

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class YSTest {
    @Test
    fun loadYsCode() {
        val data = YS.loadObject("!ys-0:\ntest:: inc(41)")
        assertEquals(42, data.getInt("test"))
    }

    @Test
    fun loadPlainYaml() {
        val data = YS.loadObject("foo: bar")
        assertEquals("bar", data.getString("foo"))
    }

    @Test
    fun loadErrorThrows() {
        assertFailsWith<RuntimeException> {
            YS.load(":")
        }
    }

    @Test
    fun loadMultipleTimes() {
        repeat(2) {
            val data = YS.loadObject("!ys-0:\ntest:: inc(41)")
            assertEquals(42, data.getInt("test"))
        }
    }
}
