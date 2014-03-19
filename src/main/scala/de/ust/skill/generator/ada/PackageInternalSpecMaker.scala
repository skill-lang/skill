/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import java.io.PrintWriter

trait PackageInternalSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-internal.ads""")

    out.write(s"""
with Ada.Streams.Stream_IO;
with Interfaces;

package ${packagePrefix.capitalize}.Internal is

   package ASS_IO renames Ada.Streams.Stream_IO;

   type Byte is new Interfaces.Unsigned_8;

end ${packagePrefix.capitalize}.Internal;
""")

    out.close()
  }
}
