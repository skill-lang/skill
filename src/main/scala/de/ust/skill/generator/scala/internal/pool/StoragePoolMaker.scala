package de.ust.skill.generator.scala.internal.pool

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait StoragePoolMaker extends GeneralOutputMaker{
  override def make {
    super.make
    val out = open("internal/pool/StoragePool.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal.pool

import java.io.BufferedOutputStream

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.language.postfixOps

import ${packagePrefix}internal.SerializableState
import ${packagePrefix}internal.TypeInfo
import ${packagePrefix}internal.UserType

""")

    //the body itself is always the same
    copyFromTemplate(out, "StoragePool.scala.template")

    //class prefix
    out.close()
  }
}