/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.streams

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait OutStreamMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/streams/OutStream.java")
    //package
    out.write(s"""package ${packagePrefix}internal.streams;

/**
 * Out streams can only put bytes. The next layer of abstraction is added by the
 * serialization functions.
 * 
 * @author Timm Felden
 */
public abstract class OutStream {
	public abstract void put(byte data) throws Exception;

	/**
	 * put an array of bytes into the stream
	 * 
	 * @note you may not reuse data after putting it to a stream, because the
	 *       actual put might be a deferred operation
	 * @param data
	 *            the data to be written
	 * @throws Exception
	 *             propagated from used library
	 */
	public abstract void put(byte[] data) throws Exception;

	/**
	 * @param stream
	 *            put the buffer into the stream; required for data chunks
	 */
	public void putAll(OutBuffer stream) throws Exception {
		for (OutBuffer.Data pos = stream.head; null != pos; pos = pos.next) {
			if (pos instanceof OutBuffer.ByteData)
				put(((OutBuffer.ByteData) pos).data);
			else if (pos instanceof OutBuffer.BulkData)
				put(((OutBuffer.BulkData) pos).data);
		}
	}

	/**
	 * signal the stream to close
	 */
	public abstract void close() throws Exception;
}
""")

    //class prefix
    out.close()
  }
}
