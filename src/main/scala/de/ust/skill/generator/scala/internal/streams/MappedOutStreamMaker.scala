/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.streams

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait MappedOutStreamMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/streams/MappedOutStream.java")
    //package
    out.write(s"""package ${packagePrefix}internal.streams;

import java.io.IOException;
import java.nio.MappedByteBuffer;

/**
 * Allows writing to memory mapped region.
 *
 * @author Timm Felden
 */
final public class MappedOutStream extends OutStream {

	protected MappedOutStream(MappedByteBuffer buffer) {
		super(buffer);
	}

	@Override
	protected void refresh() throws IOException {
		// do nothing; let the JIT remove this method and all related checks
	}
}
""")

    //class prefix
    out.close()
  }
}
