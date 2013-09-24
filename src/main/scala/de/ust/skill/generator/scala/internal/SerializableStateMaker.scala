package de.ust.skill.generator.scala.internal

import scala.collection.JavaConversions._

import de.ust.skill.generator.scala.GeneralOutputMaker



trait SerializableStateMaker extends GeneralOutputMaker {
  override def make {
    super.make
    val out = open("internal/SerializableState.scala")

    //package & imports
    out.write(s"""package ${packagePrefix}internal

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

import scala.collection.mutable.HashMap

import ${packagePrefix}api._
import ${packagePrefix}internal.parsers._
import ${packagePrefix}internal.pool._
import ${packagePrefix}internal.types._
""")

    // first part: exceptions, internal structure, file writing
    copyFromTemplate(out, "SerializableState.scala.part1.template")

    //access to declared types
    IR.foreach({ t ⇒
      val name = t.getName()
      val Name = name.capitalize
      val sName = name.toLowerCase()
      val tName = packagePrefix + name

      val addArgs = t.getAllFields().filter(!_.isConstant).map({
        f ⇒ s"${f.getName()}: ${_T(f.getType())}"
      }).mkString(", ")

      out.write(s"""
  /**
   * returns a $name iterator
   */
  def get${Name}s(): Iterator[$tName] = new PoolIterator[$tName](pools("$sName").asInstanceOf[${Name}StoragePool])

  /**
   * adds a new $name to the $name pool
   */
  def add$Name($addArgs) = pools("$sName").asInstanceOf[${Name}StoragePool].add$Name(new _root_.${packagePrefix}internal.types.$name($addArgs))
""")

    })

    // second part: debug stuff; reading of files
    copyFromTemplate(out, "SerializableState.scala.part2.template")

    out.close()
  }
}
