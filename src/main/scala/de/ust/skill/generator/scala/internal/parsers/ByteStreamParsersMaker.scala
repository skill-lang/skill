/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.parsers

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait ByteStreamParsersMaker extends GeneralOutputMaker{
  abstract override def make {
    super.make
    val out = open("internal/parsers/ByteStreamParsers.scala")
    //package
    out.write(s"""package ${packagePrefix}internal.parsers

import java.util.Arrays

import scala.language.implicitConversions
import scala.util.parsing.combinator.Parsers

import ${packagePrefix}internal.SerializableState
import ${packagePrefix}internal.SkillException
import ${packagePrefix}internal.UnexpectedEOF

/**
 * @note This parser does not support backtracking. If one would be interest in backtracking the atomic parsers should
 *  use the in.mark and in.reset methods if they fail.
 *
 * @author Timm Felden
 */
class ByteStreamParsers extends Parsers {
  type Elem = Byte
  protected implicit def readerToByteReader(x: Input): ByteReader = x.asInstanceOf[ByteReader]

  def hasMore(): Parser[Unit] = new Parser[Unit] {
    def apply(in: Input) = {
      if (in.atEnd)
        Failure("EOF", in)
      else
        Success((), in)
    }
  }

  def bytes(n: Int): Parser[Array[Byte]] = new Parser[Array[Byte]] {
    def apply(in: Input) = try {
      Success(in take n, in)
    } catch {
      case e: Exception ⇒ throw UnexpectedEOF(s"while taking $$n bytes", e)
    }
  }
  def bytes(n: Long): Parser[Array[Byte]] = bytes(n.toInt)

  def i8 = new Parser[Byte] {
    def apply(in: Input) = try {
      Success(in.next, in)
    } catch {
      case e: Exception ⇒ throw UnexpectedEOF("while reading i8", e)
    }
  }
  def i16 = new Parser[Short] {
    def apply(in: Input) = try {
      Success(in.i16, in)
    } catch {
      case e: Exception ⇒ throw UnexpectedEOF("while reading i16", e)
    }
  }
  def i32 = new Parser[Int] {
    def apply(in: Input) = try {
      Success(in.i32, in)
    } catch {
      case e: Exception ⇒ throw UnexpectedEOF("while reading i32", e)
    }
  }
  def i64 = new Parser[Long] {
    def apply(in: Input) = try {
      Success(in.i64, in)
    } catch {
      case e: Exception ⇒ throw UnexpectedEOF("while reading i64", e)
    }
  }

  def v64 = new Parser[Long] {
    def apply(in: Input) = try {
      Success(in.v64, in)
    } catch {
      case e: Exception ⇒ throw UnexpectedEOF("while reading v64", e)
    }
  }

  def f32 = new Parser[Float] {
    def apply(in: Input) = try {
      Success(in.f32, in)
    } catch {
      case e: Exception ⇒ throw UnexpectedEOF("while reading f32", e)
    }
  }
  def f64 = new Parser[Double] {
    def apply(in: Input) = try {
      Success(in.f64, in)
    } catch {
      case e: Exception ⇒ throw UnexpectedEOF("while reading f64", e)
    }
  }

  def string(σ: SerializableState) = v64 ^^ {
    _ match {
      case 0 ⇒ null
      case i ⇒ σ.String.get(i)
    }
  }
}
""")

    //class prefix
    out.close()
  }
}
