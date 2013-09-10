package de.ust.skill.generator.scala.internal.pool

import java.io.PrintWriter

trait StringPoolMaker {
  protected def makeStringPool(out: PrintWriter) {
    out.write(s"""package ${packagePrefix}internal.pool

import scala.collection.mutable.HashSet
import java.io.OutputStream
import ${packagePrefix}internal.SerializableState

class StringPool {
  import SerializableState.v64;

  private var knownStrings = new HashSet[String];

  /**
   * writes the contents of the pool to the stream
   */
  def write(out: OutputStream) {
    out.write(v64(knownStrings.size))

    knownStrings.foreach(s ⇒ {
      val b = s.getBytes("UTF-8")
      out.write(v64(b.length));
      out.write(b)
    });
  }
}""")
    out.close()
  }

  /**
   * Assume a package prefix provider.
   */
  protected def packagePrefix(): String
}