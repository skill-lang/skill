/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada2

import scala.collection.JavaConversions._

trait ApiSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api.ads""")

    out.write(s"""
with Interfaces.C;
with Interfaces.C.Pointers;

package ${packagePrefix.capitalize}.Api is

   subtype i8 is Interfaces.Integer_8 range Interfaces.Integer_8'Range;
   subtype i16 is Interfaces.Integer_16 range Interfaces.Integer_16'Range;
   subtype i32 is Interfaces.Integer_32 range Interfaces.Integer_32'Range;
   subtype i64 is Interfaces.Integer_64 range Interfaces.Integer_64'Range;
   subtype v64 is Interfaces.Integer_64 range Interfaces.Integer_64'Range;

   type Unsigned_Char_Array (<>) is private;
   type v64_Extended is private;

private

   type v64_Extended is
      record
         Value  : v64;
         Length : Interfaces.C.size_t'Base range 1 .. 9;
      end record;

   type Unsigned_Char_Array is array (Interfaces.C.size_t range <>) of aliased Interfaces.C.unsigned_char;
   pragma Convention (C, Unsigned_Char_Array);
   for Unsigned_Char_Array'Component_Size use Interfaces.C.unsigned_char'Size;

end ${packagePrefix.capitalize}.Api;
    """.replaceAll("""\h+$""", ""))

    out.close()
  }
}
