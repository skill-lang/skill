/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.streams

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait FileInputStreamMaker extends GeneralOutputMaker{
  abstract override def make {
    super.make
    val out = open("internal/streams/FileInputStream.java")
    //package
    out.write(s"""package ${packagePrefix}internal.streams;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Stack;

/**
 * FileChannel based input stream.
 *
 * @author Timm Felden
 */
final public class FileInputStream extends InStream {

	private final MappedByteBuffer input;
	private final Stack<Long> positions = new Stack<Long>();

	public FileInputStream(FileChannel file) throws IOException {
		input = file.map(MapMode.READ_ONLY, 0, file.size());
	}

	@Override
	public long position() {
		return input.position();
	}

	/**
	 * use with care!
	 * 
	 * @param position
	 *            jump to target position, without the ability to restore the
	 *            old position
	 */
	@Override
	public void jump(long position) {
		input.position((int) position);
	}

	@Override
	public void push(long position) {
		positions.push((long) input.position());
		input.position((int) position);
	}

	@Override
	public void pop() {
		input.position(positions.pop().intValue());
	}

	@Override
	public boolean eof() {
		return input.limit() == input.position();
	}

	/**
	 * @return true iff there are at least n bytes left in the stream
	 */
	@Override
	public boolean has(int n) {
		return input.limit() >= n + input.position();
	}

	/**
	 * @return raw byte array taken from the stream
	 */
	@Override
	public byte[] bytes(long length) {
		final byte[] rval = new byte[(int) length];
		input.get(rval);
		return rval;
	}

	/**
	 * @return take an i8 from the stream
	 */
	@Override
	public byte i8() {
		return input.get();
	}

	/**
	 * @return take an i16 from the stream
	 */
	@Override
	public short i16() {
		return input.getShort();
	}

	/**
	 * @return take an i32 from the stream
	 * @throws UnexpectedEOF
	 *             if there is no i32 in the stream
	 */
	@Override
	public int i32() {
		return input.getInt();
	}

	/**
	 * @return take an i64 from the stream
	 */
	@Override
	public long i64() {
		return input.getLong();
	}

	/**
	 * @return take an v64 from the stream
	 */
	@Override
	public long v64() {
		long count = 0;
		long rval = 0;
		long r = i8();
		while (count < 8 && 0 != (r & 0x80)) {
			rval |= (r & 0x7f) << (7 * count);

			count += 1;
			r = i8();
		}
		rval = (rval | (8 == count ? r : (r & 0x7f)) << (7 * count));
		return rval;
	}

	/**
	 * @return take an f32 from the stream
	 */
	@Override
	public float f32() {
		return input.getFloat();
	}

	/**
	 * @return take an f64 from the stream
	 */
	@Override
	public double f64() {
		return input.getDouble();
	}
}
""")

    //class prefix
    out.close()
  }
}
