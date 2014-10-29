/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 29.10.2014                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal.streams;

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
