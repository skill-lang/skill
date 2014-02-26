/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.streams

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait InStreamMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/streams/InStream.java")
    //package & imports
    out.write(s"""package ${packagePrefix}internal.streams;

/**
 * Implementations of this class are used to turn a byte stream into a stream of
 * integers and floats.
 *
 * @author Timm Felden
 */
public abstract class InStream {

	public InStream() {
		super();
	}

	public abstract double f64();

	public abstract float f32();

	public abstract long v64();

	public abstract long i64();

	public abstract int i32();

	public abstract short i16();

	public abstract byte i8();

	public abstract byte[] bytes(long length);

	public abstract boolean has(int n);

	public abstract boolean eof();

	public abstract void pop();

	public abstract void push(long position);

	public abstract void jump(long position);

	public abstract long position();
}
""")

    //class prefix
    out.close()
  }
}
