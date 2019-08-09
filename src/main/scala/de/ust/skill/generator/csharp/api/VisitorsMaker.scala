/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.csharp.api

import de.ust.skill.generator.csharp.GeneralOutputMaker

trait VisitorsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    if (visitors.length > 0) {
      val out = files.open(s"api/Visitor.cs")
      //package & imports
      out.write(s"""
using System;

namespace ${this.packageName}
{
    namespace api
    {

        /// <summary>
        /// Base class of a distributed dispatching function ranging over specified types
        /// implemented by the visitor pattern.
        ///
        /// @author Simon Glaub, Timm Felden
        /// </summary>
        /// <param id =_R> the result type </param>
        /// <param id =_A> the argument type </param>
        /// <param id =_E> the type of throws exception; use RuntimeException for nothrow </param>
        public abstract class Visitor<_R, _A, _E> where _E : Exception{${
                (for (t ← visitors) yield s"""
            public abstract _R visit(${mapType(t)} self, _A arg);""").mkString
              }
        }
    }
}
""")

      out.close()
    }
  }
}
