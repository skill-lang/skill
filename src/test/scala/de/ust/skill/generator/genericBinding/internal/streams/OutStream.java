/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 19.11.2014                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal.streams;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Encapsulation of memory mapped files that allows to use skill types and mmap
 * efficiently.
 *
 * @author Timm Felden
 */
abstract public class OutStream {

	// the buffer size is only used for mapping headers, which are usually
	// smaller
	protected final int BUFFERSIZE;
	protected ByteBuffer buffer;

	// current write position
	protected long position = 0L;

	public long position() {
		if (null == buffer)
			return position;

		return buffer.position() + position;
	}

	protected OutStream(ByteBuffer buffer) {
		this.buffer = buffer;
		BUFFERSIZE = buffer.capacity();
	}

	protected abstract void refresh() throws IOException;

	final public void i8(byte data) throws IOException {
		if (null == buffer || buffer.position() == BUFFERSIZE)
			refresh();
		buffer.put(data);
	}

	final public void i16(short data) throws IOException {
		if (null == buffer || buffer.position() + 2 > BUFFERSIZE)
			refresh();
		buffer.putShort(data);
	}

	final public void i32(int data) throws IOException {
		if (null == buffer || buffer.position() + 4 > BUFFERSIZE)
			refresh();
		buffer.putInt(data);
	}

	final public void i64(long data) throws IOException {
		if (null == buffer || buffer.position() + 8 > BUFFERSIZE)
			refresh();
		buffer.putLong(data);
	}

	final public void v64(long v) throws IOException {
		if (null == buffer || buffer.position() + 9 >= BUFFERSIZE)
			refresh();

		if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
			buffer.put((byte) v);
		} else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
			buffer.put((byte) (0x80L | v));
			buffer.put((byte) (v >> 7));
		} else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
			buffer.put((byte) (0x80L | v));
			buffer.put((byte) (0x80L | v >> 7));
			buffer.put((byte) (v >> 14));
		} else if (0L == (v & 0xFFFFFFFFF0000000L)) {
			buffer.put((byte) (0x80L | v));
			buffer.put((byte) (0x80L | v >> 7));
			buffer.put((byte) (0x80L | v >> 14));
			buffer.put((byte) (v >> 21));
		} else if (0L == (v & 0xFFFFFFF800000000L)) {
			buffer.put((byte) (0x80L | v));
			buffer.put((byte) (0x80L | v >> 7));
			buffer.put((byte) (0x80L | v >> 14));
			buffer.put((byte) (0x80L | v >> 21));
			buffer.put((byte) (v >> 28));
		} else if (0L == (v & 0xFFFFFC0000000000L)) {
			buffer.put((byte) (0x80L | v));
			buffer.put((byte) (0x80L | v >> 7));
			buffer.put((byte) (0x80L | v >> 14));
			buffer.put((byte) (0x80L | v >> 21));
			buffer.put((byte) (0x80L | v >> 28));
			buffer.put((byte) (v >> 35));
		} else if (0L == (v & 0xFFFE000000000000L)) {
			buffer.put((byte) (0x80L | v));
			buffer.put((byte) (0x80L | v >> 7));
			buffer.put((byte) (0x80L | v >> 14));
			buffer.put((byte) (0x80L | v >> 21));
			buffer.put((byte) (0x80L | v >> 28));
			buffer.put((byte) (0x80L | v >> 35));
			buffer.put((byte) (v >> 42));
		} else if (0L == (v & 0xFF00000000000000L)) {
			buffer.put((byte) (0x80L | v));
			buffer.put((byte) (0x80L | v >> 7));
			buffer.put((byte) (0x80L | v >> 14));
			buffer.put((byte) (0x80L | v >> 21));
			buffer.put((byte) (0x80L | v >> 28));
			buffer.put((byte) (0x80L | v >> 35));
			buffer.put((byte) (0x80L | v >> 42));
			buffer.put((byte) (v >> 49));
		} else {
			buffer.put((byte) (0x80L | v));
			buffer.put((byte) (0x80L | v >> 7));
			buffer.put((byte) (0x80L | v >> 14));
			buffer.put((byte) (0x80L | v >> 21));
			buffer.put((byte) (0x80L | v >> 28));
			buffer.put((byte) (0x80L | v >> 35));
			buffer.put((byte) (0x80L | v >> 42));
			buffer.put((byte) (0x80L | v >> 49));
			buffer.put((byte) (v >> 56));
		}
	}

	final public void f32(float data) throws IOException {
		if (null == buffer || buffer.position() + 4 > BUFFERSIZE)
			refresh();
		buffer.putFloat(data);
	}

	final public void f64(double data) throws IOException {
		if (null == buffer || buffer.position() + 8 > BUFFERSIZE)
			refresh();
		buffer.putDouble(data);
	}
}
