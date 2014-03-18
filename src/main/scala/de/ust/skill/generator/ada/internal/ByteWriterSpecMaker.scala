/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import java.io.PrintWriter
import de.ust.skill.generator.ada.GeneralOutputMaker

trait ByteWriterSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-internal-byte_writer.ads""")

    out.write(s"""
with Ada.Unchecked_Conversion;

package ${packagePrefix.capitalize}.Internal.Byte_Writer is

   procedure Write_i8 (Stream : ASS_IO.Stream_Access; Value : i8);
   procedure Write_i16 (Stream : ASS_IO.Stream_Access; Value : i16);
   procedure Write_i32 (Stream : ASS_IO.Stream_Access; Value : i32);
   procedure Write_i64 (Stream : ASS_IO.Stream_Access; Value : i64);

   procedure Write_v64 (Stream : ASS_IO.Stream_Access; Value : v64);

   procedure Write_Boolean (Stream : ASS_IO.Stream_Access; Value : Boolean);
   procedure Write_String (Stream : ASS_IO.Stream_Access; Value : String);

private

   procedure Write_Byte (Stream : ASS_IO.Stream_Access; Next : Byte);

   type Byte_v64_Type is array (Natural range <>) of Byte;
   function Get_v64_Bytes (Value : v64) return Byte_v64_Type;

   pragma Inline (Write_i8, Write_i16, Write_i32, Write_i64, Write_v64, Write_Boolean, Write_String, Write_Byte, Write_Byte, Get_v64_Bytes);

end ${packagePrefix.capitalize}.Internal.Byte_Writer;
""")

    out.close()
  }
}
