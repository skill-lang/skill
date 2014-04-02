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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Stack;

/**
 * FileChannel based input stream.
 *
 * @author Timm Felden
 */
final public class FileInputStream extends InStream {

	private final MappedByteBuffer input;
	private final Stack<Long> positions = new Stack<Long>();
	private final Path path;
	private final FileChannel file;

	public FileInputStream(Path path) throws IOException {
		this.file = (FileChannel) Files.newByteChannel(path, StandardOpenOption.READ);
		input = file.map(MapMode.READ_ONLY, 0, file.size());
		this.path = path;
	}

	@Override
	public long position() {
		return input.position();
	}

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

	@Override
	public boolean has(int n) {
		return input.limit() >= n + input.position();
	}

	@Override
	public byte[] bytes(long length) {
		final byte[] rval = new byte[(int) length];
		input.get(rval);
		return rval;
	}

	@Override
	public boolean bool() {
		return input.get() != 0;
	}

	@Override
	public byte i8() {
		return input.get();
	}

	@Override
	public short i16() {
		return input.getShort();
	}

	@Override
	public int i32() {
		return input.getInt();
	}

	@Override
	public long i64() {
		return input.getLong();
	}

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

	@Override
	public float f32() {
		return input.getFloat();
	}

	@Override
	public double f64() {
		return input.getDouble();
	}

	public Path path() {
		return path;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();

		if (file.isOpen())
			file.close();
	}
}
""")

    //class prefix
    out.close()
  }
}
