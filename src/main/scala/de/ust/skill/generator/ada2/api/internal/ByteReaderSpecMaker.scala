/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada2

import scala.collection.JavaConversions._

trait ByteReaderSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api-internal-byte_reader.ads""")

    out.write(s"""
with Interfaces.C;

package ${packagePrefix.capitalize}.Api.Internal.Byte_Reader is

   function Read_i8 (
      Mapped : Unsigned_Char_Array;
      Start  : Interfaces.C.size_t
   ) return i8;

   function Read_i16 (
      Mapped : Unsigned_Char_Array;
      Start  : Interfaces.C.size_t
   ) return i16;

   function Read_i32 (
      Mapped : Unsigned_Char_Array;
      Start  : Interfaces.C.size_t
   ) return i32;

   function Read_i64 (
      Mapped : Unsigned_Char_Array;
      Start  : Interfaces.C.size_t
   ) return i64;

   function Read_v64 (
      Mapped : Unsigned_Char_Array;
      Start  : Interfaces.C.size_t
   ) return v64_Extended;

   function Read_String (
      Mapped : Unsigned_Char_Array;
      Start  : Interfaces.C.size_t;
      Length : i32
   ) return String;

   pragma Inline (
      Read_i8,
      Read_i16,
      Read_i32,
      Read_i64,
      Read_v64,
      Read_String
   );

end ${packagePrefix.capitalize}.Api.Internal.Byte_Reader;
    """.replaceAll("""\h+$""", ""))

    out.close()
  }
}
