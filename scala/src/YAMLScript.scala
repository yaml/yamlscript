//> using scala 3.7.3
//> using dep net.java.dev.jna:jna:5.14.0
//> using dep com.lihaoyi::upickle:4.2.1

package org.yamlscript

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import java.io.File

trait LibYS extends Library:
  def graal_create_isolate(
    params: Pointer,
    isolate: PointerByReference,
    thread: PointerByReference,
  ): Int

  def graal_tear_down_isolate(thread: Pointer): Int

  def load_ys_to_json(thread: Pointer, input: String): String

object YAMLScript:
  val version = "0.2.29"

  def load(input: String): ujson.Value =
    val resp = ujson.read(loadJson(input))
    resp.obj.get("error") match
      case Some(err) if err != ujson.Null =>
        throw RuntimeException(err("cause").str)
      case _ =>
        resp("data")

  def loadJson(input: String): String =
    val lib = Native.load(libysPath(), classOf[LibYS])
    val isolate = PointerByReference()
    val thread = PointerByReference()
    if lib.graal_create_isolate(null, isolate, thread) != 0 then
      throw RuntimeException("Failed to create isolate")
    val json = lib.load_ys_to_json(thread.getValue(), input)
    if lib.graal_tear_down_isolate(thread.getValue()) != 0 then
      throw RuntimeException("Failed to tear down isolate")
    json

  private def libysName(): String =
    if Platform.isWindows then "libys.dll"
    else if Platform.isMac then s"libys.dylib.$version"
    else s"libys.so.$version"

  private def libysPath(): String =
    val name = libysName()
    val env =
      if Platform.isWindows then "PATH"
      else if Platform.isMac then "DYLD_LIBRARY_PATH"
      else "LD_LIBRARY_PATH"
    val paths =
      sys.env.get(env).toSeq.flatMap(_.split(File.pathSeparator)) ++
        Seq("/usr/local/lib", sys.props("user.home") + "/.local/lib")
    paths.map(p => File(p, name)).find(_.isFile).map(_.toString).getOrElse(
      throw RuntimeException(s"Shared library file '$name' not found")
    )
