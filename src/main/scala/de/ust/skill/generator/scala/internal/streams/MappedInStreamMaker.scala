/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.streams

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait MappedInStreamMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/streams/MappedInStream.java")
    //package
    out.write(s"""package ${packagePrefix}internal.streams;

/**
 * This stream is used to parse a mapped region of field data.
 * 
 * @author Timm Felden
 */
public class MappedInStream extends InStream {

	@Override
	public double f64() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float f32() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long v64() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long i64() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int i32() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public short i16() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte i8() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean bool() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public byte[] bytes(long length) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean has(int n) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean eof() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void pop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void push(long position) {
		// TODO Auto-generated method stub

	}

	@Override
	public void jump(long position) {
		// TODO Auto-generated method stub

	}

	@Override
	public long position() {
		// TODO Auto-generated method stub
		return 0;
	}
}
""")

    //class prefix
    out.close()
  }
}
