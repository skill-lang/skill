/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java.api

import de.ust.skill.generator.java.GeneralOutputMaker

trait VisitorsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    if (visitors.length > 0) {
      val out = files.open(s"api/Visitor.java")
      //package & imports
      out.write(s"""package ${packagePrefix}api;

import ${packagePrefix}*;

/**
 * Base class of a distributed dispatching function ranging over specified types
 * implemented by the visitor pattern.
 * 
 * @author Timm Felden
 *
 * @param <_R>
 *            the result type
 * @param <_A>
 *            the argument type
 * @param <_E>
 *            the type of throws exception; use RuntimeException for nothrow
 */
public abstract class Visitor<_R, _A, _E extends Exception> {${
        (for (t ← visitors) yield s"""
    public abstract _R visit(${mapType(t)} self, _A arg) throws _E;""").mkString
      }
}
""")

      out.close()
    }
  }
}
