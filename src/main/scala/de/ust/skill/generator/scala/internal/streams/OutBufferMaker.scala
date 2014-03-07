/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.streams

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait OutBufferMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/streams/OutBuffer.java")
    //package
    out.write(s"""package ${packagePrefix}internal.streams;

/**
 * Store data as a non-empty single linked list.
 * 
 * @note we do not want to use an array here, because we would run into jvm
 *       limits before running out of memory - sadly.
 * 
 * @author Timm Felden
 */
final public class OutBuffer extends OutStream {
	static abstract class Data {
		public Data next = null;
	}

	static final class NoData extends Data {
		// is used for roots; can appear everywhere!
	}

	static final class ByteData extends Data {
		public final byte data;

		ByteData(byte data) {
			this.data = data;
		}

		ByteData(byte data, Data tail) {
			tail.next = this;
			this.data = data;
		}
	}

	static final class BulkData extends Data {
		public final byte[] data;

		BulkData(byte[] data) {
			this.data = data;
		}

		BulkData(byte[] data, Data tail) {
			tail.next = this;
			this.data = data;
		}
	}

	final Data head;
	private Data tail;
	private long size;

	public long size() {
		return size;
	}

	public OutBuffer() {
		head = new NoData();
		tail = head;
		size = 0;
	}

	public OutBuffer(byte data) {
		head = new ByteData(data);
		tail = head;
		size = 1;
	}

	public OutBuffer(byte[] data) {
		head = new BulkData(data);
		tail = head;
		size = data.length;
	}

	@Override
	public void put(byte data) {
		tail = new ByteData(data, tail);
		size++;
	}

	@Override
	public void put(byte[] data) {
		tail = new BulkData(data, tail);
		size += data.length;
	}

	@Override
	public void putAll(OutBuffer stream) throws Exception {
		tail.next = stream.head;
		tail = stream.tail;
		size += stream.size;
	}

	@Override
	public void close() throws Exception {
		// irrelevant
	}
}
""")

    //class prefix
    out.close()
  }
}
