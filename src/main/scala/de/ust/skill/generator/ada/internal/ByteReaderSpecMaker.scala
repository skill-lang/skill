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

   function Read_i8 (Input_Stream : ASS_IO.Stream_Access) return i8;
   function Read_i16 (Input_Stream : ASS_IO.Stream_Access) return i16;
   function Read_i32 (Input_Stream : ASS_IO.Stream_Access) return i32;
   function Read_i64 (Input_Stream : ASS_IO.Stream_Access) return i64;

   function Read_v64 (Input_Stream : ASS_IO.Stream_Access) return v64;

   function Read_Boolean (Input_Stream : ASS_IO.Stream_Access) return Boolean;
   function Read_String (Input_Stream : ASS_IO.Stream_Access; Length : Integer) return String;

private

   function Read_Byte (Input_Stream : ASS_IO.Stream_Access) return Byte;

   pragma Inline (Read_i8, Read_i16, Read_i32, Read_i64, Read_v64, Read_Boolean, Read_String, Read_Byte);

end ${packagePrefix.capitalize}.Internal.Byte_Reader;
""")

    out.close()
  }
}
