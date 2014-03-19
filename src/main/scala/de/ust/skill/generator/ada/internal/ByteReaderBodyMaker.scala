/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import java.io.PrintWriter
import de.ust.skill.generator.ada.GeneralOutputMaker

trait ByteReaderBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-internal-byte_reader.adb""")

    out.write(s"""
package body ${packagePrefix.capitalize}.Internal.Byte_Reader is

   procedure Read_Buffer (Stream : not null access Ada.Streams.Root_Stream_Type'Class; Item : out Buffer) is
      Buffer : Ada.Streams.Stream_Element_Array (1 .. Ada.Streams.Stream_Element_Offset (Buffer_Size));
      Last : Ada.Streams.Stream_Element_Offset;
   begin
      Stream.Read (Buffer, Last);
      Buffer_Last := Positive (Last);

      for I in 1 .. Last loop
         Item (Integer (I)) := Byte (Buffer (I));
      end loop;
   end Read_Buffer;

   function Read_Byte (Input_Stream : ASS_IO.Stream_Access) return Byte is
   begin
      if Buffer_Size = Buffer_Index then
         Buffer'Read (Input_Stream, Buffer_Array);
         Buffer_Index := 0;
      end if;

      Buffer_Index := Buffer_Index + 1;

      declare
         Next : Byte := Buffer_Array (Buffer_Index);
      begin
         return Next;
      end;
   end Read_Byte;

   --  Short_Short_Integer
   function Read_i8 (Input_Stream : ASS_IO.Stream_Access) return i8 is
      function Convert is new Ada.Unchecked_Conversion (Byte, i8);
   begin
      return Convert (Read_Byte (Input_Stream));
   end Read_i8;

   --  Short_Integer (Short)
   function Read_i16 (Input_Stream : ASS_IO.Stream_Access) return i16 is
      A : i16 := i16 (Read_Byte (Input_Stream));
      B : i16 := i16 (Read_Byte (Input_Stream));
   begin
      return A * (2**8) + B;
   end Read_i16;

   --  Integer
   function Read_i32 (Input_Stream : ASS_IO.Stream_Access) return i32 is
      A : i32 := i32 (Read_Byte (Input_Stream));
      B : i32 := i32 (Read_Byte (Input_Stream));
      C : i32 := i32 (Read_Byte (Input_Stream));
      D : i32 := i32 (Read_Byte (Input_Stream));
   begin
      return A * (2**24) + B*(2**16) + C*(2**8) + D;
   end Read_i32;

   --  Long_Integer (Long)
   function Read_i64 (Input_Stream : ASS_IO.Stream_Access) return i64 is
      A : i64 := i64 (Read_Byte (Input_Stream));
      B : i64 := i64 (Read_Byte (Input_Stream));
      C : i64 := i64 (Read_Byte (Input_Stream));
      D : i64 := i64 (Read_Byte (Input_Stream));
      E : i64 := i64 (Read_Byte (Input_Stream));
      F : i64 := i64 (Read_Byte (Input_Stream));
      G : i64 := i64 (Read_Byte (Input_Stream));
      H : i64 := i64 (Read_Byte (Input_Stream));
   begin
      return A * (2**56) + B*(2**48) + C*(2**40) + D*(2**32) + E*(2**24) + F*(2**16) + G*(2**8) + H;
   end Read_i64;

   function Read_v64 (Input_Stream : ASS_IO.Stream_Access) return v64 is
      use Interfaces;

      subtype Result is Interfaces.Unsigned_64;
      function Convert is new Ada.Unchecked_Conversion (Source => Result, Target => v64);

      Count : Natural := 0;
      rval : Result := 0;
      Bucket : Result := Result (Read_Byte (Input_Stream));
   begin
      while (Count < 8 and then 0 /= (Bucket and 16#80#)) loop
         rval := rval or ((Bucket and 16#7f#) * (2 ** (7 * Count)));
         Count := Count + 1;
         Bucket := Result (Read_Byte (Input_Stream));
      end loop;

      case Count is
         when 8 => rval := rval or (Bucket * (2 ** (7 * Count)));
         when others => rval := rval or ((Bucket and 16#7f#) * (2 ** (7 * Count)));
      end case;

      return Convert (rval);
   end Read_v64;

   function Read_Boolean (Input_Stream : ASS_IO.Stream_Access) return Boolean is
      Unexcepted_Value : exception;
   begin
      case Read_Byte (Input_Stream) is
         when 16#ff# => return True;
         when 16#00# => return False;
         when others => raise Unexcepted_Value;
      end case;
   end Read_Boolean;

   function Read_String (Input_Stream : ASS_IO.Stream_Access; Length : Integer) return String is
      New_String : String (1 .. Length);
   begin
      for I in Integer range 1 .. Length loop
         New_String (I) := Character'Val (Read_Byte (Input_Stream));
      end loop;
      return New_String;
   end Read_String;

   procedure Skip_Bytes (Input_Stream : ASS_IO.Stream_Access; Length : Long) is
   begin
      for I in 1 .. Length loop
         declare
            Skip : Byte := Read_Byte (Input_Stream);
         begin
            null;
         end;
      end loop;
   end Skip_Bytes;

end ${packagePrefix.capitalize}.Internal.Byte_Reader;
""")

    out.close()
  }
}
