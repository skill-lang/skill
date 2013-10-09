/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.parsers

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait ByteReaderMaker extends GeneralOutputMaker{
  override def make {
    super.make
    val out = open("internal/parsers/ByteReader.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal.parsers

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Arrays

import scala.util.parsing.input.Position
import scala.util.parsing.input.Reader
import scala.collection.mutable.Stack

import ${packagePrefix}internal.UnexpectedEOF
""")

    //the body itself is always the same
    copyFromTemplate(out, "ByteReader.scala.template")

    //class prefix
    out.close()
  }
}
