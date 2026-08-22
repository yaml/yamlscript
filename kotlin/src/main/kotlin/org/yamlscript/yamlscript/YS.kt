// Copyright 2023-2026 Ingy dot Net
// This code is licensed under MIT license (See License for details)

// Kotlin binding/API for the libys shared library.
//
// This is a thin idiomatic Kotlin layer over the Java binding (the
// org.yamlscript:yamlscript artifact), which wraps libys with JNA.
//
// The current user facing API is the YS object, whose load() function
// takes a YAMLScript string as input and returns the value that the
// YAMLScript code evaluates to.

package org.yamlscript.yamlscript

import org.json.JSONArray
import org.json.JSONObject

object YS {
    // This value is automatically updated by 'make bump'.
    const val YAMLSCRIPT_VERSION = "0.2.31"

    /** Compile and eval a YAMLScript string and return the result. */
    fun load(input: String): Any? {
        val resp = YAMLScript.loadJSON(input)
        return if (resp.isNull("data")) null else resp.get("data")
    }

    /** Load a YAMLScript string that evaluates to a mapping. */
    fun loadObject(input: String): JSONObject =
        YAMLScript.loadObject(input)

    /** Load a YAMLScript string that evaluates to a sequence. */
    fun loadArray(input: String): JSONArray =
        YAMLScript.loadArray(input)

    /** Load a YAMLScript string that evaluates to a string. */
    fun loadString(input: String): String =
        YAMLScript.loadJSON(input).getString("data")

    /** Load a YAMLScript string that evaluates to an integer. */
    fun loadInt(input: String): Int =
        YAMLScript.loadInt(input)

    /** Load a YAMLScript string that evaluates to a boolean. */
    fun loadBoolean(input: String): Boolean =
        YAMLScript.loadBoolean(input)
}
