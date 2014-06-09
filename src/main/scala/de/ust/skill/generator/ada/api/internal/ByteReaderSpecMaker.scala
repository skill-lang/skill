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
    val out = open(s"""${packagePrefix}-api-internal-byte_reader.ads""")

    out.write(s"""
with Ada.Unchecked_Conversion;

--
--  This package provides the necessary functions to read the basic skill types
--  from a stream.
--

private package ${packagePrefix.capitalize}.Api.Internal.Byte_Reader is

   procedure Reset_Buffer;
   function End_Of_Buffer return Boolean;

   function Read_i8 (Stream : ASS_IO.Stream_Access) return i8;
   function Read_i16 (Stream : ASS_IO.Stream_Access) return i16;
   function Read_i32 (Stream : ASS_IO.Stream_Access) return i32;
   function Read_i64 (Stream : ASS_IO.Stream_Access) return i64;

   function Read_v64 (Stream : ASS_IO.Stream_Access) return v64;

   function Read_f32 (Stream : ASS_IO.Stream_Access) return f32;
   function Read_f64 (Stream : ASS_IO.Stream_Access) return f64;

   function Read_Boolean (Stream : ASS_IO.Stream_Access) return Boolean;
   function Read_String (
      Stream : ASS_IO.Stream_Access;
      Length : i32
   ) return String;

   procedure Skip_Bytes (
      Stream : ASS_IO.Stream_Access;
      Length : Long
   );

private

   Buffer_Size  : constant Positive := 2 ** 12;
   Buffer_Last  :          Positive;
   Buffer_Index :          Integer  := Buffer_Size;
   type Buffer is array (Positive range <>) of Byte;
   procedure Read_Buffer (
      Stream : not null access Ada.Streams.Root_Stream_Type'Class;
      Item   : out Buffer
   );
   for Buffer'Read use Read_Buffer;
   Buffer_Array : Buffer (1 .. Buffer_Size);

   function Read_Byte (Stream : ASS_IO.Stream_Access) return Byte;

   pragma Inline (
      Reset_Buffer,
      End_Of_Buffer,
      Read_i8,
      Read_i16,
      Read_i32,
      Read_i64,
      Read_v64,
      Read_f32,
      Read_f64,
      Read_Boolean,
      Read_String,
      Skip_Bytes,
      Read_Buffer,
      Read_Byte
   );

end ${packagePrefix.capitalize}.Api.Internal.Byte_Reader;
""")

    out.close()
  }
}
