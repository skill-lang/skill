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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import ${packagePrefix}internal.streams.OutBuffer.BulkData;
import ${packagePrefix}internal.streams.OutBuffer.ByteData;

/**
 * BufferedOutputStream based output stream.
 *
 * @author Timm Felden
 */
final public class FileOutputStream extends OutStream {

	private final BufferedOutputStream out;

	private FileOutputStream(BufferedOutputStream out) {
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
		return new FileOutputStream(new BufferedOutputStream(Files.newOutputStream(target, StandardOpenOption.APPEND,
				StandardOpenOption.WRITE)));
	}

	/**
	 * @return a new file output stream, that is setup to write the target file
	 * @throws IOException
	 *             propagated error
	 */
	public static FileOutputStream write(Path target) throws IOException {
		return new FileOutputStream(new BufferedOutputStream(Files.newOutputStream(target, StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)));
	}

	@Override
	public void put(byte data) throws Exception {
		out.write(data);
	}

	@Override
	public void put(byte[] data) throws Exception {
		out.write(data);
	}

	@Override
	public void close() throws Exception {
		out.close();
	}

	@Override
	public void v64(long v) throws Exception {
		if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
			out.write((int) v);
			return;
		} else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
			out.write((int) (0x80L | v));
			out.write((int) (v >> 7));
			return;
		} else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
			out.write((int) (0x80L | v));
			out.write((int) (0x80L | v >> 7));
			out.write((int) (v >> 14));
			return;
		} else if (0L == (v & 0xFFFFFFFFF0000000L)) {
			out.write((int) (0x80L | v));
			out.write((int) (0x80L | v >> 7));
			out.write((int) (0x80L | v >> 14));
			out.write((int) (v >> 21));
			return;
		} else if (0L == (v & 0xFFFFFFF800000000L)) {
			out.write((int) (0x80L | v));
			out.write((int) (0x80L | v >> 7));
			out.write((int) (0x80L | v >> 14));
			out.write((int) (0x80L | v >> 21));
			out.write((int) (v >> 28));
			return;
		} else if (0L == (v & 0xFFFFFC0000000000L)) {
			out.write((int) (0x80L | v));
			out.write((int) (0x80L | v >> 7));
			out.write((int) (0x80L | v >> 14));
			out.write((int) (0x80L | v >> 21));
			out.write((int) (0x80L | v >> 28));
			out.write((int) (v >> 35));
			return;
		} else if (0L == (v & 0xFFFE000000000000L)) {
			out.write((int) (0x80L | v));
			out.write((int) (0x80L | v >> 7));
			out.write((int) (0x80L | v >> 14));
			out.write((int) (0x80L | v >> 21));
			out.write((int) (0x80L | v >> 28));
			out.write((int) (0x80L | v >> 35));
			out.write((int) (v >> 42));
			return;
		} else if (0L == (v & 0xFF00000000000000L)) {
			out.write((int) (0x80L | v));
			out.write((int) (0x80L | v >> 7));
			out.write((int) (0x80L | v >> 14));
			out.write((int) (0x80L | v >> 21));
			out.write((int) (0x80L | v >> 28));
			out.write((int) (0x80L | v >> 35));
			out.write((int) (0x80L | v >> 42));
			out.write((int) (v >> 49));
			return;
		} else {
			out.write((int) (0x80L | v));
			out.write((int) (0x80L | v >> 7));
			out.write((int) (0x80L | v >> 14));
			out.write((int) (0x80L | v >> 21));
			out.write((int) (0x80L | v >> 28));
			out.write((int) (0x80L | v >> 35));
			out.write((int) (0x80L | v >> 42));
			out.write((int) (0x80L | v >> 49));
			out.write((int) (v >> 56));
			return;
		}
	}

	@Override
	public void putAll(OutBuffer stream) throws Exception {
		for (OutBuffer.Data d = stream.head; d != null; d = d.next) {
			if (d instanceof BulkData) {
				out.write(((BulkData) d).data, 0, ((BulkData) d).used);
			} else if (d instanceof ByteData) {
				out.write(((ByteData) d).data);
			}
		}
	}
}
""")

    //class prefix
    out.close()
  }
}
