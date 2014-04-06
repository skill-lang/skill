/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.streams

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait FileOutputStreamMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/streams/FileOutputStream.java")
    //package
    out.write(s"""package ${packagePrefix}internal.streams;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * BufferedOutputStream based output stream.
 *
 * @author Timm Felden
 */
final public class FileOutputStream extends OutStream {

	private final int BUFFERSIZE = 8 * 1024;
	private final byte[] buffer = new byte[BUFFERSIZE];
	private int capacity = 0;

	private void flush() throws IOException {
		if (0 != capacity) {
			out.write(buffer, 0, capacity);
			capacity = 0;
		}
	}

	private final OutputStream out;

	private FileOutputStream(OutputStream out) {
		this.out = out;
	}

	/**
	 * @return a new file output stream, that is setup to append to the target
	 *         fileoutput stream, that is setup to write the target file
	 * @throws IOException
	 *             propagated error
	 * 
	 */
	public static FileOutputStream append(Path target) throws IOException {
		return new FileOutputStream(Files.newOutputStream(target, StandardOpenOption.APPEND, StandardOpenOption.WRITE));
	}

	/**
	 * @return a new file output stream, that is setup to write the target file
	 * @throws IOException
	 *             propagated error
	 */
	public static FileOutputStream write(Path target) throws IOException {
		Files.deleteIfExists(target);
		return new FileOutputStream(Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING));
	}

	@Override
	public void put(byte data) throws Exception {
		if (capacity == BUFFERSIZE)
			flush();
		buffer[capacity++] = data;
	}

	@Override
	public void put(byte[] data) throws Exception {
		flush();
		out.write(data);
	}

	@Override
	public void close() throws Exception {
		flush();
		out.close();
	}

	@Override
	public void v64(long v) throws Exception {
		if (capacity + 9 >= BUFFERSIZE)
			flush();

		if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
			buffer[capacity++] = (byte) v;
			return;
		} else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
			buffer[capacity++] = (byte) (0x80L | v);
			buffer[capacity++] = (byte) (v >> 7);
			return;
		} else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
			buffer[capacity++] = (byte) (0x80L | v);
			buffer[capacity++] = (byte) (0x80L | v >> 7);
			buffer[capacity++] = (byte) (v >> 14);
			return;
		} else if (0L == (v & 0xFFFFFFFFF0000000L)) {
			buffer[capacity++] = (byte) (0x80L | v);
			buffer[capacity++] = (byte) (0x80L | v >> 7);
			buffer[capacity++] = (byte) (0x80L | v >> 14);
			buffer[capacity++] = (byte) (v >> 21);
			return;
		} else if (0L == (v & 0xFFFFFFF800000000L)) {
			buffer[capacity++] = (byte) (0x80L | v);
			buffer[capacity++] = (byte) (0x80L | v >> 7);
			buffer[capacity++] = (byte) (0x80L | v >> 14);
			buffer[capacity++] = (byte) (0x80L | v >> 21);
			buffer[capacity++] = (byte) (v >> 28);
			return;
		} else if (0L == (v & 0xFFFFFC0000000000L)) {
			buffer[capacity++] = (byte) (0x80L | v);
			buffer[capacity++] = (byte) (0x80L | v >> 7);
			buffer[capacity++] = (byte) (0x80L | v >> 14);
			buffer[capacity++] = (byte) (0x80L | v >> 21);
			buffer[capacity++] = (byte) (0x80L | v >> 28);
			buffer[capacity++] = (byte) (v >> 35);
			return;
		} else if (0L == (v & 0xFFFE000000000000L)) {
			buffer[capacity++] = (byte) (0x80L | v);
			buffer[capacity++] = (byte) (0x80L | v >> 7);
			buffer[capacity++] = (byte) (0x80L | v >> 14);
			buffer[capacity++] = (byte) (0x80L | v >> 21);
			buffer[capacity++] = (byte) (0x80L | v >> 28);
			buffer[capacity++] = (byte) (0x80L | v >> 35);
			buffer[capacity++] = (byte) (v >> 42);
			return;
		} else if (0L == (v & 0xFF00000000000000L)) {
			buffer[capacity++] = (byte) (0x80L | v);
			buffer[capacity++] = (byte) (0x80L | v >> 7);
			buffer[capacity++] = (byte) (0x80L | v >> 14);
			buffer[capacity++] = (byte) (0x80L | v >> 21);
			buffer[capacity++] = (byte) (0x80L | v >> 28);
			buffer[capacity++] = (byte) (0x80L | v >> 35);
			buffer[capacity++] = (byte) (0x80L | v >> 42);
			buffer[capacity++] = (byte) (v >> 49);
			return;
		} else {
			buffer[capacity++] = (byte) (0x80L | v);
			buffer[capacity++] = (byte) (0x80L | v >> 7);
			buffer[capacity++] = (byte) (0x80L | v >> 14);
			buffer[capacity++] = (byte) (0x80L | v >> 21);
			buffer[capacity++] = (byte) (0x80L | v >> 28);
			buffer[capacity++] = (byte) (0x80L | v >> 35);
			buffer[capacity++] = (byte) (0x80L | v >> 42);
			buffer[capacity++] = (byte) (0x80L | v >> 49);
			buffer[capacity++] = (byte) (v >> 56);
			return;
		}
	}

	@Override
	public void putAll(OutBuffer stream) throws Exception {
		flush();
		for (OutBuffer.Data d = stream.head; d != null; d = d.next) {
			out.write(d.data, 0, d.used);
		}
	}
}
""")

    //class prefix
    out.close()
  }
}
