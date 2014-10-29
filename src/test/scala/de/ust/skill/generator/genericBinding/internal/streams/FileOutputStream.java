/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 29.10.2014                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal.streams;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * BufferedOutputStream based output stream.
 *
 * @author Timm Felden
 */
final public class FileOutputStream extends OutStream {

	private final FileChannel file;

	private FileOutputStream(FileChannel file) {
		// the size is smaller then 4KiB, because headers are expected to be 1KiB at most
		super(ByteBuffer.allocate(1024));
		this.file = file;
	}

	public static FileOutputStream write(Path target) throws IOException {
		Files.deleteIfExists(target);
		return new FileOutputStream(FileChannel.open(target, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.READ));
	}

	/**
	 * @return a new file output stream, that is setup to append to the target
	 *         fileoutput stream, that is setup to write the target file
	 * @throws IOException
	 *             propagated error
	 * 
	 */
	public static FileOutputStream append(Path target) throws IOException {
		return new FileOutputStream(FileChannel.open(target, StandardOpenOption.WRITE, StandardOpenOption.APPEND,
				StandardOpenOption.READ));
	}

	@Override
	protected void refresh() throws IOException {
		if (null == buffer)
			buffer = ByteBuffer.allocate(BUFFERSIZE);
		else if (0 != buffer.position()) {
			flush();
			buffer = ByteBuffer.allocate(BUFFERSIZE);
		}
	}

	private void flush() throws IOException {
		if (null != buffer) {
			final int p = buffer.position();
			buffer.limit(p);
			position += p;
			buffer.position(0);
			assert (p == buffer.remaining());
			file.write(buffer);
		}
	}

	/**
	 * put an array of bytes into the stream
	 * 
	 * @note you may not reuse data after putting it to a stream, because the
	 *       actual put might be a deferred operation
	 * @param data
	 *            the data to be written
	 */
	public void put(byte[] data) throws IOException {
		if (data.length > BUFFERSIZE) {
			if (null != buffer) {
				flush();
				buffer = null;
			}
			file.write(ByteBuffer.wrap(data), position);
		} else {
			if (null == buffer || buffer.position() + data.length > BUFFERSIZE)
				refresh();
			buffer.put(data);
		}
	}

	/**
	 * Creates a map as usually used for writing field data chunks concurrently.
	 * 
	 * @param basePosition
	 *            absolute start index of the mapped region
	 * @param begin
	 *            begin offset of the mapped region
	 * @param end
	 *            end offset of the mapped region
	 */
	synchronized public MappedOutStream map(long basePosition, long begin, long end) throws IOException {
		if (null != buffer) {
			flush();
			buffer = null;
		}
		long p = basePosition + end;
		position = position < p ? p : position;
		return new MappedOutStream(file.map(MapMode.READ_WRITE, basePosition + begin, end - begin));
	}

	/**
	 * signal the stream to close
	 */
	public void close() throws IOException {
		flush();
		file.force(false);
		file.close();
	}
}
