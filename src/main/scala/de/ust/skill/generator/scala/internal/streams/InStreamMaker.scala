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

	/**
	 * @return take an f64 from the stream
	 */
	public abstract double f64();

	/**
	 * @return take an f32 from the stream
	 */
	public abstract float f32();

	/**
	 * @return take an v64 from the stream
	 */
	public abstract long v64();

	/**
	 * @return take an i64 from the stream
	 */
	public abstract long i64();

	/**
	 * @return take an i32 from the stream
	 * @throws UnexpectedEOF
	 *             if there is no i32 in the stream
	 */
	public abstract int i32();

	/**
	 * @return take an i16 from the stream
	 */
	public abstract short i16();

	/**
	 * @return take an i8 from the stream
	 */
	public abstract byte i8();

	/**
	 * @return raw byte array taken from the stream
	 */
	public abstract byte[] bytes(long length);

	/**
	 * @return true iff there are at least n bytes left in the stream
	 */
	public abstract boolean has(int n);

	public abstract boolean eof();

	public abstract void pop();

	public abstract void push(long position);

	/**
	 * use with care!
	 * 
	 * @param position
	 *            jump to target position, without the ability to restore the
	 *            old position
	 */
	public abstract void jump(long position);

	public abstract long position();
}
""")

    //class prefix
    out.close()
  }
}
