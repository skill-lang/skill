/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import java.io.PrintWriter
import de.ust.skill.generator.ada.GeneralOutputMaker

trait ByteReaderSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-internal-byte_reader.ads""")

    out.write(s"""
with Ada.Unchecked_Conversion;

package ${packagePrefix.capitalize}.Internal.Byte_Reader is

   procedure Initialize (pInput_Stream : ASS_IO.Stream_Access);

   function Read_i8 return i8;
   function Read_i16 return i16;
   function Read_i32 return i32;
   function Read_i64 return i64;

   function Read_v64 return v64;

   function Read_f32 return f32;
   function Read_f64 return f64;

   function Read_Boolean return Boolean;
   function Read_String (Length : Integer) return String;

   procedure Skip_Bytes (Length : Long);

private

   function Read_Byte return Byte;

end ${packagePrefix.capitalize}.Internal.Byte_Reader;
""")

    out.close()
  }
}
