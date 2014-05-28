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
    val out = open(s"""${packagePrefix}-api-internal-byte_reader.adb""")

    out.write(s"""
package body ${packagePrefix.capitalize}.Api.Internal.Byte_Reader is

   procedure Read_Buffer (
      Stream : not null access Ada.Streams.Root_Stream_Type'Class;
      Item   : out Buffer
   ) is
      use Ada.Streams;

      Buffer : Stream_Element_Array (1 .. Stream_Element_Offset (Buffer_Size));
      Last   : Stream_Element_Offset;
   begin
      Stream.Read (Buffer, Last);
      Buffer_Last := Positive (Last);

      for I in 1 .. Last loop
         Item (Integer (I)) := Byte (Buffer (I));
      end loop;
   end Read_Buffer;

   procedure Reset_Buffer is
   begin
      Byte_Reader.Buffer_Index := Byte_Reader.Buffer_Size;
   end Reset_Buffer;

   function End_Of_Buffer return Boolean is
      (Byte_Reader.Buffer_Index >= Byte_Reader.Buffer_Last);

   function Read_Byte (Stream : ASS_IO.Stream_Access) return Byte is
   begin
      if Buffer_Size = Buffer_Index then
         Buffer'Read (Stream, Buffer_Array);
         Buffer_Index := 0;
      end if;

      Buffer_Index := Buffer_Index + 1;

      declare
         Next : Byte := Buffer_Array (Buffer_Index);
      begin
         return Next;
      end;
   end Read_Byte;

   function Read_i8 (Stream : ASS_IO.Stream_Access) return i8 is
      function Convert is new Ada.Unchecked_Conversion (Byte, i8);
   begin
      return Convert (Read_Byte (Stream));
   end Read_i8;

   function Read_i16 (Stream : ASS_IO.Stream_Access) return i16 is
      A : i16 := i16 (Read_Byte (Stream));
      B : i16 := i16 (Read_Byte (Stream));
   begin
      return A * (2**8) +
             B;
   end Read_i16;

   function Read_i32 (Stream : ASS_IO.Stream_Access) return i32 is
      A : i32 := i32 (Read_Byte (Stream));
      B : i32 := i32 (Read_Byte (Stream));
      C : i32 := i32 (Read_Byte (Stream));
      D : i32 := i32 (Read_Byte (Stream));
   begin
      return A * (2 ** 24) +
             B * (2 ** 16) +
             C * (2 ** 8) +
             D;
   end Read_i32;

   function Read_i64 (Stream : ASS_IO.Stream_Access) return i64 is
      A : i64 := i64 (Read_Byte (Stream));
      B : i64 := i64 (Read_Byte (Stream));
      C : i64 := i64 (Read_Byte (Stream));
      D : i64 := i64 (Read_Byte (Stream));
      E : i64 := i64 (Read_Byte (Stream));
      F : i64 := i64 (Read_Byte (Stream));
      G : i64 := i64 (Read_Byte (Stream));
      H : i64 := i64 (Read_Byte (Stream));
   begin
      return A * (2 ** 56) +
             B * (2 ** 48) +
             C * (2 ** 40) +
             D * (2 ** 32) +
             E * (2 ** 24) +
             F * (2 ** 16) +
             G * (2 ** 8) +
             H;
   end Read_i64;

   function Read_v64 (Stream : ASS_IO.Stream_Access) return v64 is
      use Interfaces;

      function Convert is new Ada.Unchecked_Conversion (Unsigned_64, v64);

      Count        : Natural     := 0;
      Return_Value : Unsigned_64 := 0;
      Bucket       : Unsigned_64 := Unsigned_64 (Read_Byte (Stream));
   begin
      while (Count < 8 and then 0 /= (Bucket and 16#80#)) loop
         Return_Value := Return_Value or ((Bucket and 16#7f#) * (2 ** (7 * Count)));
         Count        := Count + 1;
         Bucket       := Unsigned_64 (Read_Byte (Stream));
      end loop;

      case Count is
         when 8      => Return_Value := Return_Value or (Bucket * (2 ** (7 * Count)));
         when others => Return_Value := Return_Value or ((Bucket and 16#7f#) * (2 ** (7 * Count)));
      end case;

      return Convert (Return_Value);
   end Read_v64;

   function Read_f32 (Stream : ASS_IO.Stream_Access) return f32 is
      function Convert is new Ada.Unchecked_Conversion (i32, f32);

      A : i32 := Read_i32 (Stream);
   begin
      return Convert (A);
   end Read_f32;

   function Read_f64 (Stream : ASS_IO.Stream_Access) return f64 is
      function Convert is new Ada.Unchecked_Conversion (i64, f64);

      A : i64 := Read_i64 (Stream);
   begin
      return Convert (A);
   end Read_f64;

   function Read_Boolean (Stream : ASS_IO.Stream_Access) return Boolean is
      Unexcepted_Value : exception;
   begin
      case Read_Byte (Stream) is
         when 16#ff# => return True;
         when 16#00# => return False;
         when others => raise Unexcepted_Value;
      end case;
   end Read_Boolean;

   function Read_String (
      Stream : ASS_IO.Stream_Access;
      Length       : i32
   ) return String is
      New_String : String (1 .. Integer (Length));
   begin
      for I in Integer range 1 .. Integer (Length) loop
         New_String (I) := Character'Val (Read_Byte (Stream));
      end loop;
      return New_String;
   end Read_String;

   procedure Skip_Bytes (
      Stream : ASS_IO.Stream_Access;
      Length       : Long
   ) is
   begin
      for I in 1 .. Length loop
         declare
            Skip : Byte := Read_Byte (Stream);
         begin
            null;
         end;
      end loop;
   end Skip_Bytes;

end ${packagePrefix.capitalize}.Api.Internal.Byte_Reader;
""")

    out.close()
  }
}
