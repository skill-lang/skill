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

/**
 * BufferedOutputStream based output stream.
 *
 * @author Timm Felden
 */
final public class FileOutputStream extends OutStream {

	private final BufferedOutputStream out;
	private final byte[] single = new byte[1];

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
		single[0] = data;
		out.write(single);
	}

	@Override
	public void put(byte[] data) throws Exception {
		out.write(data);
	}

	@Override
	public void close() throws Exception {
		out.close();
	}

}
""")

    //class prefix
    out.close()
  }
}
