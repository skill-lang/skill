/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 19.11.2014                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal.streams;

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
	 * @return take a bool from the stream
	 */
	public abstract boolean bool();

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
