package de.ust.skill.generator.scala.internal.pool

import java.io.PrintWriter

trait StoragePoolMaker {
  protected def makeStoragePool(out: PrintWriter) {
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

  /**
   * Assume template copy functionality.
   */
  protected def copyFromTemplate(out: PrintWriter, template: String): Unit

  /**
   * Assume a package prefix provider.
   */
  protected def packagePrefix(): String
}